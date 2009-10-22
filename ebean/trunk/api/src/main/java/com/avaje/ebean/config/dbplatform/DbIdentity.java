package com.avaje.ebean.config.dbplatform;

/**
 * Defines the identity/sequence behaviour for the database.
 */
public class DbIdentity {

	/**
	 * Set if this DB supports sequences. 
	 * Note some DB's support both Sequences and Identity.
	 */
	private boolean supportsSequence;
	
	private boolean supportsGetGeneratedKeys;
	
	private String sequenceNextValTemplate;
	
	private String selectLastInsertedIdTemplate;

	private String selectSequenceNextValSqlTemplate;
	
	private IdType idType = IdType.IDENTITY;
	
	public DbIdentity() {
	}
	
	/**
	 * Return the sequence nextval SQL given the sequence name.
	 */
	public String getSequenceNextVal(String sequence){
		if (sequenceNextValTemplate == null){
			return null;
		} 
		return sequenceNextValTemplate.replace("{sequence}", sequence);
	}

	/**
	 * Return the sequence nextval SQL given the sequence name.
	 */
	public String getSelectSequenceNextValSql(String sequenceNextVal){
		if (selectSequenceNextValSqlTemplate == null){
			return null;
		}
		return selectSequenceNextValSqlTemplate.replace("{sequencenextval}", sequenceNextVal);
	}

	/**
	 * Return true if GetGeneratedKeys is supported.
	 * <p>
	 * GetGeneratedKeys required to support JDBC batching transparently.
	 * </p>
	 */
	public boolean isSupportsGetGeneratedKeys() {
		return supportsGetGeneratedKeys;
	}

	/**
	 * Set if GetGeneratedKeys is supported.
	 */
	public void setSupportsGetGeneratedKeys(boolean supportsGetGeneratedKeys) {
		this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
	}

	/**
	 * Set the template used to get the sequence next val SQL.
	 * <p>
	 * Set a string that contains "{sequence}" where the sequence name goes.
	 * </p>
	 */
	public void setSequenceNextValTemplate(String sequenceNextValTemplate) {
		this.sequenceNextValTemplate = sequenceNextValTemplate;
	}

	/**
	 * Return the SQL query to find the SelectLastInsertedId.
	 * <p>
	 * This should only be set on databases that don't
	 * support GetGeneratedKeys.
	 * </p>
	 */
	public String getSelectLastInsertedId(String table) {
		if (selectLastInsertedIdTemplate == null){
			return null;
		}
		return selectLastInsertedIdTemplate.replace("{table}", table);
	}

	/**
	 * Set the template used to build the SQL query to return the LastInsertedId.
	 * <p>
	 * The template can contain "{table}" where the table name should be include in
	 * the sql query.
	 * </p>
	 * <p>
	 * This should only be set on databases that don't
	 * support GetGeneratedKeys.
	 * </p>
	 */
	public void setSelectLastInsertedIdTemplate(String selectLastInsertedIdTemplate) {
		this.selectLastInsertedIdTemplate = selectLastInsertedIdTemplate;
	}

	/**
	 * Return true if the database supports sequences.
	 */
	public boolean isSupportsSequence() {
		return supportsSequence;
	}

	/**
	 * Set to true if the database supports sequences.
	 * Generally this also means you want to set the default IdType
	 * to sequence (some DB's support both sequences and identity).
	 */
	public void setSupportsSequence(boolean supportsSequence, IdType idType) {
		this.supportsSequence = supportsSequence;
		this.idType = idType;
	}

	/**
	 * Return the default ID generation type that should be used.
	 * This should be either SEQUENCE or IDENTITY (aka Autoincrement).
	 * <p>
	 * Note: Id properties of type UUID automatically get a UUID 
	 * generator assigned to them.
	 * </p>
	 */
	public IdType getIdType() {
		return idType;
	}

	/**
	 * Set the default ID generation type that should be used.
	 */
	public void setIdType(IdType idType) {
		this.idType = idType;
	}

	/**
	 * Set a template used to create the SQL to fetch the sequence nextval.
	 * <p>
	 * {sequencenextval} is replaced in the template with the actual sequence 
	 * nextval expression.
	 * </p>
	 */
	public void setSelectSequenceNextValSqlTemplate(String selectSequenceNextValSqlTemplate) {
		this.selectSequenceNextValSqlTemplate = selectSequenceNextValSqlTemplate;
	}
	
	
}
