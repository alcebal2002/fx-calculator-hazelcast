package utils;

import static java.lang.System.out;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationProperties {
	
	// Logger
	private static Logger logger = LoggerFactory.getLogger(ApplicationProperties.class);

    private static Properties applicationProperties;

    public static Properties getApplicationProperties () {
    	return applicationProperties;
    }
    
    public static String getApplicationPropertiesFile () {
    	return Constants.APPLICATION_PROPERTIES;
    }

    public static List<String> getListProperty (final String propertyName) {
        String values = getStringProperty (propertyName);
    	
    	return Arrays.asList(values.split("\\s*,\\s*"));
    }

    public static String getStringProperty (final String propertyName) {
        return getProperty(propertyName);
    }

    public static int getIntProperty (final String propertyName) {
        return Integer.parseInt(getProperty(propertyName));
    }

    public static float getFloatProperty (final String propertyName) {
        return Float.parseFloat(getProperty(propertyName));
    }
    
    public static long getLongProperty (final String propertyName) {
        return Long.parseLong(getProperty(propertyName));
    }

    public static boolean getBooleanProperty (final String propertyName) {
        return Boolean.parseBoolean(getProperty(propertyName));
    }

    public static void loadApplicationProperties () {
    	logger.info("Loading application properties from " + Constants.APPLICATION_PROPERTIES);
    	
        if (applicationProperties == null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            try{
            	InputStream resourceStream = loader.getResourceAsStream(getApplicationPropertiesFile ());
                applicationProperties = new Properties();
                applicationProperties.load(resourceStream);                
            } catch (Exception ex) {
                out.println ("Exception: " + ex.getClass() + " - " + ex.getMessage());
            }
        } else {
        	logger.info ("Properties already loaded");
        }
    }
    
    public static String getProperty (final String propertyName) {

       return applicationProperties.getProperty(propertyName);
    }
    
    public static String printProperties () {
    	StringBuilder stringBuilder = new StringBuilder();
    	Enumeration<?> propsList = applicationProperties.propertyNames();

        String propName;

    	for (; propsList.hasMoreElements(); ) {
            propName = (String) propsList.nextElement();
    		stringBuilder.append (propName + "|" + (String) applicationProperties.get(propName) + "\n");
        }
    	return stringBuilder.toString();
    }
}