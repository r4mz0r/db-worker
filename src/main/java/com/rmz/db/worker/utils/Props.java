package com.rmz.db.worker.utils;

import org.apache.log4j.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(Props.class);

    private Props() {
        try {
            properties = new Properties();
            FileInputStream reader = new FileInputStream(FILE_APP_PROP_PATH);
            InputStreamReader inputStreamReader = new InputStreamReader(reader, ENCODING);
            properties.load(inputStreamReader);
        } catch (Exception ex) {
            LOGGER.error("Ошибка при попытке получения параметров из файла: " + FILE_APP_PROP_PATH + "!", ex);
        }
    }

    public static synchronized Props getInstance() {
        if (instance == null) {
            instance = new Props();
        }

        return instance;
    }

    private String getProp(String name) {
        String val = getProps().getProperty(name, "");
        if (val.isEmpty()) {
            LOGGER.debug("Property {" + name + "} was not found in properties file");
        }

        return val.trim();
    }

    public static String get(String prop) {
        return getInstance().getProp(prop);
    }

    public static String get(String prop, String defaultValue) {
        String value = getInstance().getProp(prop);
        return value.isEmpty() ? defaultValue : value;
    }

    public static Properties getProps() {
        return properties;
    }
}
