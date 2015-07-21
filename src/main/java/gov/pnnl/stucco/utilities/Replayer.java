package gov.pnnl.stucco.utilities;

import gov.pnnl.stucco.collectors.Collector;
import gov.pnnl.stucco.collectors.CollectorFactory;
import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Replays the various feeds from previously saved exogenous and endogenous data
 * as specified in the configuration entries.
 */
public class Replayer {
    
    /** The Collectors being managed by this Replayer. */
    private List<Collector> collectors = new ArrayList<Collector>();
    
    private String outputDir = "";
    
    
    /** 
     * Creates the Replayer.
     * 
     * @param configData  
     * The master configuration map
     * 
     * @param section
     * The section within the map
     */
    @SuppressWarnings("unchecked")
    Replayer( Map<String, Object> configData, String section)
    {
        Map<String,Object> replayerConfig = (Map<String, Object>) configData.get(section);
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
            parser.add1("-section");
            parser.parse(args);
            
            if (parser.found("-file")) {
                String configFilename = parser.getValue();
                Config.setConfigFile(new File(configFilename));
            }
            else {
                throw new UsageException("-file switch is required");
            }
            
            String section = "replayer-file";
            if (parser.found("-section")) {
                section = parser.getValue();
            }
            
            // Get the configuration data
            Map<String, Object> config = Config.getMap();

            // get the content and play it
            Replayer replay = new Replayer(config, section);
            replay.play();
        } 
        catch (UsageException e) {
            System.err.println("Usage: Replayer -file configFile [-section configSection]");
            System.exit(1);
        }
    }

}
