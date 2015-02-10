package gov.pnnl.stucco.collectors;


import gov.pnnl.stucco.jetcd.StuccoClient;
import gov.pnnl.stucco.jetcd.StuccoJetcdUtil;
import gov.pnnl.stucco.utilities.CommandLine;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import jetcd.EtcdException;
import jetcd.StuccoClientImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;


/**
 * Class for YAML configuration file(s).
 * 
 * @author Shawn Bohn, Grant Nakamura; August 2013 
 */
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    
    /** Singleton instance of Config. We may revisit this. */
    static private Config instance;

    /** URL pointing to configuration service. */
    static private String configUrl = null;
    
    /** File holding configuration data, as alternative to using configuration service. */
    static private File configFile = null;
    
    /** Configuration map retrieved from either the config service or a YAML file. */
    private Map<String, Object> config = null;
    
    
    /** Sets the configuration service URL. */
    public static void setConfigUrl(String url) {
        configUrl = url;
    }
    
    /** Sets the configuration file. */
    public static void setConfigFile(File f) {
        configFile = f;
    }
    
    private Config() {
    }
    
    // TODO: add logging that would dump entire configuration entries
    
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
                logger.info("Using configuration from file: {}", configFile);
            }
            else {
                if (configUrl == null) {
                    // No URL specified, use environment vars or defaults for host:port
                    
                    String host = System.getenv("ETCD_HOST");
                    if (host == null) {
                        host = "localhost";
                    }
                    
                    String port = System.getenv("ETCD_PORT");
                    if (port == null) {
                        port = "4001";
                    }
                    
                    configUrl = String.format("http://%s:%s", host, port);
                }
                
                // Use config service
                StuccoClient client = new StuccoClientImpl(configUrl);
                config = client.listNested("/");
                logger.info("Using configuration from file: {}", configUrl);

            }
            config = StuccoJetcdUtil.trimKeyPaths(config);
        } 
        catch (IOException e) {
            logger.error("Configuration file %s not found%n", configFile, e);
        } 
        catch (EtcdException e) {
            logger.error("Unable to read from config URL %s%n", configUrl, e);
        }
    }
    
    /** 
     * Modifies a configuration map acquired from a Yaml.load(), to replace
     * non-String values with their toString() equivalents.
     * 
     * <p> Currently assumes each Object is either a value, 
     * Map<String, Object>, or List<Map<String, Object>>.
     */
    private void convertYamlObjectsToString(Map<String, Object> configMap) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                // Submap, so recursively convert it
                Map<String, Object> submap = (Map<String, Object>) value;
                convertYamlObjectsToString(submap);
            }
            else if (value instanceof List) {
                // List, so recursively convert all its items
                List<Map<String, Object>> configList = (List<Map<String, Object>>) value;
                for (Map<String, Object> submap : configList) {
                    convertYamlObjectsToString(submap);
                }
            }
            else if (!(value instanceof String) && !(value instanceof List)) {
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
        } 
        catch (UsageException e) {
            System.err.println("Usage: Config (-file configFile | -url configUrl)");
            System.exit(1);
        }
    }
    
}

  















