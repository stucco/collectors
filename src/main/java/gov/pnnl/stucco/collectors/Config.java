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
    
    /**
     * File holding configuration data. This should get set but is given a
     * default.
     */
    static private File configFile = new File("./config/data-sources.yml");
    
    static private Config instance;
    
    private Map<String, Object> config;
    
    
    /** Sets the configuration file. */
    public static void setConfigFile(File f) {
        configFile = f;
    }
    
    
    @SuppressWarnings("unchecked")
    private Config() {
        Yaml yaml = new Yaml();
        try {
            InputStream input = new FileInputStream(configFile);
            config = (Map<String, Object>) yaml.load(input);
        } 
        catch (IOException e)  
        {
            System.err.printf("Configuration file %s not found \n", configFile);
        }
    }
    
    /** Gets the configuration map, using the default file. */
    static public Map<String, Object> getMap() {
        if (instance == null) {
            instance = new Config();
        }
        return instance.config;
    }
    
}

  















