package com.avaje.tests.basic.encrypt;

import com.avaje.ebean.config.EncryptKey;
import com.avaje.ebean.config.EncryptKeyManager;

public class BasicEncyptKeyManager implements EncryptKeyManager {

    public EncryptKey getEncryptKey(String tableName, String columnName) {
        return new BasicEncryptKey("simple");
    }

}
