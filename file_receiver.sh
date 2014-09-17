#!/bin/bash

# Instructions: Run this from the collectors directory

java -Xmx2048m -jar target/file_receiver.jar -file ../config/stucco.yml &
