package utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datamodel.FxRate;

public class DatabaseUtils {
	
	//Logger
	private static Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);
	

	public static void getHistoricalRates (final String currentCurrency, Map<String, List<FxRate>> historicalDataMap, final Properties applicationProperties) {
 
		Statement stmt = null;
		String sql = null;
		ResultSet rs = null;
		
		try {
			logger.info ("Retrieving historical rates from database for " + currentCurrency);
			stmt = DatabaseConnection.getInstance(applicationProperties).getConnection().createStatement();
			sql = "SELECT * FROM historico_" + currentCurrency + " WHERE fecha >= STR_TO_DATE('" + applicationProperties.getProperty("application.startDate") + "','%Y-%m-%d') AND fecha <= STR_TO_DATE('" + applicationProperties.getProperty("application.endDate") + "','%Y-%m-%d') ORDER BY fecha ASC, hora ASC";
			logger.info("Executing query: " + sql);
			rs = stmt.executeQuery(sql);

			int positionId = 0;

			while(rs.next()) {
				//Retrieve by column name
				String conversionDate = rs.getString("fecha");
				String conversionTime = rs.getString("hora");
				float open = rs.getFloat("apertura");
				float high = rs.getFloat("alto");
				float low = rs.getFloat("bajo");
				float close = rs.getFloat("cerrar");

				if (!historicalDataMap.containsKey(currentCurrency)) {
					historicalDataMap.put(currentCurrency, new ArrayList<FxRate>());
				}
				(historicalDataMap.get(currentCurrency)).add(new FxRate(positionId, currentCurrency, conversionDate, conversionTime, open, high, low, close));
				positionId++;
			}
			rs.close();
		} catch(Exception e) {
			//Handle errors for Class.forName
			logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
		} finally {
			//finally block used to close resources
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				logger.error ("SQLException: " + e.getClass() + " - " + e.getMessage());
			}

			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
			}
		}
	}

	public static float getSpread (final String currentCurrency, final Properties applicationProperties) {
		 
		Statement stmt = null;
		String sql = null;
		ResultSet rs = null;

		float result = 0;
		
		try {
			logger.info ("Retrieving spreads from database");
			stmt = DatabaseConnection.getInstance(applicationProperties).getConnection().createStatement();
			sql = "SELECT id_par, divisas, spread FROM pares WHERE divisas = '" +  currentCurrency + "' AND spread <> '0.000000' ORDER BY id_par";
			logger.info("Executing query: " + sql);
			rs = stmt.executeQuery(sql);

			while(rs.next()) {
				//Retrieve by column name
				result = rs.getFloat("spread");
			}
			rs.close();
		} catch(Exception e) {
			//Handle errors for Class.forName
			logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
		} finally {
			//finally block used to close resources
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				logger.error ("SQLException: " + e.getClass() + " - " + e.getMessage());
			}

			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return result;
	}	
	
	public static boolean checkCurrencyTableExists (final String currentCurrency, final Properties applicationProperties) {
		 
		Statement stmt = null;
		String sql = null;
		ResultSet rs = null;

		boolean exists = false;
		
		try {
			logger.info ("Checking if currency table exists for " + currentCurrency);
			
			stmt = DatabaseConnection.getInstance(applicationProperties).getConnection().createStatement();
			
			sql = "SELECT UPPER(SUBSTRING(table_name, 11)) as 'currency' FROM information_schema.TABLES WHERE table_name like 'historico_" + currentCurrency + "' AND data_length > 0";
			logger.info("Executing query: " + sql);
			
			try {
			
				rs = stmt.executeQuery(sql);

				while(rs.next()) {
					//Retrieve currency name
					if ((currentCurrency.toUpperCase()).equals(rs.getString("currency").toUpperCase())) {
						exists = true;
					}
				}
				rs.close();
			} catch(Exception e) {
				//Handle errors for Class.forName
				logger.error ("Exception while executing " + sql);
				logger.debug ("Exception: " + e.getClass() + " - " + e.getMessage());
			}
	
		} catch(Exception e) {
			//Handle errors for Class.forName
			logger.error ("Exception while checking if currency table exists for " + currentCurrency);
			logger.debug ("Exception: " + e.getClass() + " - " + e.getMessage());
		} finally {
			//finally block used to close resources
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				logger.error ("SQLException: " + e.getClass() + " - " + e.getMessage());
			}

			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return exists;
	}
}