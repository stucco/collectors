#!/bin/bash

# Run outside the VM, in the collectors directory
java -Xmx2048m -jar target/scheduler.jar -file ../config/stucco.yml -section scheduler &
