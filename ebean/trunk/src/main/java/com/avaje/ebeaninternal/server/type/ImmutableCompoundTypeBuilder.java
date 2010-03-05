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
package com.avaje.ebeaninternal.server.type;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to build Immutable Compound Value objects.
 * <p>
 * The individual values are collected for a given type and when they have all
 * been collected then the immutable compound value object is created and
 * returned.
 * </p>
 * 
 * @author rbygrave
 * 
 */
public final class ImmutableCompoundTypeBuilder {

    private static ThreadLocal<ImmutableCompoundTypeBuilder> local = new ThreadLocal<ImmutableCompoundTypeBuilder>() {
        protected synchronized ImmutableCompoundTypeBuilder initialValue() {
            return new ImmutableCompoundTypeBuilder();
        }
    };

    private Map<Class<?>, Entry> entryMap = new HashMap<Class<?>, Entry>();

    /**
     * Clear the cache of partial compound objects.
     */
    public static void clear() {
        local.get().entryMap.clear();
    }

    /**
     * Set the value for the property of a compound type.
     * <p>
     * If this is the last value required for the compound type then the
     * compound type is created and returned, otherwise null is returned (and we
     * need more values set).
     * </p>
     */
    public static Object set(CtCompoundType<?> ct, String propName, Object value) {
        return local.get().setValue(ct, propName, value);
    }

    private Object setValue(CtCompoundType<?> ct, String propName, Object value) {

        Entry e = getEntry(ct);
        Object compoundValue = e.set(propName, value);
        if (compoundValue != null) {
            removeEntry(ct);
        }
        return compoundValue;
    }

    /**
     * Once we have built a compound value we remove the entry.
     */
    private void removeEntry(CtCompoundType<?> ct) {
        entryMap.remove(ct.getCompoundTypeClass());
    }

    /**
     * Get the Entry which contains the values collected so far for this type.
     */
    private Entry getEntry(CtCompoundType<?> ct) {
        Entry e = entryMap.get(ct.getCompoundTypeClass());
        if (e == null) {
            e = new Entry(ct);
            entryMap.put(ct.getCompoundTypeClass(), e);
        }
        return e;
    }

    /**
     * Holds the values collected so far for a given compound type.
     */
    private static class Entry {

        private final CtCompoundType<?> ct;

        private final Map<String, Object> valueMap;

        private Entry(CtCompoundType<?> ct) {
            this.ct = ct;
            this.valueMap = new HashMap<String, Object>();
        }

        private Object set(String propName, Object value) {
            // collect the values...
            valueMap.put(propName, value);

            // when got all the values this returns the
            // compound value, otherwise null
            return ct.create(valueMap);
        }
    }
}
