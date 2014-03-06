collectors
==========

Install
-------
    mvn install

The vagrant scripts should automatically install collectors. If you need to reinstall (rebuild) collectors within vagrant make sure to run the above command as root.

Running the install command will create a target directory, where replayer.jar will be stored.

Run Replayer
-----------
In vagrant, make sure to run this command from the /stucco directory:
    ./collectors/replay.sh

When using the script above, Replayer will get its configuration from etcd at ETCD_HOST:ETCD_PORT or localhost:4001 if those environment variables are not set.

