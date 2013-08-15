package gov.pnnl.stucco.collectors;

import java.io.File;

import gov.pnnl.stucco.collectors.DirectorySender;

public class Replayer {
    
    private DirectorySender exogenous;
    private DirectorySender endogenous;
    
    private String exoDir, endDir;
    
    // Constructor
    Replayer(String configFile)
    {
        loadConfiguration(configFile);
        
        exogenous  = new DirectorySender(new File(exoDir));
        endogenous = new DirectorySender(new File(endDir));
    }
    
    /**
     * load the configuration file to know where our content will be send
     */
    private void loadConfiguration(String configFile)
    {
        exoDir = "";
        endDir = "";
        
    }
    /**
     * replays the content of previous collected dataset
     * @param output - the directory of where content should be written (if specified)
     */
    public void play(String output)
    {
        // TODO: could put these two in separate threads and run at the same time?
        exogenous.send();
        endogenous.send();
    }
    
    /**
     * Main program to replay a downloaded or previously saved forensic data. 
     * @param args
     */
    static public void main(String[] args) {
        // the first argument defines the configuration file to use
        String configFile = "";
        
        // the second argument (if specify) tells us if we want he results to be written to a directory
        String outputDir = "";  // note this is to override what may already be specified in the configuration file
        
        // get the content and play it
        Replayer replay = new Replayer(configFile);
        replay.play(outputDir);

      }

}
