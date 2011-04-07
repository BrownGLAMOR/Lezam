#Get in-depth results for the paper

BASE.DIR = "/Users/sodomka/mydocs/workspace/AA-new2/analysis/inDepth/data/"
FILENAME1 = "CP.rie.exact.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt"
FILENAME2 = "CP.rie.exact.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt"
FILENAME3 = "CP.rie.sampled.noPrior.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt"
FILENAME4 = "CP.rie.sampled.historic-EMA.finals2010.games-15127-15137.days-0-57.queries-0-15.agents-all.txt"
GAME.FILTER = 15127:15136

#######
infile1 = paste(BASE.DIR, FILENAME1, sep="")
infile2 = paste(BASE.DIR, FILENAME2, sep="")
infile3 = paste(BASE.DIR, FILENAME3, sep="")
infile4 = paste(BASE.DIR, FILENAME4, sep="")


data1 = create.table(infile1)
data1.sub = subset(data1, data1$game.idx %in% GAME.FILTER)

data2 = create.table(infile2)
data2.sub = subset(data2, data2$game.idx %in% GAME.FILTER)

data3 = create.table(infile3)
data3.sub = subset(data3, data3$game.idx %in% GAME.FILTER)

data4 = create.table(infile4)
data4.sub = subset(data4, data4$game.idx %in% GAME.FILTER)

data=data3.sub



############
## Get #imps in each auction
##Assumes we've made predictions for everyone
imps.in.auction = NULL
games = unique(data$game.idx)
for (g in games) {
	data1 = subset(data, data$game.idx==g)
	days = unique(data1$day.idx)
	for (d in days) {
		data2 = subset(data1, data1$day.idx==d)
		queries = unique(data2$query.idx) 
		for (q in queries) {
			data3 = subset(data2, data2$query.idx==q)
			opp.bid.ranks = unique(data3$opp.bid.rank)
			imps = NULL
			for (r in 1:length(opp.bid.ranks)) {
				imps = c(imps, data3$actual.imps[data3$opp.bid.rank==r][1])
			}
			actual.waterfall = greedy.waterfall.alg(imps, NUM.SLOTS)
			slot1.imps = sum(actual.waterfall[,1])
			imps.in.auction = rbind(imps.in.auction, c(g, d, q, slot1.imps))
		}
	}
}
colnames(imps.in.auction) = c("game.idx", "day.idx", "query.idx", "imps.in.auction")
imps.in.auction = as.data.frame(imps.in.auction)
data = merge(data, imps.in.auction)
data$pct.imps.seen = data$actual.imps / data$imps.in.auction





###################
# By agent

#Output %imps seen by agent 
#Pct. imps the PREDICTING agent saw
for (agent in unique(data$our.agent)) { 
	print(agent); 
	print(summary(data$pct.imps.seen[data$our.agent==agent & data$our.bid.rank==data$opp.bid.rank])  )
}


#Output MAE by agent
for (agent in unique(data$our.agent)) { 
	print(agent); 
	print(summary(data$abs.err[data$our.agent==agent]))
}

#Output standard errors
for (agent in unique(data$our.agent)) { 
	print(agent); 
	print(sd(data$abs.err[data$our.agent==agent])/sqrt(sum(data$our.agent==agent))  )
}

#Output num predictions
for (agent in unique(data$our.agent)) { 
	print(agent) 
	print(sum(data$our.agent==agent))
}

#How often is each agent in an integer average posiion?
for (agent in unique(data$our.agent)) { 
	print(agent) 
	print(sum(data$our.avg.pos[data$our.agent==agent]==round(data$our.avg.pos[data$our.agent==agent])) / length(data$our.agent==agent))
}

#Plot pct integer vs. performance
errs = NULL
pcts = NULL
for (agent in unique(data$our.agent)) { 
	rows = data$our.agent==agent
	err = mean(data$abs.err[rows])
	errs = c(errs, err)
	pct = sum(data$our.avg.pos[rows]==round(data$our.avg.pos[rows])) / length(data$our.avg.pos[rows])
	pcts = c(pcts, pct)
}
plot(pcts, errs)


###################
# By query focus level
for (focus.level in unique(data$focus.level)) {
	print(focus.level); 
	print(summary(data$abs.err[data$focus.level==focus.level]))
}


################
# By agent and focus level
stats = NULL
for (agent in unique(data$our.agent)) {
	agent.stats = NULL
	for (focus.level in unique(data$focus.level)) {
		rows=data$focus.level==focus.level & data$our.agent==agent
		agent.rows = data$our.agent==agent
		
		err = mean(data$abs.err[rows])
		#N = sum(rows)
		pct.N = 100*sum(rows)/sum(agent.rows)
		pct.int = 100*sum(data$our.avg.pos[rows]==round(data$our.avg.pos[rows])) / sum(rows)
		pct.1 = 100*sum(data$our.avg.pos[rows]==1) / sum(rows)
		mean.pct.seen = 100*mean(data$pct.imps.seen[ data$our.agent==agent & data$focus.level==focus.level & data$our.bid.rank==data$opp.bid.rank]  )
		level.stats = t(as.matrix(c(pct.N, pct.int, pct.1, mean.pct.seen)))
		
		colnames(level.stats) = c("\\%N", "\\%X.0", "\\%1.0", "\\%Imp")
		agent.stats = cbind(agent.stats, level.stats)
	}
	N = sum(data$our.agent==agent)
	agent.stats = cbind(N, agent.stats)
	rownames(agent.stats) = agent
	stats = rbind(stats, agent.stats)
}


#Make table of stats
latex.mat = xtable(stats)
align(latex.mat) = "|r|r|rrrr|rrrr|rrrr|"
caption(latex.mat) = "Metrics of agent behavior by query focus level, across ten 2010 TAC AA finals games. From left to right, columns are as follows: agent name, number of predictions made, percentage of predictions made in a given focus level, percentage of auctions where the agent had an integer average position, percentage of auctions where the agent was in the top slot, average percentage of impressions seen before the agent dropped out."
digits(latex.mat) = 0 #c(0, 0, 2, 2, 2, 2, 0, 2, 2, 2, 2, 0, 2, 2, 2, 2)
header = "\\hline  &  & \\multicolumn{4}{c|}{F0} & \\multicolumn{4}{c|}{F1} & \\multicolumn{4}{c|}{F2} \\\\"
print(latex.mat, sanitize.text.function=function(x){x}, add.to.row=list(pos=list(-1), command=c(header)))
