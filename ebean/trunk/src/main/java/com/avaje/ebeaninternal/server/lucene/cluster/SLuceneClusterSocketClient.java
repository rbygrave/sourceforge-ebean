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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LIndexCommitInfo;
import com.avaje.ebeaninternal.server.lucene.LIndexFileInfo;
import com.avaje.ebeaninternal.server.lucene.LIndexVersion;

public class SLuceneClusterSocketClient implements SLuceneSocketMessageTypes {

    private static final Logger logger = Logger.getLogger(SLuceneClusterSocketClient.class.getName());
    
    private InetSocketAddress master;
    
    private LIndex index;
    
    private SLuceneClient client;
    
    private File tmpDir;
    
    private ArrayList<File> addFiles = new ArrayList<File>();
    
    private ArrayList<File> replaceFiles = new ArrayList<File>();

    public SLuceneClusterSocketClient(LIndex index) {
        this.index = index;
    }
    
    public boolean isSynchIndex(String masterHost) throws IOException {
        
        this.master = SocketClient.parseHostPort(masterHost);
       
        LIndexVersion localVersion = index.getLastestVersion();
        System.out.println("-- Got localVersion "+localVersion);
        
        SocketClient client = new SocketClient(master);
        
        String serverName = index.getBeanDescriptor().getServerName();
        this.client = new SLuceneClient(serverName, client, localVersion.getVersion(), index);
        
        try {
            LIndexCommitInfo commitInfo = getCommitInfo();
            if (commitInfo == null){
                logger.info("Lucene index up to date ["+index.getDefnName()+"]");
                return false;
            } 
            
            getCommitFiles(localVersion, commitInfo);
            return true;
            
        } catch (IOException e){
            String msg = "Error synch'ing index "+index.getDefnName();
            logger.log(Level.SEVERE, msg, e); 
            throw e;
        }
    }
    
    public void transferFiles() {
        File indexDir = index.getIndexDir();
        for (int i = 0; i < addFiles.size(); i++) {
            File addFile = addFiles.get(i);
            File destFile = new File(indexDir, addFile.getName());
            
            addFile.renameTo(destFile);
        }
        
        tmpDir.delete();
    }
    
    private void getCommitFiles(LIndexVersion localVersion, LIndexCommitInfo commitInfo) throws IOException {
        try {
            client.setRemoteVersion(commitInfo.getVersion().getVersion());
            
            //boolean newGen = localVersion.getGeneration() != commitInfo.getVersion().getGeneration();
            copyFiles(commitInfo);
            
        } finally {
            try {
                client.sendRelease();
            } catch (IOException e) {
                String msg = "Error sending release for index "+client.getIndex().getDefnName();
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }
    
    private void copyFiles(LIndexCommitInfo commitInfo) throws IOException {
        
        LIndex index = client.getIndex();
        
        File indexDir = index.getIndexDir();
        
        this.tmpDir = new File(indexDir, "tmp-"+System.currentTimeMillis());
        
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            String msg = "Could not create directory tmpDir: "+tmpDir;
            throw new IOException(msg);
        }
        
        List<LIndexFileInfo> fileInfo = commitInfo.getFileInfo();
        
        logger.info("Lucene index synchonizing from["+master+"] ver["+commitInfo.getVersion()+"] files["+fileInfo+"]");
        
        for (int i = 0; i < fileInfo.size(); i++) {
            
            LIndexFileInfo fi  = fileInfo.get(i);
            String fileName = fi.getName();
//            if (fileName.endsWith(".del")){
//                logger.info("... skip .del file ["+fi.getName()+"]"); 
//                
//            } else {
                LIndexFileInfo localFileInfo = index.getLocalFile(fileName);
                
                if (localFileInfo.exists()) {
                    if (localFileInfo.isMatch(fi)){
                        logger.info("... skip ["+fi.getName()+"]");                        
                    } else {
                        logger.warning("Lucene index file ["+fi.getName()+"] exists but not match size or lastModified?");  
                        downloadFile(false, fi);                        
                    }
                } else {
                    downloadFile(true, fi);
                }
//            }
        }
    }
        
    
    private void downloadFile(boolean addFile, LIndexFileInfo fi) throws IOException {
        try {
            
            String fileName = fi.getName();
            
            InputStream fileInputStream = client.sendGetFile(fileName);
            BufferedInputStream bufIs = new BufferedInputStream(fileInputStream);
               
            File fileOut = new File(tmpDir, fileName);
            FileOutputStream os = new FileOutputStream(fileOut);
            
            byte[] buf = new byte[2048];
            
            int totalLen = 0;
            int len = 0;
            while((len = bufIs.read(buf)) > -1){
                os.write(buf, 0, len);
                totalLen += len;
            }
            
            os.flush();
            os.close();
            
            fileOut.setLastModified(fi.getLastModified());
            System.out.println("got file len:"+totalLen+" "+fileName);
            
            if (addFile){
                addFiles.add(fileOut);
            } else {
                replaceFiles.add(fileOut);
            }
            
        } finally {
            client.disconnect();
        }
    }
    
    private LIndexCommitInfo getCommitInfo() throws IOException {
        try {
            boolean gotInfo = client.sendObtainCommit();
            if (!gotInfo){
                return null;
            } 
            
            return LIndexCommitInfo.read(client.getSocketClient().getDataInput());
            
        } finally {
            client.disconnect();
        }
    }
    

}
