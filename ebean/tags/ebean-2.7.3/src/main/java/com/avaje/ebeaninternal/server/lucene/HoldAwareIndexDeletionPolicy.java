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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexDeletionPolicy;

public class HoldAwareIndexDeletionPolicy implements IndexDeletionPolicy {

    private static final Logger logger = Logger.getLogger(HoldAwareIndexDeletionPolicy.class.getName());

    private final Map<Long, CommitRefCount> commitRefCounts = new HashMap<Long, CommitRefCount>();

    private IndexCommit lastCommit;

    private final String indexDir;
    
    public HoldAwareIndexDeletionPolicy(String indexDir) {
        this.indexDir = indexDir;
    }
    
    /**
     * Deletes all commits except the most recent one.
     */
    public void onInit(List<? extends IndexCommit> commits) {
        onCommit(commits);
    }

    /**
     * Deletes all commits except the most recent one.
     */
    public void onCommit(List<? extends IndexCommit> commits) {
        synchronized (commitRefCounts) {
            int size = commits.size();

            lastCommit = commits.get(size - 1);
            // potentially should keep last optimised commit

            for (int i = 0; i < size - 1; i++) {
                IndexCommit indexCommit = commits.get(i);
                if (commitRefCounts.containsKey(indexCommit.getVersion())) {
                    // this indexCommit is currently held for replication
                } else {
                    // maybe delete this commit
                    potentialIndexCommitDelete(indexCommit);
                }
            }
        }
    }

    private void potentialIndexCommitDelete(IndexCommit indexCommit) {
        // Could add ability to use a Deletion Policy
        indexCommit.delete();
    }

    public long getLastVersion() {
        synchronized (commitRefCounts) {
            if (lastCommit == null){
                return 0;
            } else {
                return lastCommit.getVersion();
            }
        }
    }
    
    /**
     * Obtain and hold the last commit point if it is newer than the
     * remoteIndexVersion.
     * <p>
     * Returns null if the local index version is the same as the
     * remoteIndexVersion.
     * </p>
     */
    public LIndexCommitInfo obtainLastIndexCommitIfNewer(long remoteIndexVersion) {

        synchronized (commitRefCounts) {

            if (remoteIndexVersion != 0 && remoteIndexVersion == lastCommit.getVersion()) {
                // Remote index is current
                return null;
            }
            
            incRefIndexCommit(lastCommit);
            return new LIndexCommitInfo(indexDir, lastCommit);
        }
    }

    /**
     * Increment a reference counter for the duration of index replication.
     */
    private void incRefIndexCommit(IndexCommit indexCommit) {
        Long commitVersion = Long.valueOf(indexCommit.getVersion());
        CommitRefCount refCount = commitRefCounts.get(commitVersion);
        if (refCount == null) {
            refCount = new CommitRefCount();
            commitRefCounts.put(commitVersion, refCount);
        }
        refCount.inc();
    }

    /**
     * Release a commit that was obtained by obtainLastIndexCommitIfNewer().
     * This is typically done after an index replication has completed.
     */
    public void releaseIndexCommit(long indexCommitVersion) {
        synchronized (commitRefCounts) {
            Long commitVersion = Long.valueOf(indexCommitVersion);
            CommitRefCount refCount = commitRefCounts.get(commitVersion);
            if (refCount == null) {
                logger.log(Level.WARNING, "No Reference counter for indexCommitVersion: " + commitVersion);
            } else {
                if (refCount.dec() <= 0) {
                    // this IndexCommit can now be deleted (if desired)
                    // as no process is using it (and it's associated files)
                    commitRefCounts.remove(commitVersion);
                }
            }
        }
    }
    
    public void touch(long indexCommitVersion) {
        synchronized (commitRefCounts) {
            Long commitVersion = Long.valueOf(indexCommitVersion);
            CommitRefCount refCount = commitRefCounts.get(commitVersion);
            if (refCount == null) {
                logger.warning("No Reference counter for indexCommitVersion: " + commitVersion);
            } else {
                refCount.touch();
            }
        }
    }
    
    public long getLastTouched(long indexCommitVersion) {
        synchronized (commitRefCounts) {
            Long commitVersion = Long.valueOf(indexCommitVersion);
            CommitRefCount refCount = commitRefCounts.get(commitVersion);
            if (refCount == null) {
                return 0;
            } else {
                return refCount.getLastTouched();
            }
        }
    }
    
    private static class CommitRefCount {
        private int refCount;
        private long lastTouched = System.currentTimeMillis();
        
        public void inc() {
            ++refCount;
        }

        public int dec(){
            return --refCount;
        }

        public void touch() {
            lastTouched = System.currentTimeMillis();
        }

        public long getLastTouched() {
            return lastTouched;
        }


    }
    
}
