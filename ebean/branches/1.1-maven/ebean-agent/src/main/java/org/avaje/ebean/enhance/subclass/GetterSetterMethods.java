package org.avaje.ebean.enhance.subclass;

import java.util.List;

import org.avaje.ebean.enhance.agent.ClassMeta;
import org.avaje.ebean.enhance.agent.EnhanceConstants;
import org.avaje.ebean.enhance.agent.FieldMeta;
import org.avaje.ebean.enhance.asm.ClassVisitor;
import org.avaje.ebean.enhance.asm.Opcodes;

public class GetterSetterMethods implements Opcodes, EnhanceConstants {

	/**
	 * Add getters and setters to for interception.
	 * <p>
	 * Note that we don't intercept Id properties and we don't intercept setters
	 * on 'OneToMany' properties etc.
	 * </p>
	 */
	public static void add(ClassVisitor cv, ClassMeta classMeta) {

		List<FieldMeta> localFields = classMeta.getLocalFields();
		for (int x = 0; x < localFields.size(); x++) {
			FieldMeta fieldMeta = localFields.get(x);
			fieldMeta.addPublicGetSetMethods(cv, classMeta, true);
		}

		List<FieldMeta> inheritedFields = classMeta.getInheritedFields();
		for (int i = 0; i < inheritedFields.size(); i++) {
			FieldMeta fieldMeta = inheritedFields.get(i);
			// for persistent inherited fields add a
			// getter and setter to enable interception
			fieldMeta.addPublicGetSetMethods(cv, classMeta, false);
		}
	}

}
