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
	

	public static void getHistoricalRates (final String currentCurrency, final String startDate, final String endDate, Map<String, List<FxRate>> historicalDataMap, final Properties applicationProperties) {
 
		Statement stmt = null;
		String sql = null;
		ResultSet rs = null;
		
		try {
			logger.info ("Retrieving historical rates from database for " + currentCurrency);
			stmt = DatabaseConnection.getInstance(applicationProperties).getConnection().createStatement();
			//sql = "SELECT * FROM historical_" + currentCurrency + " WHERE hist_date >= STR_TO_DATE('" + startDate + "','%Y-%m-%d') AND hist_date <= STR_TO_DATE('" + endDate + "','%Y-%m-%d') ORDER BY hist_date ASC, hist_time ASC";
			sql = "SELECT * FROM historical_data WHERE hist_ccy_pair = '" + currentCurrency + "' AND hist_date >= STR_TO_DATE('" + startDate + "','%Y-%m-%d') AND hist_date <= STR_TO_DATE('" + endDate + "','%Y-%m-%d') ORDER BY hist_date ASC, hist_time ASC";
			logger.info("Executing query: " + sql);
			rs = stmt.executeQuery(sql);

			int positionId = 0;

			while(rs.next()) {
				//Retrieve by column name
				String conversionDate = rs.getString("hist_date");
				String conversionTime = rs.getString("hist_time");
				float open = rs.getFloat("hist_open");
				float high = rs.getFloat("hist_high");
				float low = rs.getFloat("hist_low");
				float close = rs.getFloat("hist_close");

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
			sql = "SELECT ccy_pair, spread FROM ccy_pairs WHERE ccy_pair = '" +  currentCurrency + "' AND spread <> '0.000000'";
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
			
			sql = "SELECT UPPER(SUBSTRING(table_name, 12)) as 'currency' FROM information_schema.TABLES WHERE table_name like 'historical_" + currentCurrency + "' AND data_length > 0";
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
				if (logger.isDebugEnabled())
					logger.debug ("Exception: " + e.getClass() + " - " + e.getMessage());
			}
	
		} catch(Exception e) {
			//Handle errors for Class.forName
			logger.error ("Exception while checking if currency table exists for " + currentCurrency);
			if (logger.isDebugEnabled())
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