/**
 * Copyright (C) 2009 Authors
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebeaninternal.server.loadcontext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DLoadWeakList<T> {

	protected final ArrayList<WeakReference<T>> list = new ArrayList<WeakReference<T>>();

	protected int removedFromTop;	
	
	protected DLoadWeakList() {
		
	}
	
	public int add(T e){
	    synchronized (this) {
	        int i = list.size();
	        list.add(new WeakReference<T>(e));
	        return i;            
        }
	}

	/**
	 * Return the next batch of entries from the top.
	 */
    public List<T> getNextBatch(int batchSize) {
        synchronized (this) {
            return getBatch(0, batchSize);
        }
    }
   
    /**
     * Return the batch of entries based on the position and batch size.
     */
	public List<T> getLoadBatch(int position, int batchSize){
	    synchronized (this) {
    		if (batchSize < 1){
    			throw new RuntimeException("batchSize "+batchSize+" < 1 ??!!");
    		}
    		
    		int relativePos = position - removedFromTop;
    		if (relativePos - batchSize < 0){
    			relativePos = 0;
    		}
    		if (relativePos > 0 && ((relativePos + batchSize)> list.size())){
    			relativePos = list.size() - batchSize;
    			if (relativePos < 0){
    				relativePos = 0;
    			}
    		}
    		
    	    return getBatch(relativePos, batchSize);
	    }
	}
	
    private List<T> getBatch(int relativePos, int batchSize) {

        ArrayList<T> batch = new ArrayList<T>();

		int count = 0;
		boolean removeFromTop = relativePos == 0;
		
		while(count < batchSize){
			if (list.isEmpty()){
				break;
			}
			WeakReference<T> weakEntry;
			if (removeFromTop){
				weakEntry = list.remove(relativePos);
				removedFromTop++;
				
			} else {
				if (relativePos >= list.size()){
					break;
				}
				weakEntry = list.get(relativePos);
				list.set(relativePos, null);
				relativePos++;
			}
			T ebi = weakEntry == null ? null : weakEntry.get();
			
			if (ebi != null){
				batch.add(ebi);
				count++;
			}
		}
		
		return batch;
	}
}
