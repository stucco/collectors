package gov.pnnl.stucco.collectors;


import gov.pnnl.stucco.jetcd.StuccoClient;
import gov.pnnl.stucco.jetcd.StuccoJetcdUtil;
import gov.pnnl.stucco.utilities.CommandLine;
import gov.pnnl.stucco.utilities.Replayer;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import jetcd.EtcdException;
import jetcd.StuccoClientImpl;

import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    
    private Map<String, Object> config = null;
    
    
    /** Sets the configuration file. */
    public static void setConfigFile(File f) {
        configFile = f;
    }
    
    public static void setConfigUrl(String url) {
        configUrl = url;
    }
    
    @SuppressWarnings("unchecked")
    private Config() {
    }
    
    /** 
     * Loads the configuration, either from file, URL, or environment variables,
     * in that order of preference.
     */
    private void load() {
        try {
            if (configFile != null) {
                // Use config file
                Yaml yaml = new Yaml();
                InputStream input = new FileInputStream(configFile);
                Map<String, Object> yamlConfig = (Map<String, Object>) yaml.load(input);
                convertYamlObjectsToString(yamlConfig);
                
                config = (Map<String, Object>) yamlConfig.get("default");
            }
            else {
                
                if (configUrl == null) {
                    String host = System.getenv("ETCD_HOST");
                    String port = System.getenv("ETCD_PORT");
                    if (host == null || port == null) {
                        throw new EtcdException(1, "", "Missing URL and environment variables", 0);
                    }
                    configUrl = String.format("http://%s:%s", host, port);
                }
                
                // Use config service
                StuccoClient client = new StuccoClientImpl(configUrl);
                config = client.listNested("/");
            }
            config = StuccoJetcdUtil.trimKeyPaths(config);
        } 
        catch (IOException e) {
            System.err.printf("Configuration file %s not found%n", configFile);
        } 
        catch (EtcdException e) {
            System.err.printf("Unable to read from config URL %s%n", configUrl);
        }
    }
    
    /** 
     * Modifies a configuration map acquired from a Yaml.load(), to replace
     * non-String values with their toString() equivalents.
     */
    private void convertYamlObjectsToString(Map<String, Object> configMap) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                // Submap, so recursively convert it
                Map<String, Object> submap = (Map<String, Object>) value;
                convertYamlObjectsToString(submap);
            }
            else if (!(value instanceof String) && !(value instanceof Collection)) {
                // Convert non-String value to String
                entry.setValue(value.toString());
            }
        }
    }
    
    /** Gets the configuration map, using the default file. */
    static public Map<String, Object> getMap() {
        if (instance == null) {
            // Create the empty singleton instance
            instance = new Config();
        }
        
        if (instance.config == null) {
            // The config hasn't been loaded yet, so do it now
            instance.load();
        }
        
        return instance.config;
    }
    
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
            
            System.err.println("Top-level keys = " + config.keySet());
            
        } 
        catch (UsageException e) {
            System.err.println("Usage: Replayer (-file configFile | -url configUrl)");
            System.exit(1);
        }
        
    }
    
}

  















