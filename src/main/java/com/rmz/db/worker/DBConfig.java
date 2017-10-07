package com.rmz.db.worker;

/**
 * Database configuration class
 *
 * @author mekhdiev-rr
 */
public class DBConfig {

    private String name;
    private String tnsName;
    private String username;
    private String password;

    public DBConfig(String name, String tnsName, String username, String password) {
        this.name = name;
        this.tnsName = tnsName;
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getTnsName() {
        return tnsName;
    }

    public String getUserName() {
        return username;
    }
}
