package gov.pnnl.stucco.utilities;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import gov.pnnl.stucco.collectors.*;

/**
 * Replays the various feeds from previously saved exogenous and endogenous data
 * As specified in the configuration entries.
 * @author Shawn Bohn,  August 2013
 *
 */
public class Replayer {
    
    private List<Collector> collectors = new ArrayList<Collector>();
	private String outputDir = "";
    
    
    @SuppressWarnings("unchecked")
	Replayer( Map<String, Object> configData)
    {
        Map<String,Object> replayerConfig = (Map<String, Object>) configData.get("replayer");
        Collection<Object> collectorConfig = (Collection<Object>) replayerConfig.get("collectors");
        for(Object obj : collectorConfig) {
            Map<String, String> cc = (Map<String, String>) obj;
            collectors.add(CollectorFactory.makeCollector(cc.get("type"),cc.get("URI"), configData));
        }
        
        outputDir = (String) replayerConfig.get("outputDir");
   }
    
    /**
     * replays the content of previous collected dataset
     * @param output - the directory of where content should be written (if specified)
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
     * @param args
     */
    @SuppressWarnings("unchecked")
	static public void main(String[] args) {        
        Map<String, Object> config = Config.getMap();

        // get the content and play it
        Replayer replay = new Replayer(config);
        replay.play();
    }

}
