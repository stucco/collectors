collectors
==========

Install
-------
    ./maven-collectors-build.sh

Run Replayer
-----------
In vagrant, make sure to run this command from the /stucco directory:

    ./collectors/replay.sh

Replayer configuration
----------------------
When using the script above, Replayer will get its configuration from etcd at ETCD_HOST:ETCD_PORT or localhost:4001 if those environment variables are not set.

If you want to specify a configuration file, say /stucco/custom.yml, you can run:

    cd /stucco
    java -Xmx2048m -jar /stucco/collectors/target/replayer.jar –file /stucco/custom.yml

Testing
--------
To test the collectors run:

    ./run_tests.sh

(Before running the tests, make sure rabbitmq and the document service are running.)

# Scheduler #

## Scheduler Usage ##

### Quick Start ###
To run the Scheduler from:

- Inside the VM, run 
       
    `/stucco/collectors/scheduler-vm.sh`

- Outside the VM

        cd /stucco 
        collectors/scheduler.sh

These access the config repository’s stucco.yml. In it the entries for collectors can have a *cron* key. The value is a cron expression.

### More Detailed Information ###

#### Command Line Switches ####
The Schedule is written as a Java application, with main class **gov.pnnl.stucco.utilities.CollectorScheduler**.

The schedule is maintained in the Stucco configuration file, **stucco.yml**. The file can be accessed directly. Alternatively, the scheduler can read from the *etcd* configuration service.

The *CollectorScheduler* class recognizes the following switches:

- `-section` *<configuration-section>*.  This tells the Scheduler what configuration section to use. It is currently a required switch and should be specified as `–section scheduler`.
- `-file` *<configuration-file>*. This tells the Scheduler to read the collector configuration from the given YAML file, currently stucco.yml.
- `-url *<configuration-URL>*. This tells the Scheduler to read the collector configuration from the *etcd* service’s URL, which will typically be *http://10.10.10.100:4001/v2/keys/* (the actual IP may vary depending on your setup). Alternatively, inside the VM, you can use *localhost* instead of the IP.

#### Schedule Format ####

Each collector’s configuration can contain scheduling information in the form of a cron expression.

##### Cron Expression Essentials for Stucco #####


- There are seven whitespace-delimited fields (six required, one optional):

        s m h D M d [Y]
These are seconds, minutes, hours, day of month, month, day of week, and year
- Specify * to mean “every”
- Exactly one of the `D/d` fields must be specified as `?` to indicate it isn’t used

See [http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger](http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger) for greater detail.
In addition, we support specifying a cron expression of “`now`”, to mean “immediately run once”. 

Example from a configuration file:

    default:
      …
      scheduler:
        collectors:
          -
            type: RSS
            data-type: unstructured
            source-name: somesource
            source-URI: http://somesource.org/feed.rss
            content-type: text/xml
            cron: 0 45 23 ? * 2
    
The cron expression in the bottom line says to collect on the 2nd day of the week at 23:45:00; in other words, every Monday at 11:45 PM. 

## Scheduler Implementation ##

### Overview ###
The Scheduler is built in Java, using the Quartz library for running the schedule. The Scheduler instantiates various collector types. In the case of the RSS collector, the collector in turn instantiates a Web page collector for each feed item. The RSS collector uses the Rome library to identify the RSS or Atom feed items.

### Reducing Redundant Work ###
Most of the Scheduler consists of fairly straightforward use of Quartz. The one area that is slightly more complicated is the logic used to try to prevent or at least reduce redundant collection and messaging. We’re trying to avoid collecting pages that haven’t changed since the last collection. Sometimes we may not have sufficient information to avoid such redundant collection, but we can still try to detect the redundancy and avoid re-messaging the content to the rest of Stucco.

Our strategy is to use built-in HTTP features to prevent redundant collection where possible, and to use internal bookkeeping to detect redundant collection when it does happen. We implement this strategy using the following tactics:

- We use HTTP HEAD requests to see if GET requests are necessary. In some cases the HEAD request will be enough to tell that there is nothing new to collect. 
- We make both HTTP HEAD and GET requests conditional, using HTTP’s If-Modified-Since and If-None-Match request headers. If-Modified-Since checks against a timestamp.  If-None-Match checks against a previously returned response header called an ETag (entity tag). An ETag is essentially an ID of some sort, often a checksum.
- We record a SHA-1 checksum on collected content, so we check it for a match the next time. This is necessary because not all sites run the conditional checks. For an RSS (or Atom) feed, the checksum is performed on the set of feed URLs.

Because of the timing of the various checks, they are conducted within the collectors.

