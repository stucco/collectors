package gov.pnnl.stucco.collectors;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Class for YAML configuration file(s).
 * 
 * @author Shawn Bohn, Grant Nakamura; August 2013 
 */
public class Config {
    
    static private Config instance = new Config();
    
    private Map<String, Object> config;
    
    
    @SuppressWarnings("unchecked")
    private Config() {
        String configFile = "./config/data-sources.yml";

        Yaml yaml = new Yaml();
        try {
            InputStream input = new FileInputStream(new File(configFile));
            config = (Map<String, Object>) yaml.load(input);
        } 
        catch (IOException e)  
        {
            System.err.printf("Configuration file %s not found \n", configFile);
        }
    }
    
    /** Gets the configuration map, using the default file. */
    static public Map<String, Object> getMap() {
        return instance.config;
    }
    
}

  















