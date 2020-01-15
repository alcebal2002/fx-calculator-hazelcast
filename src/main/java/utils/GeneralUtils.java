package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;

import datamodel.FxRate;

public class GeneralUtils {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(GeneralUtils.class);
	
    public static String getHostName () {
    	String result = "unknown";
        try {
            result = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {}
        return result;
    }
	
	public static final Date getDateFromString(final String date, final String format) {
		Date result = null;
		try {
			// format example: "dd-mm-yyyy"
			result = new SimpleDateFormat(format).parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void waitForKeyToContinue () {
		logger.info ("");
		logger.info ("*************************************************************************");
		logger.info ("Ensure all the Workers are up & running. Then Press Any Key to continue...");
		logger.info ("*************************************************************************");

		try {
			System.in.read();
		} catch (Exception e) {
			logger.error ("Exception: " + e.getClass() + " - " + e.getMessage());
		}
	}
	
	public static boolean checkIfFileExists (final String currentCurrency, final Properties appliationProperties) {

		boolean exists = false;

		String historicalDataPath = appliationProperties.getProperty(Constants.WK_HISTORICALDATAPATH);
		String historicalDataFileExtension = appliationProperties.getProperty(Constants.WK_HISTORICALDATAFILEEXTENSION);

		String file = historicalDataPath + currentCurrency + historicalDataFileExtension;

		File f = new File(file);
		if(f.exists() && !f.isDirectory()) {
			exists = true;
		}
		return exists;
	}

    public static void checkResultsPath() throws Exception {
    	
    	if (ApplicationProperties.getBooleanProperty(Constants.AP_WRITERESULTSTOFILE)) {
        	logger.info("Checking if results directory exists (" + ApplicationProperties.getStringProperty(Constants.AP_RESULTSPATH) + ")");

        	if (!GeneralUtils.checkIfDirectoryExists(ApplicationProperties.getStringProperty(Constants.AP_RESULTSPATH))) {
    			throw (new Exception("Results directory (" + ApplicationProperties.getStringProperty(Constants.AP_RESULTSPATH) + " does not exist. Please check !"));
    		} else {
    			logger.info ("Results Path exists");
    		}
    	}
    }

	public static boolean checkIfDirectoryExists (final String directory) {

		boolean exists = false;

		File f = new File(directory);
		if(f.exists() && f.isDirectory()) {
			exists = true;
		}
		return exists;
	}
	
	// Populates historical data and puts the objects into historical data list)
    // Depending on the datasource parameter, data could be retrieved from database (mysql) or files
    // FX Historical Data format: conversionDate,conversionTime,open,high,low,close
    public static long populateHistoricalFxData (final String currentCurrency, final String startDate, final String endDate, Map<String, List<FxRate>> historicalDataMap, final Properties applicationProperties) {
    	
    	long result = 0;
    	
    	logger.info("Data source set to: " + applicationProperties.getProperty(Constants.AP_DATASOURCE));

    	if ("database".equals(applicationProperties.getProperty(Constants.AP_DATASOURCE))) {
    		
    		// Gets properties from task item properties
    		
    		// Populate historical data from mysql database
    		DatabaseUtils.getHistoricalRates(currentCurrency, startDate, endDate, historicalDataMap, applicationProperties);
    		
    		if (historicalDataMap != null && historicalDataMap.size() > 0) {
    			// There should be only 1 record in the map corresponding to the currentCurrency
   	            logger.info (currentCurrency + " -> total FX records loaded " + historicalDataMap.get(currentCurrency).size());
    		}
    	} else {

   	    	int totalCounter = 0;
   	    	int lineNumber = 0;

   	    	// Gets properties from application.properties file   	    	
			String historicalDataPath = ApplicationProperties.getStringProperty(Constants.WK_HISTORICALDATAPATH);
			String historicalDataFileExtension = ApplicationProperties.getStringProperty(Constants.WK_HISTORICALDATAFILEEXTENSION);
			String historicalDataSeparator = ApplicationProperties.getStringProperty(Constants.WK_HISTORICALDATASEPARATOR);
			int printAfter = ApplicationProperties.getIntProperty(Constants.WK_PRINTAFTER);

			String fileName = historicalDataPath + currentCurrency + historicalDataFileExtension;
    		
        	logger.info("Populating historical data from file (" + fileName + "). Fields separated by " + historicalDataSeparator.charAt(0));
        	
        	try {
        		CSVReader reader = new CSVReader(new FileReader(fileName), historicalDataSeparator.charAt(0));
    	        String [] nextLine;
    	        while ((nextLine = reader.readNext()) != null) {
    	        	
    	        	FxRate fxRate = new FxRate (currentCurrency,nextLine,totalCounter,startDate,endDate);
    	        	
    	        	// Check if the fxRate has been created or excluded due to the date filtering
    	        	if (currentCurrency.equals(fxRate.getCurrencyPair())) {
    					if (!historicalDataMap.containsKey(currentCurrency)) {
    						historicalDataMap.put(currentCurrency, new ArrayList<FxRate>());							
    					}
    					(historicalDataMap.get(currentCurrency)).add(fxRate);

    					if (totalCounter%printAfter == 0) {
    						if (logger.isDebugEnabled())
    							logger.debug ("  " + currentCurrency + " -> loaded " + totalCounter + " records so far");
        				}
    					totalCounter++;
    	        	}
    	        	lineNumber++;
    	        }
    	        logger.info (currentCurrency + " -> total records loaded " + totalCounter);
    	        reader.close();
    	    	
        	} catch (Exception ex) {
        		logger.error ("Exception in file " + fileName + " - line " + lineNumber + " - " + ex.getClass() + " - " + ex.getMessage());
        	}
    	}
    	
    	if (historicalDataMap.containsKey(currentCurrency)) {
    		result = historicalDataMap.get(currentCurrency).size();
    	}
    	
    	return result;
    }

	public static boolean checkIfCurrencyExists (final String currentCurrency, final Properties applicationProperties) {
/*
		boolean exists = false;

		if ("database".equals(applicationProperties.getProperty(Constants.AP_DATASOURCE))) {
			exists = DatabaseUtils.checkCurrencyTableExists(currentCurrency,applicationProperties);
		} else {
			exists = GeneralUtils.checkIfFileExists(currentCurrency, applicationProperties);
		}
*/
		return true;
	}
    
    public static float getSpread (final String currentCurrency, final Properties applicationProperties) {
    	float result = 0;

		if (logger.isDebugEnabled())
			logger.debug("Data source set to: " + applicationProperties.getProperty(Constants.AP_DATASOURCE));
    	if ("database".equals(applicationProperties.getProperty(Constants.AP_DATASOURCE))) {
			if (logger.isDebugEnabled())
				logger.debug("Retrieving spread value for " + currentCurrency + " from database");
    		// Populate spread data from mysql database
    		result = DatabaseUtils.getSpread(currentCurrency, applicationProperties);
    	} else {

			if (logger.isDebugEnabled())
				logger.debug("Retrieving spread value for " + currentCurrency + " from file");
   	    	int counter = 0;

			String historicalDataPath = applicationProperties.getProperty(Constants.WK_HISTORICALDATAPATH);
			String historicalDataFileExtension = applicationProperties.getProperty(Constants.WK_HISTORICALDATAFILEEXTENSION);
			String historicalDataSeparator = applicationProperties.getProperty(Constants.WK_HISTORICALDATASEPARATOR);

			String fileName = historicalDataPath + "pares" + historicalDataFileExtension;
    		        	
        	try {
        		CSVReader reader = new CSVReader(new FileReader(fileName), historicalDataSeparator.charAt(0));
    	        String [] nextLine;
    	        while ((nextLine = reader.readNext()) != null) {
    	        	counter++;
    	        	
    	        	if (currentCurrency.equals(nextLine[1])) {
    	        		result = Float.parseFloat(nextLine[2]);
    	        		break;
    	        	}
    	        	
    	       	}
    	        reader.close();
    	    	
        	} catch (Exception ex) {
        		logger.error ("Exception in file " + fileName + " - line " + counter + " - " + ex.getClass() + " - " + ex.getMessage());
        	}
    	}
    	
   		logger.info (currentCurrency + " -> Spread: " + result);

    	return result;
    }
   
    public static void increaseMapCounter (Map<String, Integer> resultMap, final String keyString) {
		if (resultMap.containsKey(keyString)) {
			resultMap.put(keyString,resultMap.get(keyString)+1);
		} else {
			resultMap.put(keyString,1);
		}
    }
    
	public static List<String> getFilesFromPath (final String path, final String extension) {
		List<String> filesList = new ArrayList<String>();
		File dir = new File(path);
		for (File file : dir.listFiles()) {
			if (file.getName().toLowerCase().endsWith((extension))) {
				filesList.add(file.getName());
			}
		}
		return filesList;
	}
	
	public static String printElapsedTime (final long startTime, final long endTime) {
		
		long millis = endTime - startTime;
		return (printElapsedTime(millis));
	}
	
	public static String printElapsedTime (final long totalMillis) {
		
		long millis = totalMillis;
		
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days); 
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes); 
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

		return (millis + " ms - (" + hours + " hrs " + minutes + " min " + seconds + " secs)");
	}
	
	// Wirte text to file 
	public static void writeTextToFile (final Path path, final String text) {

		BufferedWriter bWriter = null;

		try {
		    Files.write(path, Arrays.asList(text), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		} catch (Exception e) {
			logger.error ("Exception while writing the results to file: " + e.getClass() + " - " + e.getMessage());
		} finally {
			try {
				if (bWriter != null)
					bWriter.close();
			} catch (IOException ex) {
				logger.error ("Exception while closing the results file: " + ex.getClass() + " - " + ex.getMessage());
			}
		}
	}

	// Print levels header
	public static String printResultsHeader(final int maxLevels) {
		StringBuilder stringBuilder =  new StringBuilder();
		stringBuilder.append("CURRENCYPAIR|METHODOLOGY|STARTDATE|ENDDATE");
		
		for (int i=1; i <= maxLevels; i++) {
			stringBuilder.append("|"+i+"-UP|"+i+"-DOWN|"+i+"-TOTAL|"+i+"-%");
		}
		
		return (stringBuilder.toString());
	}

	// Print currency result levels
	public static String printResultsLevels (final String currency, final String calculationMethodology, final String startDate, final String endDate, final Map<String,Integer> levelsMap, final int maxLevels) {
		
		StringBuilder stringBuilder = new StringBuilder();
		
		double referenceLevel = 0;
		
		for (int i=1; i <= maxLevels; i++) {
			long total=0;
			if (levelsMap != null && levelsMap.containsKey("UP-"+i)) {
				stringBuilder.append(levelsMap.get("UP-"+i));
				total += levelsMap.get("UP-"+i);
			} else {
				stringBuilder.append("0");
			}
			stringBuilder.append("|");
			if (levelsMap != null && levelsMap.containsKey("DOWN-"+i)) {
				stringBuilder.append(levelsMap.get("DOWN-"+i));
				total += levelsMap.get("DOWN-"+i);
			} else {
				stringBuilder.append("0");
			}
			stringBuilder.append("|");
			stringBuilder.append(total);
			stringBuilder.append("|");
			if (i==1) referenceLevel = total;

			if (total == 0) {
				stringBuilder.append("0");
			} else {
				stringBuilder.append(new DecimalFormat("#.##").format(total*100/referenceLevel));
			}
			stringBuilder.append("|");
		}
		
		return (currency + "|" + calculationMethodology + "|" + startDate + "|" + endDate + "|" + stringBuilder.toString());
	}
}
