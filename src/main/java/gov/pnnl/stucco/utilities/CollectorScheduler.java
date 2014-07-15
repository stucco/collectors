package gov.pnnl.stucco.utilities;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.quartz.CronExpression;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
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
    private void runSchedule(Map<String, Object> collectorsSectionConfig) {
        try {
            Logger log = LoggerFactory.getLogger(CollectorScheduler.class);
            
            Collection<Object> collectorConfigs = (Collection<Object>) collectorsSectionConfig.get("collectors");
                        
            // Start up a scheduler, with no schedule yet
            Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
            sched.start();
            
            for(Object obj : collectorConfigs) {
                Map<String, Object> collectorConfig = (Map<String, Object>) obj;
                JobDataMap jobData = CollectorJob.convertToJobDataMap(collectorConfig);
                
                // Get scheduling info from configuration
                String uri = (String) collectorConfig.get("source-URI");
                String cronExpr = ((String) collectorConfig.get("cron"));
                if (cronExpr == null) {
                    cronExpr = "";
                }
                else {
                    cronExpr = cronExpr.trim();
                }
                
                // Some IDs
                String jobName = uri;
                String triggerName = uri;
                String groupName = "Stucco";
                
                // Define the job
                JobDetail job = newJob(CollectorJob.class)
                        .withIdentity(jobName, groupName)
                        .setJobData(jobData)
                        .build();

                // Define a trigger
                if (CronExpression.isValidExpression(cronExpr)) {
                    // Build the trigger with a cron schedule
                    Trigger trigger = newTrigger()
                            .withIdentity(triggerName, groupName)
                            .startNow()
                            .withSchedule(cronSchedule(cronExpr))
                            .build();
                    sched.scheduleJob(job, trigger);
                }
                else if (cronExpr.equalsIgnoreCase("now")) {
                    // Build the trigger to fire immediately, the default behavior.
                    Trigger trigger = newTrigger()
                            .withIdentity(triggerName, groupName)
                            .startNow()
                            .build();
                    sched.scheduleJob(job, trigger);
                    
                    // Note: As always, if the collector is making a timestamp 
                    // check, it may opt to not collect the URI. We may decide 
                    // we want an option to force collection.
                }
                else if (cronExpr.isEmpty()) {
                    // No expression, treat as commented out
                    log.info("URI \"{}\" skipped because of missing cron expression", uri);
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
