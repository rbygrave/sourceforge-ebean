package com.avaje.ebean.util;

import com.avaje.ebean.util.SortByClause.Property;

public final class SortByClauseParser {

//	private static final char COMMA = ',';
	
	private final String rawSortBy;
//	private final int maxLength;
//	
//	private int pos;
//	private boolean endOfSection;
	
	public static SortByClause parse(String rawSortByClause){
		return new SortByClauseParser(rawSortByClause).parse();
	}
	
	private SortByClauseParser(String rawSortByClause) {
		this.rawSortBy = rawSortByClause.trim();
//		this.maxLength = rawSortBy.length();		
	}
	
	private SortByClause parse(){
		
		SortByClause sortBy = new SortByClause();
		
		String[] sections = rawSortBy.split(",");
		for (int i = 0; i < sections.length; i++) {
			Property p = parseSection(sections[i].trim());
			if (p == null){
				break;
			} else {
				sortBy.add(p);
			}
			
		}
				
		return sortBy;
	}
	
	private Property parseSection(String section){
		if (section.length() == 0){
			return null;
		}
		String[] words = section.split(" ");
		if (words.length < 1 || words.length > 3){
			throw new RuntimeException("Expecting 1 to 3 words in ["+section+"] but got ["+words.length+"]");
		}
		
		Boolean nullsHigh = null;
		boolean ascending = true;
		String propName = words[0];
		if (words.length > 1){
			if (words[1].startsWith("nulls")){
				nullsHigh = isNullsHigh(words[1]);
				
			} else {
				ascending = isAscending(words[1]);
			}
		}
		if (words.length > 2){
			if (words[2].startsWith("nulls")){
				nullsHigh = isNullsHigh(words[2]);
				
			} else {
				ascending = isAscending(words[2]);
			}
		}
		
		return new Property(propName, ascending, nullsHigh);
	}
	
//	private Property nextProperty() {
//		
//		if (pos >= maxLength) {
//			return null;
//		}
//		
//		boolean ascending = true;
//		Boolean nullsHigh = null;
//		
//		String propertyName = nextWord();
//		if (pos < maxLength && !endOfSection){
//			// look for asc/desc/nullsHigh/nullsLow keyword
//			String nextWord = nextWord().toLowerCase();
//			if (nextWord.trim().length() == 0){
//				// ignore
//			} else if (nextWord.startsWith("nulls")){
//				nullsHigh = isNullsHigh(nextWord);
//				
//			} else {
//				ascending = isAscending(nextWord);
//			}
//		}
//		if (pos < maxLength && !endOfSection){
//			// look for asc/desc/nullsHigh/nullsLow keyword
//			String nextWord = nextWord().toLowerCase();
//			if (nextWord.trim().length() == 0){
//				// ignore
//			} else if (nextWord.startsWith("nulls")){
//				nullsHigh = isNullsHigh(nextWord);
//				
//			} else {
//				ascending = isAscending(nextWord);
//			}
//		}
//
//		if (pos < maxLength && !endOfSection){
//			// some trailing whitespace 
//			int x = pos;
//			if (!findEndOfSection()) {
//				String m = "Expecting to find a comma or eol after position "+x+" in ["+rawSortBy+"]";
//				throw new RuntimeException(m);
//			}
//		}
//		
//		return new Property(propertyName, ascending, nullsHigh);	
//	}

	private Boolean isNullsHigh(String word){
		if (SortByClause.NULLSHIGH.equalsIgnoreCase(word)){
			return Boolean.TRUE;
		}
		if (SortByClause.NULLSLOW.equalsIgnoreCase(word)){
			return Boolean.FALSE;
		}
		String m = "Expecting nullsHigh or nullsLow but got ["+word+"] in ["+rawSortBy+"]";
		throw new RuntimeException(m);
	}

	
	private boolean isAscending(String word){
		if (SortByClause.ASC.equalsIgnoreCase(word)){
			return true;
		}
		if (SortByClause.DESC.equalsIgnoreCase(word)){
			return false;
		}
		String m = "Expection ASC or DESC but got ["+word+"] in ["+rawSortBy+"]";
		throw new RuntimeException(m);
	}
	
//	private String nextWord() {
//	
//		if (pos >= maxLength){
//			return null;
//		}
//
//		// move to the next word
//		trimWhitespace();
//		
//		int startPos = pos;
//		
//		while (pos < maxLength){
//			char ch = rawSortBy.charAt(pos++);
//			if (ch == COMMA){
//				endOfSection = true;
//				return rawSortBy.substring(startPos,pos-1);
//				
//			} else if (Character.isWhitespace(ch)){
//				return rawSortBy.substring(startPos,pos-1);
//			}
//		}
//		
//		return rawSortBy.substring(startPos,pos);
//	}
//	
//	private boolean findEndOfSection() {
//		while (pos < maxLength){
//			char ch = rawSortBy.charAt(pos);
//			if (ch == COMMA){
//				pos++;
//				return true;
//			} else if (Character.isWhitespace(ch)){
//				pos++;
//			} else {
//				return false;
//			}
//		}	
//		return true;
//	}
//	
//	private void trimWhitespace() {
//		while (pos < maxLength){
//			char ch = rawSortBy.charAt(pos);
//			if (Character.isWhitespace(ch)){
//				pos++;
//			} else {
//				break;
//			}
//		}		
//	}
}
