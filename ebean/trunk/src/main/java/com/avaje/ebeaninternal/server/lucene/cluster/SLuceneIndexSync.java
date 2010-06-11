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
package com.avaje.ebeaninternal.server.lucene.cluster;

import java.io.IOException;

import com.avaje.ebeaninternal.server.cluster.LuceneClusterIndexSync;
import com.avaje.ebeaninternal.server.lucene.LIndex;

public class SLuceneIndexSync implements LuceneClusterIndexSync {

    private Mode mode;
    
    private String masterHost;
    
    public boolean sync(LIndex index, String masterHost) throws IOException {
        
        SLuceneClusterSocketClient c = new SLuceneClusterSocketClient(index);
        if (c.isSynchIndex(masterHost)) {
            c.transferFiles();
            // get the index to refresh it's searchers
            index.refresh(true);
            return true;
        }
        return false;
    }

    public boolean isMaster() {
        return Mode.MASTER_MODE.equals(mode);
    }

    public String getMasterHost() {
        return masterHost;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
}
