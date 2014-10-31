This is the README file for the collectors repository.

# Installation #

    ./maven-collectors-build.sh


# Scheduler #

## Scheduler Usage ##

### Quick Start ###
To run the Scheduler from:

- Inside the VM, run 
       
    `/stucco/collectors/scheduler-vm.sh`

- Outside the VM

        cd /stucco/collectors 
        scheduler.sh

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

Each exogenous collector’s configuration contains information about how and when to collect a source. Example from a configuration file:

    default:
      …
      scheduler:
        collectors:
          -
            source-name: Bugtraq
            type: PSEUDO_RSS
            data-type: unstructured
            source-URI: http://www.securityfocus.com/vulnerabilities
            content-type: text/html
            entry-regex: 'href="(/bid/\d+)"'
            tab-regex: 'href="(/bid/\d+/(info|discuss|exploit|solution|references))"'
            next-page-regex: 'href="(/cgi-bin/index\.cgi\?o[^"]+)">Next &gt;<'
            cron: 0 0 23 * * ?
            now-collect: all
    
##### source-name #####
The name of the source, used primarily as a key for RT.

##### type #####
The `type` key specifies the primary kind of collection for a source. Here's one way to categorize the types.

###### Generic Collectors ######
Collectors used to handle the most common cases:

- `RSS`: An RSS feed
- `PSEUDO_RSS`: A Web page acting like an RSS feed, potentially with multiple pages, multiple entries per page, and multiple subpages (tabs) per entry. This uses regular expressions to scrape the URLs it needs to traverse.
- `TABBED_ENTRY`: A Web page with multiple subpages (tabs). In typical use, this will  be a delegate for one of the above collectors, and won't be scheduled directly.
- `WEB`: A single Web page. In typical use, this will be a delegate for one of the above collectors, and won't be scheduled directly.

###### Site-Specific Collectors: ######
Collectors custom-developed for a specific source:

- `NVD`: The National Vulnerability Database
- `BUGTRAQ`: The Bugtraq pseudo-RSS feed. **(Deprecated)** Use PSEUDO_RSS.
- `SOPHOS`: The Sophos RSS feed. **(Deprecated)** Use RSS with a `tab-regex`.

###### Disk-Based Collectors ######
Collectors used for test/debug, to "play back" previously-captured data:

- `FILE`: A file on disk
- `FILEBYLINE`: A file, treated as one document per line
- `DIRECTORY`: A directory on disk

##### source-uri #####
The URI for a source.

##### *-regex #####
The collectors use regular expressions (specifically [Java regexes](https://www.google.com/?q=java+regex#q=java+regex)) to scrape additional links to traverse. There are currently keys for three kinds of links:

- `entry-regex`: In a PSEUDO_RSS feed, this regex is used to identify the individual entries.
- `tab-regex`: In an RSS or PSEUDO_RSS feed, this regex is used to identify the subpages (tabs) of a page.
- `next-page-regex`: In a PSEUDO_RSS feed, this regex is used to identify the next page of entries.


##### cron #####

When to collect is specified in the form of a [Quartz scheduler cron expression](http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger).

    CAUTION: Quartz's first field is SECONDS, not MINUTES like some crons.

- There are seven whitespace-delimited fields (six required, one optional):

        s m h D M d [Y]

These are seconds, minutes, hours, day of month, month, day of week, and year

- Specify * to mean “every”
- Exactly one of the `D/d` fields must be specified as `?` to indicate it isn’t used

In addition, we support specifying a cron expression of `now`, to mean “immediately run once”. 

##### now-collect #####

The `now-collect` configuration key is intended as an improvement on the `now` cron option, offering more nuanced control over scheduler start-up behavior. This key can take the following values:

- `all`: Collect as much as possible, skipping URLs already collected
- `new`: Collect as much as possible, but stop once we find a URL that's already collected
- `none`: Collect nothing; just let the regular schedule do it

NOTE: This feature is not fully implemented at this time. Currently, `all` acts the same as `new`, and both are equivalent to specifying a cron of `now`.

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


Replayer
========
The Replayer is used primarily for development and testing.

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

