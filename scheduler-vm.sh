#!/bin/bash

# to be run within the VM
java -Xmx2048m -jar /stucco/collectors/target/scheduler.jar -file /stucco/config/stucco.yml -section scheduler
