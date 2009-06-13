package com.avaje.ebean.config;


public class UnderscoreNamingConvention implements NamingConvention {

	public static final String DEFAULT_SEQ_FORMAT = "{table}_seq";
	
	final String sequenceFormat;

	final CamelUnderscore camelUnderscore = new CamelUnderscore();
	
	public UnderscoreNamingConvention(String sequenceFormat) {
		this.sequenceFormat = sequenceFormat;
	}
	
	public UnderscoreNamingConvention() {
		this(DEFAULT_SEQ_FORMAT);
	}
	

	/**
	 * Returns the last part of the class name.
	 */
	public String getTableNameFromClass(Class<?> beanClass) {
		
		String clsName = beanClass.getName();
		int dp = clsName.lastIndexOf('.');
		if (dp != -1) {
			clsName = clsName.substring(dp + 1);
		}

		return camelUnderscore.toUnderscoreFromCamel(clsName);
	}

	public String getColumnFromProperty(Class<?> beanClass, String beanPropertyName) {
		
		return camelUnderscore.toUnderscoreFromCamel(beanPropertyName);
	}

	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName) {
		
		return camelUnderscore.toCamelFromUnderscore(dbColumnName);
	}


	public String getSequenceName(String table) {
		
		return sequenceFormat.replace("{table}", table);
	}
	
	private static class CamelUnderscore {

	    /**
	     * Force toUnderscore to return in upper case.
	     */
	    final boolean forceUpperCase = false;
	    
	    final boolean digitsCompressed = true;
	    
	    /**
	     * Create the UnderscoreNameConverter.
	     */
	    private CamelUnderscore(){
	    }
	        
	    private String toUnderscoreFromCamel(String camelCase){

	        int lastUpper = -1;
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < camelCase.length(); i++) {
	            char c = camelCase.charAt(i);
	            if (Character.isDigit(c)) {
	                if (i > lastUpper+1 && !digitsCompressed){
	                    sb.append("_");
	                }
	                sb.append(c);
	                lastUpper = i;
	                
	            } else if (Character.isUpperCase(c)) {
	                if (i > lastUpper+1){
	                    sb.append("_");
	                }
	                sb.append(Character.toLowerCase(c));
	                lastUpper = i;
	            
	            } else {
	                sb.append(c);
	            }
	        }
	        String ret = sb.toString();
	        if (forceUpperCase){
	            ret = ret.toUpperCase();
	        } 
	        return ret;
	    }
	    
	    private String toCamelFromUnderscore(String underscore){
	        
	        StringBuffer result = new StringBuffer();
	        String[] vals = underscore.split("_");
	        
	        for (int i = 0; i < vals.length; i++) {
	            String lower = vals[i].toLowerCase();
	            if (i > 0){   
	                char c = Character.toUpperCase(lower.charAt(0));
	                result.append(c);
	                result.append(lower.substring(1));
	            } else {
	                result.append(lower);
	            }
	        }
	        
	        return result.toString();
	    }
	}

}
