import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

import datamodel.WorkerDetail;
import freemarker.template.Configuration;
import freemarker.template.Template;
import spark.Spark;
import static spark.Spark.get;
import static spark.Spark.halt;
import utils.ApplicationProperties;
import utils.Constants;
import utils.HazelcastInstanceUtils;

public class Monitor {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(Monitor.class);

	@SuppressWarnings("deprecation")
	private static Configuration freemarkerConfig = new Configuration();

	public static void main(String[] args) throws Exception {
		
		logger.info("Starting SPARK REST Framework");

		// Load properties from file
		ApplicationProperties.loadApplicationProperties ();
		
		freemarkerConfig.setClassForTemplateLoading(Monitor.class, ApplicationProperties.getStringProperty("spark.templatePath"));

		Spark.staticFileLocation(ApplicationProperties.getStringProperty("spark.publicPath"));
		HazelcastInstance hzClient = HazelcastClient.newHazelcastClient();
		
		get("/", (req, res) -> Constants.SPARK_WELCOME_MESSAGE);
        get("/stop", (req, res) -> halt(401, Constants.SPARK_BYE_MESSAGE));
        get("/monitor", (req, res) -> {
        	StringWriter writer = new StringWriter();

        	try {
        		
    			boolean refreshPage = false;

    			Iterator<Entry<String, Object>> iter = HazelcastInstanceUtils.getMap(HazelcastInstanceUtils.getWorkersMapName()).entrySet().iterator();

    			while (iter.hasNext()) {
    	            Entry<String, Object> entry = iter.next();
    	            if (((WorkerDetail) entry.getValue()).getActiveStatus()) refreshPage = true;
    	        }
    			
        		Map<String, Object> root = new HashMap<String, Object>();
				root.put( "refreshPage", refreshPage );
        		root.put( HazelcastInstanceUtils.getWorkersMapName (), hzClient.getMap(HazelcastInstanceUtils.getWorkersMapName()) );
				root.put( HazelcastInstanceUtils.getStatusMapName (), hzClient.getMap(HazelcastInstanceUtils.getStatusMapName()) );				
				Template resultTemplate = freemarkerConfig.getTemplate(ApplicationProperties.getStringProperty("spark.templateFileName"));
				resultTemplate.process(root, writer);
    		} catch (Exception ex) {
        		 logger.error ("Exception: " + ex.getClass() + " - " + ex.getMessage());
        	}
			return writer;
        });
    }
}