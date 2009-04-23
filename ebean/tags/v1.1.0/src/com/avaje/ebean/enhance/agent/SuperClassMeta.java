package com.avaje.ebean.enhance.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SuperClassMeta {

	final LinkedHashMap<String,FieldMeta> fields;
	
	SuperClassMeta superClassMeta;

	boolean entity;
	
	final String className;

	final String superClassName;

	public SuperClassMeta(String className, String superClassName, boolean entity, final LinkedHashMap<String,FieldMeta> fields){
		this.className = className;
		this.superClassName = superClassName;
		this.entity = entity;
		this.fields = fields;
	}
	
//	public SuperClassMeta(Class<?> cls){
//		
//		fields = new LinkedHashMap<String,FieldMeta>();
//		className = cls.getName();
//		superClassName = cls.getSuperclass().getName();
//		
//		Field[] declaredFields = cls.getDeclaredFields();
//		for (Field f : declaredFields) {
//			
//			int mod = f.getModifiers();
//			if (Modifier.isStatic(mod)){
//				// not interested in static fields
//				
//			} else {
//				// potentially interested in this field
//				FieldMeta fm = new FieldMeta(f);
//				fields.put(fm.getName(), fm);
//			}
//		}
//		
//		// only works if the javax.persistence.Entity 
//		// annotation is in the classpath
//		Entity entityAnnotation = cls.getAnnotation(Entity.class);
//		entity = entityAnnotation != null;
//	}
	
	public boolean isCheckSuperClassForEntity() {
		if (entity){
			return !superClassName.equals(Object.class);
		}
		return false;
	}

	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		appendDescription(sb);
		return sb.toString();
	}
	
	private void appendDescription(StringBuilder sb) {
		sb.append(className);
		if (superClassMeta != null){
			sb.append(" : ");
			superClassMeta.appendDescription(sb);
		}
	}
	public boolean isEntity() {
		return entity;
	}	

	public void addAllFields(ArrayList<FieldMeta> list){
		if (entity){
			list.addAll(fields.values());		
		}
	}
	
	public FieldMeta getFieldMeta(String fieldName){
		FieldMeta fieldMeta = fields.get(fieldName);
		if (fieldMeta != null){
			return fieldMeta;
		}
		if (superClassMeta != null){
			return superClassMeta.getFieldMeta(fieldName);
		} else {
			return null;
		}
	}
	
}
