// Spatial Index Library
//
// Copyright (C) 2002  Navel Ltd.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Contact information:
//  Mailing address:
//    Marios Hadjieleftheriou
//    University of California, Riverside
//    Department of Computer Science
//    Surge Building, Room 310
//    Riverside, CA 92521
//
//  Email:
//    marioh@cs.ucr.edu

package spatialindex.storagemanager;

import java.util.*;
import java.io.*;

//Long Done
public class DiskStorageManager implements IStorageManager
{
	private RandomAccessFile m_dataFile = null;
	private RandomAccessFile m_indexFile = null;
	private int m_pageSize = 0;
	private long l_m_nextPage = -1;
	private TreeSet <Long> m_emptyPages = new TreeSet <Long>();
	private HashMap <Long, Entry> m_pageIndex = new HashMap <Long, Entry> ();
	private byte[] m_buffer = null;

	public DiskStorageManager(PropertySet ps)
		throws SecurityException, NullPointerException, IOException, FileNotFoundException, IllegalArgumentException
	{
		long a = System.currentTimeMillis();
		Object var;

		// Open/Create flag.
		boolean bOverwrite = false;
		var = ps.getProperty("Overwrite");

		if (var != null)
		{
			if (! (var instanceof Boolean)) throw new IllegalArgumentException("Property Overwrite must be a Boolean");
			bOverwrite = ((Boolean) var).booleanValue();
		}

		// storage filename.
		var = ps.getProperty("FileName");

		if (var != null)
		{
			if (! (var instanceof String)) throw new IllegalArgumentException("Property FileName must be a String");

			File indexFile = new File((String) var + ".idx");
			File dataFile = new File((String) var + ".dat");

			// check if files exist.
			if (bOverwrite == false && (! indexFile.exists() || ! dataFile.exists())) bOverwrite = true;

			if (bOverwrite)
			{
				if (indexFile.exists()) indexFile.delete();
				if (dataFile.exists()) dataFile.delete();///////////

				boolean b = indexFile.createNewFile();
				if (b == false) throw new IOException("Index file cannot be opened.");

				b = dataFile.createNewFile();///////////
				if (b == false) throw new IOException("Data file cannot be opened.");///////////
			}

			m_indexFile = new RandomAccessFile(indexFile, "rw");
			m_dataFile = new RandomAccessFile(dataFile, "rw");///////////
		}
		else
		{
			throw new IllegalArgumentException("Property FileName was not specified.");
		}
		// find page size.
		if (bOverwrite == true)
		{
			var = ps.getProperty("PageSize");

			if (var != null)
			{
				if (! (var instanceof Integer)) throw new IllegalArgumentException("Property PageSize must be an Integer");
				m_pageSize = ((Integer) var).intValue();
				l_m_nextPage = 0;
			}
			else
			{
				throw new IllegalArgumentException("Property PageSize was not specified.");
			}
		}
		else
		{
			try
			{
				m_pageSize = m_indexFile.readInt();
			}
			catch (EOFException ex)
			{
				throw new IllegalStateException("Failed reading pageSize.");
			}

			try
			{
				l_m_nextPage = m_indexFile.readLong();
			}
			catch (EOFException ex)
			{
				throw new IllegalStateException("Failed reading nextPage.");
			}
		}

		// create buffer.
		m_buffer = new byte[m_pageSize];

		if (bOverwrite == false)
		{
			long l_count, l_id, l_page;

			// load empty pages in memory.
			try
			{
				l_count = m_indexFile.readLong();

				for (long cCount = 0; cCount < l_count; cCount++)
				{
					l_page = m_indexFile.readLong();
					m_emptyPages.add(new Long(l_page));
				}

				// load index table in memory.
				l_count = m_indexFile.readLong();

				for (long cCount = 0; cCount < l_count; cCount++)
				{
					Entry e = new Entry();

					l_id = m_indexFile.readLong();
					e.m_length = m_indexFile.readInt();

					int count2 = m_indexFile.readInt();

					for (int cCount2 = 0; cCount2 < count2; cCount2++)
					{
						l_page = m_indexFile.readLong();
						e.m_pages.add(new Long(l_page));
					}
					m_pageIndex.put(new Long(l_id), e);
				}
			}
			catch (EOFException ex)
			{
				throw new IllegalStateException("Corrupted index file.");
			}
//			System.err.println("DiskStorageManager.DiskStorageManager(): " + (System.currentTimeMillis()-a) + "\t" + l_count);
		}
		
	}

