package com.avaje.ebeaninternal.server.lucene;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.IndexCommit;

/**
 * Information about a IndexCommit held for the duration of a replication.
 * <p>
 * You must release the IndexCommit version after the replication process has
 * finished so that the IndexCommit can be released for deletion.
 * </p>
 */
public class LIndexCommitInfo {

    private final String indexDir;

    private final LIndexVersion version;

    private final List<LIndexFileInfo> fileInfo;

    public LIndexCommitInfo(String indexDir, IndexCommit indexCommit) {
        this.indexDir = indexDir;
        this.version = new LIndexVersion(indexCommit.getGeneration(), indexCommit.getVersion());
        this.fileInfo = createFileInfo(indexCommit);
    }

    public LIndexCommitInfo(String indexDir, LIndexVersion version, List<LIndexFileInfo> fileInfo) {
        this.indexDir = indexDir;
        this.version = version;
        this.fileInfo = fileInfo;
    }
    
    public String toString() {
        return indexDir+" "+version+" "+fileInfo;
    }
    
    public LIndexVersion getVersion() {
        return version;
    }

    public List<LIndexFileInfo> getFileInfo() {
        return fileInfo;
    }
    
    public static LIndexCommitInfo read(DataInput dataInput) throws IOException {
        
        String idxDir = dataInput.readUTF();
        long gen = dataInput.readLong();
        long ver = dataInput.readLong();
        int fileCount = dataInput.readInt();
        List<LIndexFileInfo> fileInfo = new ArrayList<LIndexFileInfo>(fileCount);
        
        for (int i = 0; i < fileCount; i++) {
            fileInfo.add(LIndexFileInfo.read(dataInput));
        }
        
        return new LIndexCommitInfo(idxDir, new LIndexVersion(gen, ver), fileInfo);
    }
    
    public void write(DataOutput dataOutput) throws IOException {
        
        dataOutput.writeUTF(indexDir);
        dataOutput.writeLong(version.getGeneration());
        dataOutput.writeLong(version.getVersion());
        
        int fileCount = fileInfo.size();
        dataOutput.writeInt(fileCount);
        for (int i = 0; i < fileInfo.size(); i++) {
            fileInfo.get(i).write(dataOutput);
        }
    }

    private List<LIndexFileInfo> createFileInfo(IndexCommit indexCommit) {

        try {
            Collection<String> fileNames = indexCommit.getFileNames();

            List<LIndexFileInfo> files = new ArrayList<LIndexFileInfo>(fileNames.size());
            for (String fileName : fileNames) {
                files.add(getFileInfo(fileName));
            }

            return files;

        } catch (IOException e) {
            throw new PersistenceLuceneException(e);
        }
    }

    private LIndexFileInfo getFileInfo(String fileName) {
        File f = new File(indexDir, fileName);
        return new LIndexFileInfo(f);
    }

}