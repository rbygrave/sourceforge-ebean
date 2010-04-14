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
package com.avaje.ebeaninternal.server.lib.sql;


class ArrayBuffer<E> {

    E[] elementData;

    int size;
    
    @SuppressWarnings("unchecked")
    protected ArrayBuffer(int initialCapacity) {
        elementData = (E[])new Object[initialCapacity];
    }
    
    protected boolean isEmpty() {
        return size == 0;

    }
    
    protected E get(int i){
        return elementData[i];
    }
    
    protected int size() {
        return size;
    }
    
    protected int add(E e){
        ensureCapacity(size+1);
        elementData[size++] = e;
        return size;
    }
    
    protected E remove() {
        --size;
        E e = elementData[size];
        elementData[size] = null;
        return e;
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity(int minCapacity) {
        
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            Object oldData[] = elementData;
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            elementData = (E[]) new Object[newCapacity];
            System.arraycopy(oldData, 0, elementData, 0, size);
        }
    }
    
    public boolean remove(Object o) {
        
        for (int index = 0; index < size; index++) {
            if (o == elementData[index]) {
                fastRemove(index);
                return true;
            }
        }
    
        return false;
    }

    private void fastRemove(int index) {
       
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        }
        elementData[--size] = null; // Let gc do its work
    }

}
