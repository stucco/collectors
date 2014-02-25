package gov.pnnl.stucco.collectors;


import gov.pnnl.stucco.jetcd.StuccoClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import jetcd.EtcdException;
import jetcd.StuccoClientImpl;

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
    static private File configFile = null;//new File("./config/data-sources.yml");
    
    static private String configUrl = null;
    
    static private Config instance;
    
    private Map<String, Object> config;
    
    
    /** Sets the configuration file. */
    public static void setConfigFile(File f) {
        configFile = f;
    }
    
    public static void setConfigUrl(String url) {
        configUrl = url;
    }
    
    @SuppressWarnings("unchecked")
    private Config() {
        try {
            if (configFile != null) {
                // Use config file
                Yaml yaml = new Yaml();
                InputStream input = new FileInputStream(configFile);
                config = (Map<String, Object>) yaml.load(input);
            }
            else if (configUrl != null) {
                // Use config service
                StuccoClient client = new StuccoClientImpl(configUrl);
                config = client.listNested("/stucco");
            }
        } 
        catch (IOException e) {
            System.err.printf("Configuration file %s not found%n", configFile);
        } 
        catch (EtcdException e) {
            System.err.printf("Unable to read from config URL %s%n", configUrl);
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

  















