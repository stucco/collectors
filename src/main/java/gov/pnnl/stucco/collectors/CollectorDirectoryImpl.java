package gov.pnnl.stucco.collectors;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

import java.io.File;

public class CollectorDirectoryImpl extends CollectorAbstractBase {
        
    /** Directory to collect files from. */
    private File directory;
    
    
    /** Sets up a sender for a directory. */
    public CollectorDirectoryImpl(File dir) {
      if (!dir.isDirectory()) {
        throw new IllegalArgumentException(dir + "is not a directory");
      }
      
      directory = dir;
    }
    
    /**
     * primary routine to collect the content from the directories
     * When the content has been collected on each file it is sent to the queue
     */
    @Override
    public void collect() {
        File[] files = directory.listFiles();
        for (File f : files) {
            // Read the file
            // TODO: should only instantiate this once and reuse
            CollectorFileImpl cf = new CollectorFileImpl(f);
            cf.collect();
        }
    }
    
    /**
     * what are the files within this directory
     * @return
     */
    public String[] listFiles() {
        File[] files = directory.listFiles();
        String[] fileArray = new String[files.length];
        for (int i=0; i<files.length; i++) {
            fileArray[i] = files[i].getName();
        }     
        return fileArray;
    }
    
    
    /**
     * Main
     * @param args
     */
    static public void main(String[] args) {
        try {
            File dir = new File("Send");
            CollectorDirectoryImpl collectDir = new CollectorDirectoryImpl(dir);
            collectDir.collect();
            
            //print out files in directory
            String[] fileArray = collectDir.listFiles();
            for (int i=0; i<fileArray.length; i++) {
                System.out.println(fileArray[i]);
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("Error in finding directory");
        }

    }
    
    

}
