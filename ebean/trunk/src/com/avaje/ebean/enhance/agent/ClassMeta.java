package com.avaje.ebean.enhance.agent;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.ClassVisitor;
import com.avaje.ebean.enhance.asm.EmptyVisitor;
import com.avaje.ebean.enhance.asm.FieldVisitor;
import com.avaje.ebean.enhance.asm.MethodVisitor;

/**
 * Holds the meta data for an entity bean class that is being enhanced.
 */
public class ClassMeta {

	static final Logger logger = Logger.getLogger(ClassMeta.class.getName());

	final PrintStream logout;

	final int logLevel;

	final boolean subclassing;

	String className;

	String superClassName;

	ClassMeta superMeta;

	boolean alreadyEnhanced;

	boolean hasEqualsOrHashcode;

	boolean hasDefaultConstructor;

	HashSet<String> existingSuperMethods = new HashSet<String>();

	LinkedHashMap<String, FieldMeta> fields = new LinkedHashMap<String, FieldMeta>();

	HashSet<String> classAnnotation = new HashSet<String>();

	AnnotationInfo classAnnotationInfo = new AnnotationInfo(null);

	ArrayList<MethodMeta> methodMetaList = new ArrayList<MethodMeta>();

	public ClassMeta(boolean subclassing, int logLevel, PrintStream logout) {
		this.subclassing = subclassing;
		this.logLevel = logLevel;
		this.logout = logout;
	}

	/**
	 * Return the transactional annotation information for a matching interface method.
	 */
	public AnnotationInfo getInterfaceTransactionalInfo(String methodName, String methodDesc) {

		AnnotationInfo annotationInfo = null;

		for (int i = 0; i < methodMetaList.size(); i++) {
			MethodMeta meta = methodMetaList.get(i);
			if (meta.isMatch(methodName, methodDesc)) {
				if (annotationInfo != null) {
					String msg = "Error in [" + className + "] searching the transactional methods[" + methodMetaList
							+ "] found more than one match for the transactional method:" + methodName + " "
							+ methodDesc;
					
					logger.log(Level.SEVERE, msg);

				} else {
					annotationInfo = meta.getAnnotationInfo();
					if (isLog(9)){
						log("... found transactional info from interface "+className+" "+methodName+" "+methodDesc);
					}
				}
			}
		}

		return annotationInfo;
	}

	public boolean isCheckSuperClassForEntity() {
		if (isEntity()) {
			return !superClassName.equals(Object.class);
		}
		return false;
	}

	public String toString() {
		return className;
	}

	public boolean isTransactional() {
		if (classAnnotation.contains(EnhanceConstants.AVAJE_TRANSACTIONAL_ANNOTATION)) {
			return true;
		}
		return false;
	}

	public ArrayList<MethodMeta> getMethodMeta() {
		return methodMetaList;
	}

	public void setClassName(String className, String superClassName) {
		this.className = className;
		this.superClassName = superClassName;
	}

	public String getSuperClassName() {
		return superClassName;
	}

	public boolean isSubclassing() {
		return subclassing;
	}

	public boolean isLog(int level) {
		return level <= logLevel;
	}

	public void log(String msg) {
		if (className != null) {
			msg = "cls: " + className + "  msg: " + msg;
		}
		logout.println("transform> " + msg);
	}

	/**
	 * Return true if we are enhancing (not subclassing) and the super class is
	 * also an entity.
	 * <p>
	 * In this case we will not add the identity based methods because we will
	 * inherit this from the enhanced super class.
	 * </p>
	 */
	public boolean isInheritEqualsFromSuper() {
		return !subclassing && isSuperClassEntity();
	}

	public ClassMeta getSuperMeta() {
		return superMeta;
	}

	public void setSuperMeta(ClassMeta superMeta) {
		this.superMeta = superMeta;
	}

	/**
	 * Set to true if the class has an existing equals() or hashcode() method.
	 */
	public void setHasEqualsOrHashcode(boolean hasEqualsOrHashcode) {
		this.hasEqualsOrHashcode = hasEqualsOrHashcode;
	}

	public boolean hasEqualsOrHashCode() {
		return hasEqualsOrHashcode;
	}

	/**
	 * Return true if the field is a persistent field.
	 */
	public boolean isFieldPersistent(String fieldName) {

		FieldMeta f = fields.get(fieldName);
		if (f != null) {
			return f.isPersistent();
		}
		if (superMeta == null) {
			// the field is unknown?
			return false;

		} else {
			// look in the inheritance hierarchy
			return superMeta.isFieldPersistent(fieldName);
		}
	}

	/**
	 * Return the list of fields local to this type (not inherited).
	 */
	public List<FieldMeta> getLocalFields() {

		ArrayList<FieldMeta> list = new ArrayList<FieldMeta>();

		Iterator<FieldMeta> it = fields.values().iterator();
		while (it.hasNext()) {
			FieldMeta fm = it.next();
			if (!fm.isObjectArray()) {
				// add field local to this entity type
				list.add(fm);
			}
		}

		return list;
	}

	/**
	 * Add all fields to the list.
	 */
	public void addAllFields(ArrayList<FieldMeta> list) {
		if (isEntity()) {
			list.addAll(fields.values());
		}
	}

	/**
	 * Return the list of fields inherited from super types that are entities.
	 */
	public List<FieldMeta> getInheritedFields() {

		ArrayList<FieldMeta> list = new ArrayList<FieldMeta>();

		if (superMeta != null) {
			superMeta.addAllFields(list);
		}
		return list;
	}

