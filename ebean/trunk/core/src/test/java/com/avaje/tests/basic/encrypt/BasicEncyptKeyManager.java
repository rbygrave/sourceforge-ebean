package com.avaje.tests.basic.encrypt;

import com.avaje.ebean.config.EncryptKeyManager;

public class BasicEncyptKeyManager implements EncryptKeyManager {

    public String getEncryptKey(String tableName, String columnName) {
        return "simple";
    }

}
