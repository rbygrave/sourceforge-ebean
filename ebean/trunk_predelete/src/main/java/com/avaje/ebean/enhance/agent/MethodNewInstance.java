/**
 * Copyright (C) 2009 Authors
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
import com.avaje.ebean.enhance.asm.Label;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;

/**
 * Adds the _ebean_newInstance() method.
 * 
 * @author rbygrave
 */
public class MethodNewInstance {

    /**
     * Add the _ebean_newInstance() method.
     */
    public static void addMethod(ClassVisitor cv, ClassMeta classMeta) {

        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "_ebean_newInstance", "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(10, l0);
        mv.visitTypeInsn(Opcodes.NEW, classMeta.getClassName());
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classMeta.getClassName(), "<init>", "()V");
        mv.visitInsn(Opcodes.ARETURN);
        
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L"+classMeta.getClassName()+";", null, l0, l1, 0);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }
}
