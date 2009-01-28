package org.avaje.ebean.server.deploy;

import java.util.Map;

/**
 * For updates converts logical property names to database columns and bean type to base table.
 */
public final class DeployUpdateParser extends DeployParser {


	private final Map<String, String> deployMap;


	public DeployUpdateParser(final Map<String, String> deployMap) {
		this.deployMap = deployMap;
	}

	public String convertWord() {

		String dbWord = deployMap.get(word.toLowerCase());

		if (dbWord != null) {
			return dbWord;
		} 
		// maybe tableAlias.propertyName
		return convertSubword(0, word, null);			
	}

	private String convertSubword(int start, String currentWord, StringBuilder localBuffer) {
		
		int dotPos = currentWord.indexOf('.', start);
		if (start == 0 && dotPos == -1){
			return currentWord;
		}
		if (start == 0){
			localBuffer = new StringBuilder();
		}
		if (dotPos == -1){
			// no match... 
			localBuffer.append(currentWord.substring(start));
			return localBuffer.toString();
		}
		
		// append up to the dot
		localBuffer.append(currentWord.substring(start, dotPos+1));
		
		if (dotPos == currentWord.length()-1){
			// ends with a "." ???
			return localBuffer.toString();
		}
		
		// get the remainder after the dot
		start = dotPos+1;
		String remainder = currentWord.substring(start, currentWord.length());
		
		String dbWord = deployMap.get(remainder.toLowerCase());
		if (dbWord != null){
			// we have found a match for the remainder
			localBuffer.append(dbWord);
			return localBuffer.toString();
		} else {
			//
			return convertSubword(start, currentWord, localBuffer);
		}
	}
	
	
}
