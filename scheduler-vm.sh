#!/bin/bash

# In order to run this script you should specify the section you want for this to run
# Choices are: demo-load, production
# to be run within the VM
#java -Xmx2048m -jar /stucco/collectors/target/scheduler.jar -file /stucco/config/stucco.yml -section $1
java -Xmx2048m -jar /stucco/collectors/target/scheduler.jar -section $1
