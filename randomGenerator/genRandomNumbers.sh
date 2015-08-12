#!/bin/bash
OUTPUTFILE="randomNumbers.txt"
> $OUTPUTFILE  # Overwriting
for ((i=1;i<=774;i++))  # I should figure out how to pull 774 from elsewhere
do
	if [ $(($i%50)) -eq 0 ]
	then
		echo "Processing: "$i
	fi		
	randomNum=$(od -An -N8 -x /dev/random | tr -d ' ')
	echo -e $randomNum >> $OUTPUTFILE
done
echo "Finished!"
