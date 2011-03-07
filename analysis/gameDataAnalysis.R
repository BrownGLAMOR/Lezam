library(vioplot)


#==============CONFIGUATION================
INPUT.DIR = "~/Desktop/analysis/"
FILENAME = paste(INPUT.DIR, "gamedata.txt", sep="");
OUTPUT.DIR = "~/Desktop/analysis/gamedata-output/"

COLORS = c("red", "blue", "green", "black", "yellow", "magenta", "brown", "purple", "darkolivegreen", "darkslategray", "seagreen2", "turquoise1", "rosybrown", "palegreen", "gray", "goldenrod")


#==============DATA MANIPULATION================
create.table = function(filename) {
	data = read.table(filename, sep="\t", header=TRUE)
	data$pct.budget = data$cost / data$budget

	data
}





data = create.table(FILENAME)




#Plot an agent's bids over time, for a particular query
a = "MetroClick"

col.name = "squashed.bid"
data.sub = subset(data, agent==a)
yrange = range(data.sub[,col.name])
xrange = range(data.sub$day)
plot(0, 0, type='n', xlim=xrange, ylim=yrange, main=col.name)
#plot(0, 0, type='n', xlim=xrange, ylim=c(0,1000), main=col.name)
queries = unique(data.sub$query)
for (q.idx in 1:length(queries)) {
	q = queries[q.idx]
	data.sub.sub = subset(data.sub, query==q)
	lines(data.sub.sub$day, data.sub.sub[,col.name], type="o", col=COLORS[q.idx])
	#plot(data.sub.sub$day, data.sub.sub[,col.name], type="o", col=COLORS[q.idx])
}

plot(data.sub$query, data.sub[,col.name])



####################
#Make a plot for each query, showing all agents
col.name = "imps"
pdf(paste(OUTPUT.DIR, col.name, "-byquery.pdf", sep=""))
queries = unique(data$query)
agents = unique(data$agent)
for (q.idx in 1:length(queries)) {
	q = queries[q.idx]
	data.sub = subset(data, query==q)
	#data.sub = subset(data, query==q & data[,col.name] > 0)
	plot(jitter(as.numeric(data.sub$agent)), data.sub[,col.name], main=q, ylab=col.name, las=2, col=COLORS[as.numeric(data.sub$agent)])
	#plot(data.sub$agent, data.sub[,col.name], main=q, ylab=col.name, las=2)
	yrange = range(quantile(data.sub[,col.name], c(0, 1), na.rm=TRUE))
	xrange = range(data.sub$day)
	plot(0, 0, type='n', xlim=xrange, ylim=yrange, main=q, xlab="day", ylab=col.name)
	for (a.idx in 1:length(agents)) {
		a = agents[a.idx]
		data.sub.sub = subset(data.sub, agent==a)
		#print(paste(q, a, mean(data.sub.sub$imps), sd(data.sub.sub$imps)))
		lines(data.sub.sub$day, data.sub.sub[,col.name], type="o", col=COLORS[a.idx])
		#plot(data.sub.sub$day, data.sub.sub[,col.name], type="o", col=COLORS[q.idx])
	}
	legend("topleft", NULL, agents, pch=1, col=COLORS[1:length(agents)])
}
dev.off()



####################
#Make a plot for each agent, showing all queries
col.name = "imps"
pdf(paste(OUTPUT.DIR, col.name, "-byagent.pdf", sep=""))
queries = unique(data$query)
agents = unique(data$agent)
for (a.idx in 1:length(agents)) {
	a = agents[a.idx]
	data.sub = subset(data, agent==a)
	#data.sub = subset(data, query==q & data[,col.name] > 0)
	plot(jitter(as.numeric(data.sub$query)), data.sub[,col.name], main=a, ylab=col.name, las=2, col=COLORS[as.numeric(data.sub$agent)])
	#plot(data.sub$query, data.sub[,col.name], main=q, ylab=col.name, las=2)
	yrange = range(quantile(data.sub[,col.name], c(0, 1), na.rm=TRUE))
	xrange = range(data.sub$day)
	plot(0, 0, type='n', xlim=xrange, ylim=yrange, main=a, xlab="day", ylab=col.name)
	for (q.idx in 1:length(queries)) {
		q = queries[q.idx]
		data.sub.sub = subset(data.sub, query==q)
		#print(paste(q, a, mean(data.sub.sub$imps), sd(data.sub.sub$imps)))
		lines(data.sub.sub$day, data.sub.sub[,col.name], type="o", col=COLORS[q.idx])
		#plot(data.sub.sub$day, data.sub.sub[,col.name], type="o", col=COLORS[q.idx])
	}
	legend("topleft", NULL, queries, pch=1, col=COLORS[1:length(agents)])
}
dev.off()



#####################
# Plot summary stats
pdf(paste(OUTPUT.DIR, "gameSummary.pdf", sep=""))
col.names = c("imps", "clicks", "convs", "cost", "bid", "squashed.bid", "budget")
for (col.name in col.names) {
	data.agg = aggregate(data[,c("bid", "squashed.bid", "budget", "imps", "clicks", "convs", "cost")], list(agent=data	$agent, day=data$day), sum)
	agents = unique(data$agent)
	yrange = range(quantile(data.agg[(data.agg[,col.name]<Inf) ,col.name], c(0, 1), na.rm=TRUE))
	xrange = range(data.agg$day)
	plot(0, 0, type='n', xlim=xrange, ylim=yrange, main=col.name, xlab="day", ylab=col.name)
	for (a.idx in 1:length(agents)) {
		a = agents[a.idx]
		data.sub = subset(data.agg, agent==a)
		#lines(data.sub$day, data.sub[,col.name], col=COLORS[a.idx])
		try( plot(data.sub$day, data.sub[,col.name], type='o', col=COLORS[a.idx], main=paste(a, col.name)) )
	}
	legend("topleft", NULL, agents, pch=1, col=COLORS[1:length(agents)])
}
dev.off()


#b = data.sub[,c("query", "day", "imps")]
#b2 = reshape(b, v.names="imps", idvar="query", timevar="day", direction="wide")
#b3 = as.matrix(b2[,-1])
#barplot(b3)