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

//LONG DONE
package spatialindex.storagemanager;

import java.util.*;

public abstract class Buffer implements IBuffer
{
	int m_capacity = 10;
	boolean m_bWriteThrough = false;
	IStorageManager m_storageManager = null;
	HashMap <Long,Entry> m_buffer = new HashMap <Long,Entry>();
	long m_hits = 0;

	abstract void addEntry(long l_id, Entry entry);
	abstract void removeEntry();

	public Buffer(IStorageManager sm, int capacity, boolean bWriteThrough)
	{
		m_storageManager = sm;
		m_capacity = capacity;
		m_bWriteThrough = bWriteThrough;
	}

	public byte[] loadByteArray(final long l_id)
	{
		byte[] ret = null;
		Entry e = m_buffer.get(new Long(l_id));

		if (e != null)
		{
			m_hits++;

			ret = new byte[e.m_data.length];
			System.arraycopy(e.m_data, 0, ret, 0, e.m_data.length);
  	}
		else
		{
			ret = m_storageManager.loadByteArray(l_id);
			e = new Entry(ret);
			addEntry(l_id, e);
		}

		return ret;
	}
	public long storeByteArray(final long l_id, final byte[] data)
	{
		long l_ret = l_id;

		if (l_id == l_NewPage)
  	{
 			l_ret = m_storageManager.storeByteArray(l_id, data);
 			Entry e = new Entry(data);
			addEntry(l_ret, e);
  	}
  	else
  	{
  		if (m_bWriteThrough)
			{
				m_storageManager.storeByteArray(l_id, data);
			}

			Entry e = m_buffer.get(new Long(l_id));
			if (e != null)
			{
				e.m_data = new byte[data.length];
				System.arraycopy(data, 0, e.m_data, 0, data.length);

				if (m_bWriteThrough == false)
				{
					e.m_bDirty = true;
					m_hits++;
				}
				else
				{
					e.m_bDirty = false;
				}
			}
			else
			{
				e = new Entry(data);
				if (m_bWriteThrough == false) e.m_bDirty = true;
  			addEntry(l_id, e);
			}
		}

		return l_ret;
	}
	public void deleteByteArray(final long l_id)
	{
		Long ID = new Long(l_id);
		Entry e = m_buffer.get(ID);
		if (e != null)
		{
			m_buffer.remove(ID);
		}

		m_storageManager.deleteByteArray(l_id);
	}

	public void flush()
	{
		Iterator < Map.Entry<Long, Entry>> it = m_buffer.entrySet().iterator();

		while (it.hasNext())
		{
			Map.Entry me = it.next();
			Entry e = (Entry) me.getValue();
			long id = ((Long) me.getKey()).longValue();
			if (e.m_bDirty) m_storageManager.storeByteArray(id, e.m_data);
		}

		m_storageManager.flush();
	}
	public void clear()
	{
		Iterator it = m_buffer.entrySet().iterator();

		while (it.hasNext())
		{
			Map.Entry me = (Map.Entry) it.next();
			Entry e = (Entry) me.getValue();

			if (e.m_bDirty)
			{
				long id = ((Long) me.getKey()).longValue();
				m_storageManager.storeByteArray(id, e.m_data);
			}
		}

		m_buffer.clear();
		m_hits = 0;
	}

	public long getHits()
	{
		return m_hits;
	}

	class Entry
	{
		byte[] m_data = null;
		boolean m_bDirty = false;

		Entry(final byte[] d)
		{
			m_data = new byte[d.length];
			System.arraycopy(d, 0, m_data, 0, d.length);
		}
	}; // Entry

} // Buffer
