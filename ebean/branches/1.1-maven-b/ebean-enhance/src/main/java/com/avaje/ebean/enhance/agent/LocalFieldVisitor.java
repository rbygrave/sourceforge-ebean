package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.Attribute;
import com.avaje.ebean.enhance.asm.ClassVisitor;
import com.avaje.ebean.enhance.asm.EmptyVisitor;
import com.avaje.ebean.enhance.asm.FieldVisitor;

/**
 * Used to collect information about a field (specifically from field annotations).
 */
public class LocalFieldVisitor  implements FieldVisitor {

	private static final EmptyVisitor emptyVisitor = new EmptyVisitor();
	
	final ClassVisitor cv;
	
	final FieldVisitor fv;
	
	final FieldMeta fieldMeta;
	
	/**
	 * Constructor used for subclass generation.
	 * @param fieldMeta the fieldMeta data
	 */
	public LocalFieldVisitor(FieldMeta fieldMeta) {
		this.cv = null;
		this.fv = null;
		this.fieldMeta = fieldMeta;
	}
	
	/**
	 * Constructor used for agent class enhancement.
	 * @param cv the classVisitor used to write
	 * @param fv the fieldVisitor used to write
	 * @param fieldMeta the fieldMeta data
	 */
	public LocalFieldVisitor(ClassVisitor cv, FieldVisitor fv, FieldMeta fieldMeta) {
		this.cv = cv;
		this.fv = fv;
		this.fieldMeta = fieldMeta;
	}
	
	public boolean isPersistentSetter(String methodDesc){
		return fieldMeta.isPersistentSetter(methodDesc);
	}
	
	public boolean isPersistentGetter(String methodDesc){
		return fieldMeta.isPersistentGetter(methodDesc);
	}
	
	/**
	 * Return the field name.
	 */
	public String getName() {
		return fieldMeta.fieldName;
	}
	
	/**
	 * Return the Meta data for this field.
	 */
	public FieldMeta getFieldMeta() {
		return fieldMeta;
	}

	/**
	 * Add ebean get and set methods.
	 */
	public void addGetSetMethods(ClassMeta classMeta){
		fieldMeta.addGet(cv, classMeta);
		fieldMeta.addSet(cv, classMeta);
	}
	
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {	
		fieldMeta.addAnnotationDesc(desc);
		if (fv != null){
			return fv.visitAnnotation(desc, visible);			
		} else {
			return emptyVisitor;
		}
	}

	public void visitAttribute(Attribute attr) {
		if (fv != null){
			fv.visitAttribute(attr);
		}
	}

	public void visitEnd() {
		if (fv != null){
			fv.visitEnd();
		}
	}

	
	
}
