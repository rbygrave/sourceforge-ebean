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
package com.avaje.ebean.config.dbplatform;

import com.avaje.ebean.config.GlobalProperties;


/**
 * Oracle encryption support.
 * 
 * <p>
 * You will typically need to create your own encryption and decryption
 * functions similar to the example ones below.
 * </p>
 * 
 * <pre class="code">
 * 
 *  // Remember your DB user needs execute privilege on DBMS_CRYPTO 
 *  // as well as your encryption and decryption functions
 *  
 *  
 *  // This is an Example Encryption function only - please create your own.
 * 
 * CREATE OR REPLACE FUNCTION eb_encrypt(data IN VARCHAR, key in VARCHAR) RETURN RAW IS
 * 
 *     encryption_mode NUMBER := DBMS_CRYPTO.ENCRYPT_AES128 + DBMS_CRYPTO.CHAIN_CBC  + DBMS_CRYPTO.PAD_PKCS5;
 * 
 *     BEGIN
 *          RETURN DBMS_CRYPTO.ENCRYPT(UTL_I18N.STRING_TO_RAW (data, 'AL32UTF8'), 
 *            encryption_mode, UTL_I18N.STRING_TO_RAW(key, 'AL32UTF8') );
 *     END;
 *     /
 *     
 *     
 *     
 *  // This is an Example Decryption function only - please create your own.
 *     
 * CREATE OR REPLACE FUNCTION eb_decrypt(data IN RAW, key IN VARCHAR) RETURN VARCHAR IS
 * 
 *     encryption_mode NUMBER := DBMS_CRYPTO.ENCRYPT_AES128 + DBMS_CRYPTO.CHAIN_CBC  + DBMS_CRYPTO.PAD_PKCS5;
 * 
 *     BEGIN
 *          RETURN UTL_RAW.CAST_TO_VARCHAR2(DBMS_CRYPTO.DECRYPT
 *            (data, encryption_mode, UTL_I18N.STRING_TO_RAW(key, 'AL32UTF8')));
 *     END;
 *     /
 * </pre>
 * 
 * @author rbygrave
 */
public class Oracle10DbEncrypt extends AbstractDbEncrypt {

    /**
     * Constructs the Oracle10DbEncrypt.
     */
    public Oracle10DbEncrypt() {
        
        String encryptfunction = GlobalProperties.get("ebean.oracle.encryptfunction", "eb_encrypt");
        String decryptfunction = GlobalProperties.get("ebean.oracle.decryptfunction", "eb_decrypt");
        
        this.varcharEncryptFunction = new OraVarcharFunction(encryptfunction, decryptfunction);
        this.dateEncryptFunction = new OraDateFunction(encryptfunction, decryptfunction);
    }

    /**
     * VARCHAR encryption/decryption function.
     */
    private static class OraVarcharFunction implements DbEncryptFunction {

        private final String encryptfunction;
        private final String decryptfunction;
        
        public OraVarcharFunction(String encryptfunction, String decryptfunction) {
            this.encryptfunction = encryptfunction;
            this.decryptfunction = decryptfunction;
        }

        public String getDecryptSql(String columnWithTableAlias) {
            return decryptfunction+"(" + columnWithTableAlias + ",?)";
        }

        public String getEncryptBindSql() {
            return encryptfunction+"(?,?)";
        }
        
    }

    /**
     * DATE encryption/decryption function.
     */
    private static class OraDateFunction implements DbEncryptFunction {
        
        private final String encryptfunction;
        private final String decryptfunction;
        
        public OraDateFunction(String encryptfunction, String decryptfunction) {
            this.encryptfunction = encryptfunction;
            this.decryptfunction = decryptfunction;
        }
        
        public String getDecryptSql(String columnWithTableAlias) {
            return "to_date("+decryptfunction+"(" + columnWithTableAlias + ",?),'YYYYMMDD')";
        }

        public String getEncryptBindSql() {
            return encryptfunction+"(to_char(?,'YYYYMMDD'),?)";
        }
        
    }
}
