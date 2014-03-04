collectors
==========

Install
-------
    mvn install

Running the install command will create a target directory, where replayer.jar will be stored.

Run replayer
-----------
    # Gets configuration from etcd at ETCD_HOST:ETCD_PORT or localhost:4001
    ./replay.sh
    
    # Or you can specify a configuration file:
    java -jar target/replayer.jar -file ../config/stucco.yml

