
mingame=15127
maxgame=15136
#agent=( Schlemazl )

agents=( Schlemazl TacTex Mertacor MetroClick crocodileagent Nanda_AA tau McCon )
for agent in ${agents[@]}
do
#qsub -cwd runErrorSummary.sh CP.ie.exact.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt $mingame $maxgame $agent        
#qsub -cwd runErrorSummary.sh CP.rie.exact.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
#qsub -cwd runErrorSummary.sh CP.ie.exact.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent         
#qsub -cwd runErrorSummary.sh CP.rie.exact.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
#qsub -cwd runErrorSummary.sh CP.ie.sampled.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
#qsub -cwd runErrorSummary.sh CP.rie.sampled.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
#qsub -cwd runErrorSummary.sh CP.ie.sampled.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
#qsub -cwd runErrorSummary.sh CP.rie.sampled.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent

qsub -cwd runErrorSummary.sh CP.ie.exact.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt $mingame $maxgame $agent        
qsub -cwd runErrorSummary.sh CP.rie.exact.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh CP.ie.exact.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent         
qsub -cwd runErrorSummary.sh CP.rie.exact.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh CP.ie.sampled.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh CP.rie.sampled.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh CP.ie.sampled.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh CP.rie.sampled.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt  $mingame $maxgame $agent

qsub -cwd runErrorSummary.sh MIP.ie.exact.historic-EMA.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt $mingame $maxgame $agent        
qsub -cwd runErrorSummary.sh MIP.rie.exact.historic-EMA.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh MIP.ie.exact.noPrior.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt  $mingame $maxgame $agent         
qsub -cwd runErrorSummary.sh MIP.rie.exact.noPrior.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt  $mingame $maxgame $agent

qsub -cwd runErrorSummary.sh MIP_LP.ie.exact.historic-EMA.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt $mingame $maxgame $agent        
qsub -cwd runErrorSummary.sh MIP_LP.rie.exact.historic-EMA.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt  $mingame $maxgame $agent
qsub -cwd runErrorSummary.sh MIP_LP.ie.exact.noPrior.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt  $mingame $maxgame $agent         
qsub -cwd runErrorSummary.sh MIP_LP.rie.exact.noPrior.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-Schlemazl.txt  $mingame $maxgame $agent

done


