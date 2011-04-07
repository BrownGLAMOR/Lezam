
infile1=$1
infile2=$2
mingame=$3
maxgame=$4
agent=$5

R CMD BATCH --no-save --no-restore "--args MIN.GAME.TO.FILTER=$mingame MAX.GAME.TO.FILTER=$maxgame AGENTS.TO.FILTER=\"$agent\" FILE1=\"$infile1\" FILE2=\"$infile2\" OUTFILE=\"err.$mingame-$maxgame.$agent.$infile\"" errorStatisticalTest.R 

