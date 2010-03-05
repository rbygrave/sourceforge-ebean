/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.enhance.asm.ClassVisitor;
import com.avaje.ebean.enhance.asm.FieldVisitor;
import com.avaje.ebean.enhance.asm.Label;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;

/**
 * Generate the _ebean_getIntercept() method and field.
 */
public class InterceptField implements Opcodes, EnhanceConstants {

	/**
	 * Add the _ebean_intercept field.
	 */
	public static void addField(ClassVisitor cv) {

		FieldVisitor f1 = cv.visitField(ACC_PROTECTED, INTERCEPT_FIELD, L_INTERCEPT, null, null);
		f1.visitEnd();
	}

	/**
	 * Generate the _ebean_getIntercept() method.
	 * 
	 * <pre>
	 * public EntityBeanIntercept _ebean_getIntercept() {
	 * 	return _ebean_intercept;
	 * }
	 * </pre>
	 */
	public static void addGetterSetter(ClassVisitor cv, String className) {

		String lClassName = "L" + className + ";";

		MethodVisitor mv;
		Label l0, l1;

		mv = cv.visitMethod(ACC_PUBLIC, "_ebean_getIntercept", "()" + L_INTERCEPT, null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(1, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, INTERCEPT_FIELD, L_INTERCEPT);
		mv.visitInsn(ARETURN);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", lClassName, null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

	}
}
