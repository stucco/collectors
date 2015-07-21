package gov.pnnl.stucco.utilities;


import gov.pnnl.stucco.collectors.Collector;
import gov.pnnl.stucco.collectors.CollectorFactory;

import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/** 
 * Quartz Job that constructs and runs a Collector. The configuration for the 
 * collector is passed in the JobExecutionContext.
 */
public class CollectorJob implements Job {

    public CollectorJob() {
    }
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Get parameters as a Map<String, Object>
        JobDataMap jobData = context.getMergedJobDataMap();
        
        // Create the collector
        Map<String, String> collectorConfig = convertFromJobDataMap(jobData);
        Collector collector = CollectorFactory.makeCollector(collectorConfig);

        // Run the collector
        collector.collect();
    }
    
    /** Converts from a Collector-style configuration map to a Quartz JobDataMap. */
    public static JobDataMap convertToJobDataMap(Map<String, Object> collectorConfig) {
        JobDataMap dataMap = new JobDataMap(collectorConfig);
        return dataMap;
    }
    
    /** Converts from a Quartz JobDataMap to a Collector-style configuration map. */
    public static Map<String, String> convertFromJobDataMap(JobDataMap dataMap) {
        Map<String, String> collectorConfig = new HashMap<String, String>();
        
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            collectorConfig.put(key, value);
        }
        
        return collectorConfig;
    }

}
