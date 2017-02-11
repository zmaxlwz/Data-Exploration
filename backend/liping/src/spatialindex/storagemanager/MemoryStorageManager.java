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

//Long Done
package spatialindex.storagemanager;

import java.util.*;

public class MemoryStorageManager implements IStorageManager
{
	private ArrayList m_buffer = new ArrayList();
	private Stack m_emptyPages = new Stack();

	public void flush()
	{
	}

	public byte[] loadByteArray(final long l_id)
	{
		Entry e = null;

		try
		{
			e = (Entry) m_buffer.get((int)l_id);
		}
		catch (IndexOutOfBoundsException ex)
		{
			throw new InvalidPageException((int)l_id);
		}

		byte[] ret = new byte[e.m_pData.length];
		System.arraycopy(e.m_pData, 0, ret, 0, e.m_pData.length);
		return ret;
	}

	public long storeByteArray(final long l_id, final byte[] data)
	{
		long l_ret = l_id;
		Entry e = new Entry(data);

		if (l_id == l_NewPage)
		{
			if (m_emptyPages.empty())
			{
				m_buffer.add(e);
				l_ret = m_buffer.size() - 1;
			}
			else
			{
				l_ret = ((Long) m_emptyPages.pop()).longValue();
				m_buffer.set((int)l_ret, e);
			}
		}
		else
		{
			if ((int)l_id < 0 || (int)l_id >= m_buffer.size()) throw new InvalidPageException((int)l_id);
			m_buffer.set((int)l_id, e);
		}

		return l_ret;
	}

	public void deleteByteArray(final long l_id)
	{
		Entry e = null;
		try
		{
			e = (Entry) m_buffer.get((int)l_id);
		}
		catch (IndexOutOfBoundsException ex)
		{
			throw new InvalidPageException((int)l_id);
		}

		m_buffer.set((int)l_id, null);
		m_emptyPages.push(new Long(l_id));
	}

	class Entry
	{
		byte[] m_pData;

		Entry(final byte[] d)
		{
			m_pData = new byte[d.length];
			System.arraycopy(d, 0, m_pData, 0, d.length);
		}
	} // Entry
}
