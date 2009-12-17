package com.avaje.ebean.config.dbplatform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.BackgroundExecutor;

/**
 * Database sequence based IdGenerator.
 */
public abstract class SequenceIdGenerator implements IdGenerator {

	protected static final Logger logger = Logger.getLogger(SequenceIdGenerator.class.getName());

	/**
	 * Used to synchronise the idList access.
	 */
	protected final Object monitor = new Object();

	/**
	 * Used to synchronise background loading (loadBatchInBackground).
	 */
	protected final Object backgroundLoadMonitor = new Object();

	/**
	 * The actual sequence name.
	 */
	protected final String seqName;
	
	protected final DataSource dataSource;
	
	protected final BackgroundExecutor backgroundExecutor;

	protected final ArrayList<Integer> idList = new ArrayList<Integer>(50);
	
	protected int batchSize;
		
	protected int currentlyBackgroundLoading;
	
	/**
	 * Construct given a dataSource and sql to return the next sequence value.
	 */
	public SequenceIdGenerator(BackgroundExecutor be, DataSource ds, String seqName, int batchSize) {
		this.backgroundExecutor = be;
		this.dataSource = ds;
		this.seqName = seqName;
		this.batchSize = batchSize;
	}
	
	public abstract String getSql(int batchSize);

	/**
	 * Returns the sequence name.
	 */
	public String getName() {
		return seqName;
	}

	/**
	 * Returns true.
	 */
	public boolean isDbSequence() {
		return true;
	}

	/**
	 * If allocateSize is large load some sequences in a background thread.
	 * <p>
	 * For example, when inserting a bean with a cascade on a OneToMany with 
	 * many beans Ebean can call this to ensure .
	 * </p>
	 */
	public void preAllocateIds(int allocateSize) {
		if (allocateSize > batchSize){
			// only bother if allocateSize is bigger than
			// the normal loading batchSize 
			if (allocateSize > 100){
				// max out at 100 for now
				allocateSize = 100;
			}
			loadLargeAllocation(allocateSize);
		}
	}

	/**
	 * Called by preAllocateIds when we know that a large number of Id's is going to 
	 * be needed shortly.
	 */
	protected void loadLargeAllocation(final int allocateSize) {
		// preAllocateIds was called with a relatively large batchSize
		// so we will just go ahead and load those anyway in background
		backgroundExecutor.execute(new Runnable() {			
			public void run() {
				loadMoreIds(allocateSize);
			}
		});	
	}
	
	/**
	 * Return the next Id.
	 */
	public Object nextId() {
		synchronized (monitor) {
			
			if (idList.size() == 0){
				loadMoreIds(batchSize);
			}
			Integer nextId = idList.remove(0);
		
			if (idList.size() <= batchSize/2){
				loadBatchInBackground();
			}
			
			return nextId;
		}
	}

	/**
	 * Load another batch of Id's using a background thread.
	 */
	protected void loadBatchInBackground() {
		
		// single threaded processing...
		synchronized (backgroundLoadMonitor) {
			
			if (currentlyBackgroundLoading > 0){
				// skip as already background loading 
				if (logger.isLoggable(Level.FINE)){
					logger.log(Level.FINE, "... skip background sequence load (another load in progress)");
				}
				return;
			}
			
			currentlyBackgroundLoading = batchSize;
			
			backgroundExecutor.execute(new Runnable() {			
				public void run() {
					loadMoreIds(batchSize);
					synchronized (backgroundLoadMonitor) {
						currentlyBackgroundLoading = 0;	
					}
				}
			});
		}
	}
	
	protected void loadMoreIds(final int numberToLoad) {
		
		ArrayList<Integer> newIds = getMoreIds(numberToLoad);
		
		if (logger.isLoggable(Level.FINE)){
			logger.log(Level.FINE, "... seq:"+seqName+" loaded:"+numberToLoad+" ids:"+newIds);
		}

		synchronized (monitor) {
			for (int i = 0; i < newIds.size(); i++) {
				idList.add(newIds.get(i));
			}
		}
	}
		
	/**
	 * Get more Id's by executing a query and reading the Id's returned.
	 */
	protected ArrayList<Integer> getMoreIds(int loadSize){
		
		String sql = getSql(loadSize);
		
		ArrayList<Integer> newIds = new ArrayList<Integer>(loadSize);
		
		Connection c = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		try {
			c = dataSource.getConnection();
			pstmt = c.prepareStatement(sql);
			rset = pstmt.executeQuery();
			while (rset.next()){
				int val = rset.getInt(1);
				newIds.add(Integer.valueOf(val));
			} 
			if (newIds.size() == 0){
				String m = "Always expecting more than 1 row from "+sql;
				throw new PersistenceException(m);
			}
			
			return newIds;
			
		} catch (SQLException e){
		    if (e.getMessage().contains("Database is already closed")){
		        String msg = "Error getting SEQ when DB shutting down "+e.getMessage();
		        logger.info(msg);
		        System.out.println(msg);
		        return newIds;
		    } else {
		        throw new PersistenceException("Error getting sequence nextval", e);
		    }
		} finally {
			closeResources(c, pstmt, rset);
		}
	}

	/**
	 * Close the JDBC resources.
	 */
	protected void closeResources(Connection c, PreparedStatement pstmt,ResultSet rset) {
		try {
			if (rset != null){
				rset.close();
			}
		} catch (SQLException e){
			logger.log(Level.SEVERE, "Error closing ResultSet", e);
		}
		try {
			if (pstmt != null){
				pstmt.close();
			}
		} catch (SQLException e){
			logger.log(Level.SEVERE, "Error closing PreparedStatement", e);
		}
		try {
			if (c != null){
				c.close();
			}
		} catch (SQLException e){
			logger.log(Level.SEVERE, "Error closing Connection", e);
		}		
	}
	
}
