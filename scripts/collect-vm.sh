#!/bin/bash

# In order to run this script you should specify the section you want for this to run
# Choices for the configSection are: demo, production
# to be run within the VM
#usage: (-load|-schedule) -config <configFile> -section <configSection>
java -Xmx2048m -jar /stucco/collectors/target/collect.jar $1 -config $2 -section $3
