
infile=$1
mingame=$2
maxgame=$3
agent=$4

R CMD BATCH --no-save --no-restore "--args MIN.GAME.TO.FILTER=$mingame MAX.GAME.TO.FILTER=$maxgame AGENTS.TO.FILTER=\"$agent\" FILE=\"$infile\" OUTFILE=\"err.$mingame-$maxgame.$agent.$infile\"" errorSummary.R 