	/**
	 * Return the list of all fields including ones inherited from entity super
	 * types.
	 */
	public List<FieldMeta> getAllFields() {

		ArrayList<FieldMeta> list = new ArrayList<FieldMeta>();

		if (superMeta != null) {
			superMeta.addAllFields(list);
		}

		Iterator<FieldMeta> it = fields.values().iterator();
		while (it.hasNext()) {
			FieldMeta fm = it.next();
			if (!fm.isObjectArray()) {
				// add field local to this entity type
				list.add(fm);
			}
		}

		return list;
	}

	/**
	 * Add field level get set methods for each field.
	 */
	public void addFieldGetSetMethods(ClassVisitor cv) {
		if (isEntityEnhancementRequired()) {

			Iterator<FieldMeta> it = fields.values().iterator();
			while (it.hasNext()) {
				FieldMeta fm = it.next();
				fm.addGetSetMethods(cv, this);
			}
		}
	}

	/**
	 * Return true if this class should be enhanced.
	 */
	public boolean isEnhancementRequired() {
		if (isEntityEnhancementRequired()) {
			return true;
		}
		// TODO: Add declarative Transaction enhancement option
		return false;
	}

	/**
	 * Return true if the class has an Entity, Embeddable or MappedSuperclass annotation.
	 */
	public boolean isEntity() {
		if (classAnnotation.contains(EnhanceConstants.ENTITY_ANNOTATION)) {
			return true;
		}
		if (classAnnotation.contains(EnhanceConstants.EMBEDDABLE_ANNOTATION)) {
			return true;
		}
		if (classAnnotation.contains(EnhanceConstants.MAPPEDSUPERCLASS_ANNOTATION)) {
			return true;
		}
		return false;
	}

	/**
	 * Return true for classes not already enhanced and yet annotated with entity, embeddable or mappedSuperclass.
	 */
	public boolean isEntityEnhancementRequired() {
		if (alreadyEnhanced) {
			return false;
		}
		if (classAnnotation.contains(EnhanceConstants.ENTITY_ANNOTATION)) {
			return true;
		}
		if (classAnnotation.contains(EnhanceConstants.EMBEDDABLE_ANNOTATION)) {
			return true;
		}
		if (classAnnotation.contains(EnhanceConstants.MAPPEDSUPERCLASS_ANNOTATION)) {
			return true;
		}
		return false;
	}

	/**
	 * Return the className of this entity class.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Return true if this entity bean has a super class that is an entity.
	 */
	public boolean isSuperClassEntity() {
		if (superMeta == null) {
			return false;
		} else {
			return superMeta.isEntity();
		}
	}

	/**
	 * Add a class annotation.
	 */
	public void addClassAnnotation(String desc) {
		classAnnotation.add(desc);
	}

	/**
	 * Only for subclassing, add known methods on the original entity class.
	 * <p>
	 * Used to check that the methods exist. They may not in special cases such
	 * as entity beans that use a finder etc with read only properties.
	 * </p>
	 */
	public void addExistingSuperMethod(String methodName, String methodDesc) {
		existingSuperMethods.add(methodName + methodDesc);
	}

	/**
	 * Only for subclassing return true if the method exists on the original
	 * entity class.
	 */
	public boolean isExistingSuperMethod(String methodName, String methodDesc) {
		return existingSuperMethods.contains(methodName + methodDesc);
	}

	public MethodVisitor createMethodVisitor(MethodVisitor mv, int access, String name, String desc) {

		MethodMeta methodMeta = new MethodMeta(classAnnotationInfo, access, name, desc);
		methodMetaList.add(methodMeta);

		return new MethodReader(mv, methodMeta);
	}

	private final class MethodReader extends EmptyVisitor {
		final MethodVisitor mv;
		final MethodMeta methodMeta;

		MethodReader(MethodVisitor mv, MethodMeta methodMeta) {
			this.mv = mv;
			this.methodMeta = methodMeta;
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = mv.visitAnnotation(desc, visible);

			return new AnnotationInfoVisitor(null, methodMeta.annotationInfo, av);
		}

	}

	/**
	 * Create and return a read only fieldVisitor for subclassing option.
	 */
	public FieldVisitor createLocalFieldVisitor(String name, String desc) {
		return createLocalFieldVisitor(null, null, name, desc);
	}

	/**
	 * Create and return a new fieldVisitor for use when enhancing a class.
	 */
	public FieldVisitor createLocalFieldVisitor(ClassVisitor cv, FieldVisitor fv, String name, String desc) {

		FieldMeta fieldMeta = new FieldMeta(name, desc);
		LocalFieldVisitor localField = new LocalFieldVisitor(cv, fv, fieldMeta);
		if (name.startsWith("_ebean")) {
			// can occur when reading inheritance information on
			// a entity that has already been enhanced
			if (isLog(0)) {
				log("... ignore field " + name);
			}
		} else {
			fields.put(localField.getName(), fieldMeta);
		}
		return localField;
	}

	public boolean isAlreadyEnhanced() {
		return alreadyEnhanced;
	}

	public void setAlreadyEnhanced(boolean alreadyEnhanced) {
		this.alreadyEnhanced = alreadyEnhanced;
	}

	public boolean hasDefaultConstructor() {
		return hasDefaultConstructor;
	}

	public void setHasDefaultConstructor(boolean hasDefaultConstructor) {
		this.hasDefaultConstructor = hasDefaultConstructor;
	}

	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		appendDescription(sb);
		return sb.toString();
	}

	private void appendDescription(StringBuilder sb) {
		sb.append(className);
		if (superMeta != null) {
			sb.append(" : ");
			superMeta.appendDescription(sb);
		}
	}
}
