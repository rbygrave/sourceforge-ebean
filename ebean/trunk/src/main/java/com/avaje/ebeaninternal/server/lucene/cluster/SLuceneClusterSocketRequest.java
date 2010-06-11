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

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LIndexCommitInfo;
import com.avaje.ebeaninternal.server.lucene.LIndexFileInfo;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;

public class SLuceneClusterSocketRequest implements Runnable, SLuceneSocketMessageTypes {
    
    private static final Logger logger = Logger.getLogger(SLuceneClusterSocketRequest.class.getName());
    
    private final LuceneIndexManager manager;
    //private final Socket socket;

    private final OutputStream os;
    private final DataInput dataInput;
    private final DataOutput dataOutput;
    
    private final String serverName;
    private final short msgType;
    private final String idxName;
    private final long remoteIndexVersion;
    
    public SLuceneClusterSocketRequest(ClusterManager clusterManager, Socket socket)
        throws IOException {
        
        //this.socket = socket;   

        this.os = socket.getOutputStream();
        InputStream is = socket.getInputStream();        
        this.dataInput = new DataInputStream(is);
        this.dataOutput = new DataOutputStream(os);

        this.serverName = dataInput.readUTF();
        this.msgType = dataInput.readShort();
        this.idxName = dataInput.readUTF();
        this.remoteIndexVersion = dataInput.readLong();
        
        SpiEbeanServer server = (SpiEbeanServer)clusterManager.getServer(serverName);
        this.manager = server.getLuceneIndexManager();
    }

    public void run() {
        try {

            switch (msgType) {
            case OBTAIN_COMMIT:
                obtainCommit();
                break;

            case RELEASE_COMMIT:
                releaseCommit();
                break;

            case GET_FILE:
                getFile();
                break;
                
            default:
                throw new IOException("Invalid msgType "+msgType);
            }

        } catch (IOException e) {
            String msg = "Error processing msg "+msgType+" "+idxName;
            logger.log(Level.SEVERE, msg, e);
            
        } finally {
            flush();
        }
    }
    
    private void flush() {
        try {
            os.flush();
        } catch (IOException e){
            String msg = "Error flushing Socket OuputStream";
            logger.log(Level.SEVERE, msg, e);
        }
        try {
            os.close();
        } catch (IOException e){
            String msg = "Error closing Socket OuputStream";
            logger.log(Level.SEVERE, msg, e);
        }
//        try {
//            socket.close();
//        } catch (IOException e){
//            String msg = "Error closing Socket";
//            logger.log(Level.SEVERE, msg, e);
//        }
    }
    
    private void releaseCommit() throws IOException {
        LIndex index = manager.getIndex(idxName);
        index.releaseIndexCommit(remoteIndexVersion);
        dataOutput.writeBoolean(true);
    }
    
    private void obtainCommit() throws IOException {
        LIndex index = manager.getIndex(idxName);
        LIndexCommitInfo commitInfo = index.obtainLastIndexCommitIfNewer(remoteIndexVersion);
        if (commitInfo == null){
            // the index has not changed
            dataOutput.writeBoolean(false);
        } else {
            dataOutput.writeBoolean(true);
            commitInfo.write(dataOutput);
        }
    }
    
    private void getFile() throws IOException {
        LIndex index = manager.getIndex(idxName);
        String fileName = dataInput.readUTF();
        LIndexFileInfo fileInfo = index.getFile(remoteIndexVersion, fileName);
        
        File f = fileInfo.getFile();
        if (!f.exists()){
            dataOutput.writeBoolean(false);
        } else {
            dataOutput.writeBoolean(true);
            FileInputStream fis = new FileInputStream(f);
            try {
                byte[] buf = new byte[2048];
                BufferedInputStream bis = new BufferedInputStream(fis);
                
                int len = 0;
                while((len = bis.read(buf)) > -1){
                    dataOutput.write(buf, 0, len);
                }
                
            } finally {
                try {
                    fis.close();
                } catch (IOException e){
                    String msg = "Error closing InputStream on "+f.getAbsolutePath();
                    logger.log(Level.SEVERE, msg, e);
                }
            }
        }        
    }
}
