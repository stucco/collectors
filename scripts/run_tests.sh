#!/bin/sh

# This scirpt tests the collectors. It assumes access to rabbitmq and the
# document service. These tests are tightly coupled with the test config file.
# So if you change the test config file you might need to change these tests.

CONFIG=test_config.yml
RECEIVE_DIR="data/Receive"
START_FILE=$RECEIVE_DIR/start
WAIT_TIME=1

test_count=0
pass_count=0
fail_count=0

mkdir -p $RECEIVE_DIR

echo "Starting File Receiver..."
java -Xmx2048m -jar target/file_receiver.jar -file $CONFIG &
file_receiver_pid=$!
sleep 10

echo "###########################################################"
echo "Running web collector test..."
echo "###########################################################"
test_count=$((test_count+1))
touch $START_FILE
java -Xmx2048m -jar target/replayer.jar -file $CONFIG -section replayer-web-test
sleep $WAIT_TIME

# This just checks for the existence of one new file (the collected web page)
if [ $(find $RECEIVE_DIR -type f -newer $START_FILE) ]; then
	echo "SUCCESS"
	pass_count=$((pass_count+1))
else
	echo "FAIL: no new files exist"
	fail_count=$((fail_count+1))
fi


echo "###########################################################"
echo "Running file collector test..."
echo "###########################################################"
test_count=$((test_count+1))
touch $START_FILE
java -Xmx2048m -jar target/replayer.jar -file $CONFIG -section replayer-file-test
sleep $WAIT_TIME

new_file=$(find $RECEIVE_DIR -type f -newer $START_FILE)
if [ "$new_file" ]; then
	# NOTE: this assumes that $CONFIG specifies to collect the file below data/Send/malwaredomains-domains-short.txt
	if diff data/Send/malwaredomains-domains-short.txt $new_file; then
		echo "SUCCESS"
		pass_count=$((pass_count+1))
	else
		echo "FAIL: file does not match"
		fail_count=$((fail_count+1))
	fi
else
	echo "FAIL: no new files exist"
	fail_count=$((fail_count+1))
fi


echo "###########################################################"
echo "Running file-by-line collector test..."
echo "###########################################################"
test_count=$((test_count+1))
touch $START_FILE
java -Xmx2048m -jar target/replayer.jar -file $CONFIG -section replayer-file-by-line-test
sleep $WAIT_TIME

combined=$RECEIVE_DIR/combined
rm $combined
new_file_list=$(find $RECEIVE_DIR -type f -newer $START_FILE -printf '%T@ %p\n' | sort -k1 -n | cut -d" " -f2)
for f in $new_file_list; do
	echo "" >> $combined # add new line between each line, do this first so "diff -B" will work
	cat $f >> $combined
done

# NOTE: this assumes that $CONFIG specifies to collect the file below data/Send/malwaredomains-domains-short.txt
if diff -B data/Send/malwaredomains-domains-short.txt $combined; then
	echo "SUCCESS"
	pass_count=$((pass_count+1))
else
	echo "FAIL: file does not match"
	fail_count=$((fail_count+1))
fi

echo "###########################################################"
printf "Tests: %d, Passed: %d, Failed: %d\n" $test_count $pass_count $fail_count

kill $file_receiver_pid
