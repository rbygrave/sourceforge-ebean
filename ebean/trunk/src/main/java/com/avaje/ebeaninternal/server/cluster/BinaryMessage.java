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
package com.avaje.ebeaninternal.server.cluster;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class BinaryMessage {

    public static final int TYPE_MSGCONTROL = 0;
    public static final int TYPE_BEANIUD = 1;
    public static final int TYPE_TABLEIUD = 2;
    public static final int TYPE_MSGACK = 5;
    public static final int TYPE_MSGRESEND = 6;
    
    private final ByteArrayOutputStream buffer;
    private final DataOutputStream os;
    private byte[] bytes;
    
    public BinaryMessage(int bufSize) {
        this.buffer = new ByteArrayOutputStream(bufSize);
        this.os = new DataOutputStream(buffer);
    }

    public DataOutputStream getOs() {
        return os;
    }
    
    public byte[] getByteArray() {
        if (bytes == null){
            bytes = buffer.toByteArray();
        }
        return bytes;
    }
}
