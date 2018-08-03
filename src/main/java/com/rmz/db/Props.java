package com.rmz.db;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Property class
 *
 * @author mekhdiev-rr
 */
public class Props {
    private static Properties properties;
    private static Props instance;
    private static final String FILE_APP_PROP_PATH = "src/main/resources/config/application.properties";
    private static final String ENCODING = "UTF-8";

    private Props() {
        try {
            properties = new Properties();
            FileInputStream reader = new FileInputStream(FILE_APP_PROP_PATH);
            InputStreamReader inputStreamReader = new InputStreamReader(reader, ENCODING);
            properties.load(inputStreamReader);
        } catch (Exception ex) {
            throw new Error("Ошибка при попытке получения параметров из файла: " + FILE_APP_PROP_PATH + "!", ex);
        }
    }

    protected static synchronized Props getInstance() {
        if (instance == null) {
            instance = new Props();
        }

        return instance;
    }

    private String getProp(String name) {
        String val = getProps().getProperty(name, "");
        if (val.isEmpty()) {
            System.out.println("\r\nProperty {" + name + "} was not found in properties file");
        }

        return val.trim();
    }

    protected static String get(String prop) {
        return getInstance().getProp(prop);
    }

    protected static String get(String prop, String defaultValue) {
        String value = getInstance().getProp(prop);
        return value.isEmpty() ? defaultValue : value;
    }

    protected static Properties getProps() {
        return properties;
    }
}
