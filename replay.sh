#!/bin/bash
#java -Xmx2048m -jar /stucco/collectors/target/replayer.jar
java -Xmx2048m -jar /stucco/collectors/target/replayer.jar -file $1 -section $2
