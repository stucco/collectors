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

