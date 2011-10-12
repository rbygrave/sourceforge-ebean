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
package com.avaje.ebeaninternal.server.lucene;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

public class LIndexFileInfo {

    private transient final File file;
    
    private final String name;
    
    private final long length;
    
    private final long lastModified;
    
    public LIndexFileInfo(File file) {
        this.file = file;
        this.name = file.getName();
        this.length = file.length();
        this.lastModified = file.lastModified();
    }
    
    public LIndexFileInfo(String name, long length, long lastModified) {
        this.file = null;
        this.name = name;
        this.length = length;
        this.lastModified = lastModified;
    }

    public String toString(){
        return name+" length["+length+"] lastModified["+lastModified+"]";
    }
    
    public static LIndexFileInfo read(DataInput dataInput) throws IOException {
        String name = dataInput.readUTF();
        long len = dataInput.readLong();
        long lastMod = dataInput.readLong();
        return new LIndexFileInfo(name, len, lastMod);
    }
    
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(name);
        dataOutput.writeLong(length);
        dataOutput.writeLong(lastModified);
    }
    
    public boolean exists() {
        return file.exists();
    }
    
    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public long getLastModified() {
        return lastModified;
    }
    
    public boolean isMatch(LIndexFileInfo otherFile){
        return otherFile.length == length && otherFile.lastModified == lastModified;
    }
}
