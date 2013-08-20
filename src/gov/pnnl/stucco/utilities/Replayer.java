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
import java.util.Iterator;
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
    
    private ArrayList<Collector> collectors = new ArrayList<Collector>();
    
    
    // Constructor
    Replayer( Map<String, Object> configData)
    {
        Map<String,Object> replayerConfig = (Map<String, Object>) configData.get("replayer");
        Collection<Object> collectorConfig = (Collection<Object>) replayerConfig.get("collectors");
        Iterator<Object> iter = collectorConfig.iterator();
        for(; iter.hasNext(); ) {
            Map<String, String> cc = (Map<String,String>)(iter.next());
            collectors.add(CollectorFactory.makeCollector(cc.get("type"),cc.get("URI"), configData));
        }
    }
    
    /**
     * replays the content of previous collected dataset
     * @param output - the directory of where content should be written (if specified)
     */
    public void play(String output)
    {
        // TODO: could put these two in separate threads and run at the same time?
        for(int i=0; i<collectors.size(); i++) {
            collectors.get(i).collect();
        }
    }
    
    /**
     * Main program to replay a downloaded or previously saved forensic data. 
     * @param args
     */
    static public void main(String[] args) {
        
        // we're assuming that the first input arg is the location of the configuration file
        // gives us the ability to overide with different configurations
        String configFile = "./config/config.yml";
        if(args.length > 0) {
            configFile = args[0];
        }
       
        Yaml yaml = new Yaml();
        try {
            InputStream input = new FileInputStream(new File(configFile.toString()));
            //Object data =  yaml.load(input);
            Map<String, Object> config = (Map<String, Object>) yaml.load(input);
            
            Map<String, String>replayerMap = (Map<String, String>)(config.get("replayer"));
            String outputDir = replayerMap.get("outputDir");
            
            // get the content and play it
            Replayer replay = new Replayer(config);
            replay.play(outputDir);
        } 
        catch (IOException e)  
        {
            System.out.printf("Configuration file %s not found \n", configFile.toString());
        }

      }

}
