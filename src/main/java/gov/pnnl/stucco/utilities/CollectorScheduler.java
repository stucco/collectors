package gov.pnnl.stucco.utilities;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import gov.pnnl.stucco.collectors.CollectorHttp;
import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.quartz.CronExpression;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * Scheduler for Stucco collectors. Read cron specifications from a config map,
 * and schedules collection (using the Quartz library). 
 * 
 * @author Grant Nakamura, June 2014
 */
public class CollectorScheduler {
    @SuppressWarnings({ "unchecked" })
    public void runSchedule(Map<String, Object> collectorsSectionConfig) {
        try {
            Logger log = LoggerFactory.getLogger(CollectorScheduler.class);
            
            Collection<Object> collectorConfigs = (Collection<Object>) collectorsSectionConfig.get("collectors");
                        
            // Start up a scheduler, with no schedule yet
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
            sched.start();
            
            for(Object obj : collectorConfigs) {
                Map<String, Object> collectorConfig = (Map<String, Object>) obj;
                
                // Get info from configuration
                
                // URI
                String uri = (String) collectorConfig.get(CollectorHttp.SOURCE_URI);
                
                // What to do on start up
                String startUp = (String) collectorConfig.get(CollectorHttp.NOW_COLLECT_KEY);
                startUp = (startUp == null)? "none" : startUp.trim();
                
                // The schedule
                String cronExpr = ((String) collectorConfig.get("cron"));
                cronExpr = (cronExpr == null)? "" : cronExpr.trim();
                
                
                // We'll collect at startup and/or on a schedule
                
                if (!startUp.equalsIgnoreCase("none")) {
                    // Copy the configuration data as is
                    Map<String, Object> copyConfig = new HashMap<String, Object>(collectorConfig);
                    JobDataMap jobData = CollectorJob.convertToJobDataMap(copyConfig);
                    
                    // Schedule immediately
                    scheduleJob(sched, jobData, uri, "now");
                }
                
                if (CronExpression.isValidExpression(cronExpr)) {
                    // Copy configuration data, but remove key intended for start-up only
                    Map<String, Object> copyConfig = new HashMap<String, Object>(collectorConfig);
                    copyConfig.remove(CollectorHttp.NOW_COLLECT_KEY);
                    JobDataMap jobData = CollectorJob.convertToJobDataMap(copyConfig);

                    // Add to schedule
                    scheduleJob(sched, jobData, uri, cronExpr);
                }
                else if (cronExpr.isEmpty()) {
                    // No expression, treat as commented out
                    log.info("URI \"{}\" has no cron expression", uri);
                }
                else {
                    // Not a recognized schedule specification
                    log.error("URI \"{}\" has invalid cron expression \"{}\"", uri, cronExpr);
                }
            }

            // Sleep for pretty much forever. Other threads will do the real work.
            // NOTE: This suffices for now, but will likely change as we evolve
            // the scheduler to allow interactive control.
            Thread.sleep(Long.MAX_VALUE);
        } 
        catch (SchedulerException e) {
            e.printStackTrace();
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }
    
    /**
     * Schedules a Quartz job/trigger pair.
     * 
     * @param sched
     * Quartz scheduler
     * 
     * @param jobData
     * Any data parameters needed by the job
     * 
     * @param name
     * Name to use for the job and trigger
     * 
     * @param cronExpr
     * Quartz-style cron expression or the word "now" (for immediate execution)
     * 
     * @throws SchedulerException if unable to schedule
     */
    private void scheduleJob(Scheduler sched, JobDataMap jobData, String name, String cronExpr) throws SchedulerException  {
        boolean immediate = cronExpr.equalsIgnoreCase("now");
        
        // Set up various identifiers
        String jobName = name;
        String triggerName = name;
        String groupName = immediate? "startup" : "cron";
        
        // Define the job
        JobDetail job = newJob(CollectorJob.class)
                .withIdentity(jobName, groupName)
                .setJobData(jobData)
                .build();
        
        // Shared part of trigger set up
        TriggerBuilder<Trigger> builder = newTrigger()
                .withIdentity(triggerName, groupName)
                .startNow();
        
        // Build the trigger
        Trigger trigger = null;
        if (immediate) {
            //... for immediate running
            trigger = builder.build();
        }
        else if (CronExpression.isValidExpression(cronExpr)) {
            //... for a cron schedule
            trigger = builder.withSchedule(cronSchedule(cronExpr)).build();
        }

        sched.scheduleJob(job, trigger);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try {
            // Parse the command line
            
            CommandLine parser = new CommandLine();
            parser.add1("-file");
            parser.add1("-url");
            parser.add1("-section");
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
            
            String section = "scheduler";
            if (parser.found("-section")) {
                section = parser.getValue();
            }
            
            
            // Get the configuration data
            Map<String, Object> config = Config.getMap();
            Map<String, Object> sectionMap = (Map<String, Object>) config.get(section);

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
            
            // Run the scheduler for a really long time
            CollectorScheduler scheduler = new CollectorScheduler();
            scheduler.runSchedule(sectionMap);
        } 
        catch (UsageException e) {
            System.err.println("Usage: CollectorScheduler (-file configFile | -url configUrl) [-section section]");
            System.exit(1);
        }
    }
    
}
