package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private static String databaseUrl = "jdbc:mysql://<host>:<port>/<name>";
    private static String databaseHost;
    private static String databasePort;
    private static String databaseName;
    private static String databaseUser;
    private static String databasePass;

	//Logger
	private static Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            logger.info ("Connecting to database..." + databaseUrl);
            this.connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePass);
        } catch (Exception ex) {
        	logger.error ("Exception: unable to connect to database [" + databaseUrl + "]");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public static DatabaseConnection getInstance(final Properties applicationProperties) throws SQLException {
    	
        if ((instance == null) || (instance.getConnection().isClosed())) {
        	
    		databaseHost = applicationProperties.getProperty(Constants.DB_HOST);
    		databasePort = applicationProperties.getProperty(Constants.DB_PORT);
    		databaseName = applicationProperties.getProperty(Constants.DB_NAME);
    		databaseUser = applicationProperties.getProperty(Constants.DB_USERNAME);
    		databasePass = applicationProperties.getProperty(Constants.DB_PASSWORD);
    		
    		databaseUrl = databaseUrl.replaceAll("<host>", databaseHost).replaceAll("<port>", databasePort).replaceAll("<name>", databaseName);

            instance = new DatabaseConnection();
        }
        return instance;
    }
    
	public static void closeConnection () {
	    if (instance != null && instance.connection != null) {
	        try {
	        	instance.getConnection().close();
	        } catch (SQLException e) { /* ignored */}
	    }
	}
}