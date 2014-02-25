package gov.pnnl.stucco.utilities;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

import gov.pnnl.stucco.collectors.Collector;
import gov.pnnl.stucco.collectors.CollectorFactory;
import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Replays the various feeds from previously saved exogenous and endogenous data
 * as specified in the configuration entries.
 * 
 * @author Shawn Bohn,  August 2013
 */
public class Replayer {
    
    /** The Collectors being managed by this Replayer. */
    private List<Collector> collectors = new ArrayList<Collector>();
    
	private String outputDir = "";
    
    
    @SuppressWarnings("unchecked")
	Replayer( Map<String, Object> configData)
    {
        Map<String,Object> replayerConfig = (Map<String, Object>) configData.get("replayer");
        Collection<Object> collectorConfig = (Collection<Object>) replayerConfig.get("collectors");
        for(Object obj : collectorConfig) {
            Map<String, String> cc = (Map<String, String>) obj;
            collectors.add(CollectorFactory.makeCollector(cc));
        }
        
        outputDir = (String) replayerConfig.get("outputDir");
   }
    
    /**
     * replays the content of previous collected dataset
     */
    public void play()
    {
        // TODO: Use the output dir?
        
        // TODO: could put collectors in separate threads and run at the same time?
        for(Collector c : collectors) {
            c.collect();
        }
    }
    
    /**
     * Main program to replay a downloaded or previously saved forensic data. 
     */
	static public void main(String[] args) {
	    try {
            CommandLine parser = new CommandLine();
            parser.add1("-file");
            parser.add1("-url");
            parser.parse(args);
            
            if (parser.found("-file")  &&  parser.found("-url")) {
                throw new UsageException("Can't specify both file and URL");
            }
            else if (parser.found("-file")) {
                String configFilename = parser.getValue();
                Config.setConfigFile(new File(configFilename));
            }
            else if (parser.found("-url")) {
                String configUrl = parser.getValue();
                Config.setConfigUrl(configUrl);
            }
            
            // Get the configuration data
            Map<String, Object> config = Config.getMap();

            // get the content and play it
            Replayer replay = new Replayer(config);
            replay.play();
        } 
        catch (UsageException e) {
            System.err.println("Usage: Replayer (-file configFile | -url configUrl)");
            System.exit(1);
        }
    }

}
