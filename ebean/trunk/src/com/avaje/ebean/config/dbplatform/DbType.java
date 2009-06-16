package com.avaje.ebean.config.dbplatform;

/**
 * Represents a DB type with name, length, precision, and scale.
 * <p>
 * The length is for VARCHAR types and precision/scale for DECIMAL types.
 * </p>
 */
public class DbType {

	/**
	 * The data type name (VARCHAR, INTEGER ...)
	 */
	String name;

	/**
	 * The default length or precision.
	 */
	int defaultLength;

	/**
	 * The default scale (decimal).
	 */
	int defaultScale;

	public DbType(String name) {
		this.name = name;
	}

	public DbType(String name, int defaultLength) {
		this.name = name;
		this.defaultLength = defaultLength;
	}

	public DbType(String name, int defaultPrecision, int defaultScale) {
		this.name = name;
		this.defaultLength = defaultPrecision;
		this.defaultScale = defaultScale;
	}

	/**
	 * Return the type for a specific property that incorporates the name,
	 * length, precision and scale.
	 * <p>
	 * The deployLength and deployScale are for the property we are rendering
	 * the DB type for.
	 * </p>
	 * 
	 * @param deployLength
	 *            the length or precision defined by deployment on a specific
	 *            property.
	 * @param deployScale
	 *            the scale defined by deployment on a specific property.
	 */
	public String renderType(int deployLength, int deployScale) {

		StringBuilder sb = new StringBuilder();
		sb.append(name);

		int len = deployLength != 0 ? deployLength : defaultLength;

		if (len > 0) {
			sb.append("(");
			sb.append(len);
			int scale = deployScale != 0 ? deployScale : defaultScale;
			if (scale > 0) {
				sb.append(",");
				sb.append(scale);
			}
			sb.append(")");
		}

		return sb.toString();
	}

}
