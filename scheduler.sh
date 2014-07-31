#!/bin/bash

# Run outside the VM and at top level of the development directories
java -Xmx2048m -jar collectors/target/scheduler.jar -file config/stucco.yml -section scheduler
