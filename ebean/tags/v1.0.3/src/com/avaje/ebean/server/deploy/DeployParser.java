package com.avaje.ebean.server.deploy;


/**
 * Converts logical property names to database columns.
 */
public abstract class DeployParser {

	/**
	 * used to identify sql literal.
	 */
	static final char SINGLE_QUOTE = '\'';

	/**
	 * used to identify query named parameters.
	 */
	static final char COLON = ':';

	/**
	 * Used to determine when a column name terminates.
	 */
	static final char UNDERSCORE = '_';

	/**
	 * Used to determine when a column name terminates.
	 */
	static final char PERIOD = '.';

	String source;

	StringBuilder sb;

	int sourceLength;

	int pos;

	String word;

	char wordTerminator;

	public abstract String convertWord();

	public String parse(String source) {
		pos = -1;
		this.source = source;
		this.sourceLength = source.length();
		this.sb = new StringBuilder(source.length() + 20);

		while (nextWord()) {
			String deployWord = convertWord();
			sb.append(deployWord);
			if (pos < sourceLength) {
				sb.append(wordTerminator);
			}
		}

		return sb.toString();
	}


	private boolean nextWord() {

		if (!findWordStart()) {
			return false;
		}

		StringBuilder wordBuffer = new StringBuilder();
		wordBuffer.append(source.charAt(pos));
		while (++pos < sourceLength) {
			char ch = source.charAt(pos);
			if (isWordPart(ch)) {
				wordBuffer.append(ch);
			} else {
				wordTerminator = ch;
				break;
			}
		}

		word = wordBuffer.toString();

		return true;
	}

	private boolean findWordStart() {
		while (++pos < sourceLength) {
			char ch = source.charAt(pos);
			if (ch == SINGLE_QUOTE) {
				// read a literal value and just
				// append to string builder
				sb.append(ch);
				readLiteral();
			} else if (ch == COLON) {
				// read a named parameter
				// just append to string builder
				sb.append(ch);
				readNamedParameter();
			} else if (isWordStart(ch)) {
				// its the start of a word that could
				// be translated
				return true;
			} else {
				sb.append(ch);
			}
		}
		return false;
	}

	/**
	 * Read the rest of a literal value. These do not get translated so are just
	 * read and appended to the string builder.
	 */
	private void readLiteral() {
		while (++pos < sourceLength) {
			char ch = source.charAt(pos);
			sb.append(ch);
			if (ch == SINGLE_QUOTE) {
				break;
			}
		}
	}

	/**
	 * Read a named parameter. These are not translated. They will be replaced
	 * by positioned parameters later.
	 */
	private void readNamedParameter() {
		while (++pos < sourceLength) {
			char ch = source.charAt(pos);
			sb.append(ch);
			if (Character.isWhitespace(ch)) {
				break;
			} else if (ch == ',') {
				break;
			}
		}
	}

	/**
	 * return true if the char is a letter, digit or underscore.
	 */
	private boolean isWordPart(char ch) {
		if (Character.isLetterOrDigit(ch)) {
			return true;

		} else if (ch == UNDERSCORE) {
			return true;

		} else if (ch == PERIOD) {
			return true;

		} else {
			return false;
		}
	}

	private boolean isWordStart(char ch) {
		if (Character.isLetter(ch)) {
			return true;

		} else {
			return false;
		}
	}
}
