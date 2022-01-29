package de.trundicho.timeclockstamper.core.domain.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final Properties PROPERTIES;

    static {
        PROPERTIES = new Properties();
        try {
            String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
            InputStream is = new BufferedInputStream(new FileInputStream(path + APPLICATION_PROPERTIES));
            PROPERTIES.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getString(String property) {
        return PROPERTIES.getProperty(property);
    }

}
