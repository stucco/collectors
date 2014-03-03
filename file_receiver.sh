#!/bin/bash
java -version
which java
java -Xmx2048m -jar /stucco-shared/collectors/target/file_receiver.jar -file /stucco-shared/collectors/config/stucco.yml
