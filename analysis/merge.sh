
#Merges two files together
#Usage (I think): sh merge.sh "CP.*"

cat header.txt 
awk FNR!=1 $1 
