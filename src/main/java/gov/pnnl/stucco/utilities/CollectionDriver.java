package gov.pnnl.stucco.utilities;

import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.util.Map;


/**
 * A single Java app for running either the CollectorScheduler or the Replayer.
 * Alternatively we could use a shell script for this, but we want to be able to
 * gracefully handle shutdown requests from supervisord.
 * 
 * @author Grant Nakamura, Feb 2015
 */
public class CollectionDriver {

    public static void main(String[] args) {
        try {
            CommandLine parser = new CommandLine();
            parser.add0("-load");
            parser.add0("-schedule");
            parser.add1("-config");
            parser.add1("-section");
            parser.parse(args);
            
            boolean load = parser.found("-load");
            boolean schedule = parser.found("-schedule");
            if (load == schedule) {
                throw new UsageException("Must have exactly one of -load and -schedule");
            }
            
            if (parser.found("-config")) {
                String configFilename = parser.getValue();
                Config.setConfigFile(new File(configFilename));
            }
            else {
                throw new UsageException("-config <configFile> is required");
            }
            
            String section;
            if (parser.found("-section")) {
                section = parser.getValue();
            }
            else {
                throw new UsageException("-section <configSection> is required");
            }
            
            // Set up to handle externally-triggered shutdown gracefully
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    CollectorMetadata metadata = CollectorMetadata.getInstance();
                    
                    // CollectorMetadata.read() and write() are synchronized methods.
                    // If either is running, we'll block here until the method is finished.
                    synchronized (metadata) {
                    }
                }
            });
            
            // Get the configuration data
            Map<String, Object> config = Config.getMap();

            if (load) {
                // get the content and play it
                Replayer replay = new Replayer(config, section);
                replay.play();
            }
            
            if (schedule) {
                // Run the scheduler for a really long time
                CollectorScheduler scheduler = new CollectorScheduler();
                Map<String, Object> sectionMap = (Map<String, Object>) config.get(section);
                scheduler.runSchedule(sectionMap);
            }
        } 
        catch (UsageException e) {
            System.err.println("Usage: (-load|-schedule) -config <configFile> -section <configSection>");
            System.exit(1);
        }
    }
}
