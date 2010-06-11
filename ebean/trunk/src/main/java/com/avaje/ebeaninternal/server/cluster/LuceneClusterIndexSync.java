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

import java.io.IOException;

import com.avaje.ebeaninternal.server.lucene.LIndex;

public interface LuceneClusterIndexSync {

    public enum Mode {
        /**
         * Effective owner and commit's are notified to cluster.
         */
        MASTER_MODE,
        
        /**
         * Listens for commit's from Master to trigger synch.
         */
        SLAVE_MODE
    }
    
    public boolean sync(LIndex index, String master) throws IOException;

    public boolean isMaster();
    
    public Mode getMode();
    
    public void setMode(Mode mode);
    
    public String getMasterHost();
    
    public void setMasterHost(String masterHost);
    
}