	public void flush()
	{
		try
		{
			m_indexFile.seek(0l);

			m_indexFile.writeInt(m_pageSize);
			m_indexFile.writeLong(l_m_nextPage);

			long l_id, l_page;
			long l_count = m_emptyPages.size();//TODO: safe?

			m_indexFile.writeLong(l_count);

			Iterator it = m_emptyPages.iterator();
			while (it.hasNext())
			{
				l_page = ((Long)it.next()).longValue();
				m_indexFile.writeLong(l_page);
			}

			l_count = m_pageIndex.size();//TODO: safe?
			m_indexFile.writeLong(l_count);

			it = m_pageIndex.entrySet().iterator();

			while (it.hasNext())
			{
				Map.Entry me = (Map.Entry) it.next();
				l_id = ((Long) me.getKey()).longValue();
				m_indexFile.writeLong(l_id);

				Entry e = (Entry) me.getValue();
				l_count = e.m_length;//TODO: safe?
				m_indexFile.writeInt((int)l_count);

				l_count = e.m_pages.size();//TODO: safe?
				m_indexFile.writeInt((int)l_count);

				for (int cIndex = 0; cIndex < l_count; cIndex++)
				{
					l_page = e.m_pages.get(cIndex).longValue();
					m_indexFile.writeLong(l_page);
				}
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Corrupted index file.");
		}
	}

	public byte[] loadByteArray(final long l_id)
	{
		Entry e = (Entry) m_pageIndex.get(new Long(l_id));
		if (e == null) throw new InvalidPageException((int)l_id);

		int cNext = 0;
		int cTotal = e.m_pages.size();

		byte[] data = new byte[e.m_length];
		int cIndex = 0;
		int cLen;
		int cRem = e.m_length;

		do
		{
			try
			{
				m_dataFile.seek( e.m_pages.get(cNext).longValue() * m_pageSize);
				int bytesread = m_dataFile.read(m_buffer);
				if (bytesread != m_pageSize) throw new IllegalStateException("Corrupted data file.");
			}
			catch (IOException ex)
			{
				throw new IllegalStateException("Corrupted data file.");
			}

			cLen = (cRem > m_pageSize) ? m_pageSize : cRem;
			System.arraycopy(m_buffer, 0, data, cIndex, cLen);

			cIndex += cLen;
			cRem -= cLen;
			cNext++;
		}
		while (cNext < cTotal);

		return data;
	}

	public long storeByteArray(final long l_id, final byte[] data)
	{
		if (l_id == l_NewPage)
		{
			Entry e = new Entry();
			e.m_length = data.length;

			int cIndex = 0;
			long l_cPage;
			int cRem = data.length;
			int cLen;

			while (cRem > 0)
			{
				if (! m_emptyPages.isEmpty())
				{
					Long i = m_emptyPages.first();
					m_emptyPages.remove(i);
					l_cPage = i.longValue();
				}
				else
				{
					l_cPage = l_m_nextPage;
					l_m_nextPage++;
				}

				cLen = (cRem > m_pageSize) ? m_pageSize : cRem;
				System.arraycopy(data, cIndex, m_buffer, 0, cLen);

				try
				{
					m_dataFile.seek(l_cPage * m_pageSize);
					m_dataFile.write(m_buffer);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Corrupted data file.");
				}

				cIndex += cLen;
				cRem -= cLen;
				e.m_pages.add(new Long(l_cPage));
			}

			Long i = e.m_pages.get(0);
			m_pageIndex.put(i, e);

			return i.longValue();
		}
		else
		{
			// find the entry.
			Entry oldEntry = m_pageIndex.get(new Long(l_id));
			if (oldEntry == null) throw new InvalidPageException((int)l_id);

			m_pageIndex.remove(new Long(l_id));

			Entry e = new Entry();
			e.m_length = data.length;

			int cIndex = 0;
			long l_cPage;
			int cRem = data.length;
			int cLen, cNext = 0;

			while (cRem > 0)
			{
				if (cNext < oldEntry.m_pages.size())
				{
					l_cPage = oldEntry.m_pages.get(cNext).longValue();
					cNext++;
				}
				else if (! m_emptyPages.isEmpty())
				{
					Long i = m_emptyPages.first();
					m_emptyPages.remove(i);
					l_cPage = i.longValue();
				}
				else
				{
					l_cPage = l_m_nextPage;
					l_m_nextPage++;
				}

				cLen = (cRem > m_pageSize) ? m_pageSize : cRem;
				System.arraycopy(data, cIndex, m_buffer, 0, cLen);

				try
				{
					m_dataFile.seek(l_cPage * m_pageSize);
					m_dataFile.write(m_buffer);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Corrupted data file.");
				}

				cIndex += cLen;
				cRem -= cLen;
				e.m_pages.add(new Long(l_cPage));
			}

			while (cNext < oldEntry.m_pages.size())
			{
				m_emptyPages.add(oldEntry.m_pages.get(cNext));
				cNext++;
			}

			Long i = e.m_pages.get(0);
			m_pageIndex.put(i, e);

			return i.longValue();
		}
	}

	public void deleteByteArray(final long l_id)
	{
		// find the entry.
		Entry e = m_pageIndex.get(new Long(l_id));
		if (e == null) throw new InvalidPageException((int)l_id);

		m_pageIndex.remove(new Long(l_id));

		for (int cIndex = 0; cIndex < e.m_pages.size(); cIndex++)
		{
			m_emptyPages.add(e.m_pages.get(cIndex));
		}
	}

	public void close()
	{
		flush();
	}

	class Entry
	{
		int m_length = 0;
		ArrayList <Long> m_pages = new ArrayList <Long> ();
	}
}
