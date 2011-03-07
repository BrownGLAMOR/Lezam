#Reads data from ImpressionEstimatorTest.
#Plots the results.
library(plotrix)
library(hexbin)


#==============CONFIGUATION================
INPUT.DIR = "~/Desktop/analysis/"
CJC.FILENAME = paste(INPUT.DIR, "iePred1.txt", sep="");
IP.FILENAME = paste(INPUT.DIR, "iePred2.txt", sep="");
LP.FILENAME = paste(INPUT.DIR, "iePred2.txt", sep="");
OUTPUT.DIR = "~/Desktop/analysis/output/"
WATERFALL.OUTPUT.DIR = "~/Desktop/analysis/output/waterfall/"
NUM.SLOTS = 5
COLORS = c("red", "blue", "green", "black", "yellow", "magenta", "brown", "purple")


#==============DATA MANIPULATION================
create.table = function(filename) {
	data = read.table(filename, sep=",", header=TRUE)
	data$err = data$predicted.imps - data$actual.imps
	data$abs.err = abs(data$predicted.imps - data$actual.imps)
	data$log.abs.err = log10(data$abs.err+1)
	data$our.bid.rank = data$our.bid.rank + 1 #get rid of 0-indexing
	data$opp.bid.rank = data$opp.bid.rank + 1 #get rid of 0-indexing
	data$key = paste(data$model,"-",data$game.idx,"-",data$day.idx,"-",data$query.idx,"-",data$our.bid.rank,sep="")
	data
}








plot.waterfalls = function(data, g, d, q, r, NUM.SLOTS, filename) {
	#Input: dataset, game g, day d, query q, our rank r.
	#Output: waterfall data
	data.sub = subset(data, game.idx==g & day.idx==d & query.idx==q & our.bid.rank==r)
	data.sub = data.sub[order(data.sub$opp.bid.rank),] #make data rank-ordered
	
	#Add column for observed number of impressions
	observed.imps = rep("", nrow(data.sub))
	observed.imp.idx = (data.sub$our.bid.rank == data.sub$opp.bid.rank)
	observed.imps[ observed.imp.idx ] = data.sub$actual.imps[observed.imp.idx]
	data.sub$observed.imps = observed.imps
	data.sub$color = COLORS[data.sub$opp.bid.rank]

	
	actual.waterfall = greedy.waterfall.alg(data.sub$actual.imps, NUM.SLOTS)
	predicted.waterfall = greedy.waterfall.alg(data.sub$predicted.imps, NUM.SLOTS)
		
	mean.abs.err = mean(data.sub$abs.err)
	seconds = unique(data.sub$seconds) #should only be 1 val. all are the same, since they came from the same prediction
	subheading = paste("g=",g,", d=",d,", q=",q,", r=",r,", mean.abs.err=",mean.abs.err, "time=",seconds, sep="")

	#Get actual/predicted impressions, average positions
	table1 = data.sub[,c('color', 'opp.bid.rank', 'opp.avg.pos', 'opp.sample.avg.pos', 'observed.imps', 'predicted.imps', 'actual.imps', 'abs.err')]
	colnames(table1) = c('Color', 'Start Slot', 'AvgPos', 'sAvgPos', 'Obs Imps', 'Pred Imps', 'Imps', 'Abs Error')

	
	pdf(filename)
	par( mfrow = c( 3, 1 ), oma = c( 0, 0, 2, 0 ))
	plot.new()
	addtable2plot(0,0, as.matrix(table1), bty="o",display.rownames=FALSE,hlines=TRUE, title="Summary")
	plot.waterfall(predicted.waterfall, NUM.SLOTS, "Predicted")
	plot.waterfall(actual.waterfall, NUM.SLOTS, "Actual")
	title(subheading, outer=TRUE)
	dev.off()
}


greedy.waterfall.alg = function(imps, NUM.SLOTS) {
	#Input: Impressions for agents (sorted by decreasing squashed bid)
	#Output: waterfall data
	
	#Add to the impressions of anyone starting out of the slots.
	if (length(imps) > NUM.SLOTS) {
		for (i in 1:(length(imps)-NUM.SLOTS)) {
			imps[NUM.SLOTS+i] = imps[NUM.SLOTS+i] + sort(imps[1:(NUM.SLOTS+i-1)])[i]
		}
	}
	print(imps)

	tot = matrix(0, length(imps), length(imps))
	for(a in 1:length(imps)) {
		for (s in a:1) {
			my.remaining.imps = imps[a]-sum(tot[a,])
			opp.remaining.above = sum(tot[,s-1])-sum(tot[a,])
			if(s==1) opp.remaining.above = Inf
			tot[a,s] = max( min(my.remaining.imps, opp.remaining.above), 0 )
			print(paste(a, s, my.remaining.imps, opp.remaining.above, tot[a,s]))
		}
	}
	tot
}


#==============PLOTS================

plot.waterfall = function(I.a.s, NUM.SLOTS, title) {

	min.slot = 1
	max.slot = nrow(I.a.s)
	min.imp = 1
	max.imp = max(rowSums(I.a.s))

	plot(0, 0, type='n', lwd=2, xlim=c(min.imp, max.imp), ylim=c(max.slot,min.slot), ylab="Slot", xlab="Impressions", main=title, yaxp=c(min.slot,max.slot,max.slot-min.slot))

	#Loop through agents, plot impressions for each
	for (a in 1:nrow(I.a.s)) {
		#Get start/end point for this agents impressions
		imps.per.slot = I.a.s[a,]
		end.imp = cumsum(rev(imps.per.slot))
		start.imp = c(1, end.imp[1:length(end.imp)-1] + 1)
		slot = length(imps.per.slot):1
		
		#Remove segments that had no impressions
		has.imps = (end.imp-start.imp >= 0)
		end.imp = end.imp[has.imps]
		start.imp = start.imp[has.imps]
		slot = slot[has.imps]
		
		#Plot segments
		segments(start.imp, slot, end.imp, slot, lwd=5, col=COLORS[a])
		#segments(start.imp, slot, end.imp, slot, lwd=5)
	}
	abline(h=NUM.SLOTS+.5, lty=3, col='black', lwd=2)
}



#PREDICTION ACCURACY BY DAY
#PREDICTION ACCURACY BY NUM.PARTICIPANTS
#PREDICTION ACCURACY BY OUR RANK
#PREDICTION ACCURACY BY OPPONENT RANK


#==============MAIN================
cjc = create.table(CJC.FILENAME)
ip = create.table(IP.FILENAME)
lp = create.table(LP.FILENAME)

summary(cjc)
summary(ip)
summary(lp)


pdf(paste(OUTPUT.DIR, "performance.comparison.pdf", sep=""))
plot(cjc$log.abs.err, ip$log.abs.err, main="Performance comparison between models")
dev.off()

pdf(paste(OUTPUT.DIR, "performance.comparison.hexbin.pdf", sep=""))
plot(hexbin(cjc$log.abs.err, ip$log.abs.err), main="Performance comparison between models")
dev.off()


pdf(paste(OUTPUT.DIR, "error.across.days.pdf", sep=""))
xlim=range(cjc$day.idx, ip$day.idx)
ylim=range(cjc$log.abs.err,ip$log.abs.err)
plot(0, 0, type="n", xlim=xlim, ylim=ylim, main="Absolute error across days")
points(cjc$day.idx, cjc$log.abs.err, col="red")
points(ip$day.idx, ip$log.abs.err, col="blue")
legend("topright", NULL, c("CJC","IP"), pch=1, col=c("red", "blue"));
dev.off()

pdf(paste(OUTPUT.DIR, "error.across.ourAvgPos.pdf", sep=""))
plot(ip$our.avg.pos, ip$log.abs.err)
dev.off()

pdf(paste(OUTPUT.DIR, "error.across.oppAvgPos.pdf", sep=""))
plot(ip$opp.avg.pos, ip$log.abs.err)
dev.off()

pdf(paste(OUTPUT.DIR, "error.across.ourBidRank.pdf", sep=""))
plot(ip$our.bid.rank, ip$log.abs.err)
dev.off()

pdf(paste(OUTPUT.DIR, "error.across.focusLevel.pdf", sep=""))
plot(ip$focus.level, ip$abs.err)
dev.off()

pdf(paste(OUTPUT.DIR, "participants.vs.runtime.pdf", sep=""))
plot(as.factor(ip$num.participants), log10(ip$seconds+1), main="Num participants vs. runtime", xlab="Num participants", ylab="log10(seconds+1)")
dev.off()

pdf(paste(OUTPUT.DIR, "runtime.vs.error.pdf", sep=""))
plot(log10(ip$seconds+1), log10(ip$abs.err+1))
dev.off()

#======
#Cumulative distribution plot (for error)
ip.summary = aggregate(ip[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err", "log.abs.err")], list(key=ip$key), mean)
lp.summary = aggregate(lp[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err", "log.abs.err")], list(key=lp$key), mean)
cjc.summary = aggregate(cjc[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err", "log.abs.err")], list(key=cjc$key), mean)
ip.pct.below = NULL
lp.pct.below = NULL
cjc.pct.below = NULL
possible.max.err = seq(0, max(ip.summary$abs.err, lp.summary$abs.err, cjc.summary$abs.err))
for (max.err in possible.max.err) {
	ip.pct.below = c(ip.pct.below, sum(ip.summary$abs.err <= max.err)/nrow(ip.summary))
	lp.pct.below = c(lp.pct.below, sum(lp.summary$abs.err <= max.err)/nrow(lp.summary))
	cjc.pct.below = c(cjc.pct.below, sum(cjc.summary$abs.err <= max.err)/nrow(cjc.summary))
}
#summary stats table
table1 = cbind(summary(ip.summary$abs.err), summary(lp.summary$abs.err), summary(cjc.summary$abs.err))
colnames(table1) = c("IP", "LP", "CJC")

pdf(paste(OUTPUT.DIR, "error.cumulative.distribution.pdf", sep=""))
plot(0, 0, type="n", xlim=c(0, 1000), ylim=c(0,1), main="Frequency of instances under error threshold", xlab="threshold (mean absolute error)", ylab="frequency of predictions with abs.error under threshold")
lines(possible.max.err, ip.pct.below, col="blue", lwd=2)
lines(possible.max.err, lp.pct.below, col="green", lwd=2)
lines(possible.max.err, cjc.pct.below, col="red", lwd=2)
legend("bottomright", NULL, c("IP", "LP", "CJC"), pch=1, lty=1, lwd=2, col=c("blue", "green", "red"));
addtable2plot(0, 0, table1, bty="o",display.rownames=TRUE,hlines=TRUE, title="Summary Stats")
dev.off()


#======
#Cumulative distribution plot (for time)
ip.times = ip$seconds[ip$opp.bid.rank==1] #don't need to sum times across opp.bid.ranks, since they were made in the same prediction
lp.times = lp$seconds[lp$opp.bid.rank==1] #don't need to sum times across opp.bid.ranks, since they were made in the same prediction
cjc.times = cjc$seconds[cjc$opp.bid.rank==1]
ip.pct.below = NULL
lp.pct.below = NULL
cjc.pct.below = NULL
possible.max.time = seq(0, .5, .005)
for (max.time in possible.max.time) {
	ip.pct.below = c(ip.pct.below, sum(ip.times <= max.time)/length(ip.times))
	lp.pct.below = c(lp.pct.below, sum(lp.times <= max.time)/length(lp.times))
	cjc.pct.below = c(cjc.pct.below, sum(cjc.times <= max.time)/length(cjc.times))
}
#summary stats table
table1 = cbind(summary(ip.times), summary(lp.times), summary(cjc.times))
colnames(table1) = c("IP", "LP", "CJC")

pdf(paste(OUTPUT.DIR, "runtime.cumulative.distribution.pdf", sep=""))
plot(0, 0, type="n", xlim=c(0, .5), ylim=c(0,1), main="Frequency of instances under time threshold", xlab="threshold (seconds)", ylab="frequency of predictions with runtime under threshold")
lines(possible.max.time, ip.pct.below, col="blue", lwd=2)
lines(possible.max.time, lp.pct.below, col="green", lwd=2)
lines(possible.max.time, cjc.pct.below, col="red", lwd=2)
legend("bottomright", NULL, c("IP", "LP", "CJC"), pch=1, lty=1, lwd=2, col=c("blue", "green", "red"));
addtable2plot(0, 0, table1, bty="o",display.rownames=TRUE,hlines=TRUE, title="Summary Stats")

dev.off()









#============
#PLOT WATERFALLS WHERE WE HAVE HIGH ERROR
ip.summary = aggregate(ip[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err")], list(key=ip$key), mean)
#Get list of (g, d, q, r) values for which the ip performs poorly
worst.rows = order(ip.summary$abs.err, decreasing=TRUE)
worst.rows = worst.rows[1:30]
for (row in worst.rows) {
	g = ip.summary$game.idx[row]
	d = ip.summary$day.idx[row]
	q = ip.summary$query.idx[row]
	r = ip.summary$our.bid.rank[row]
	filename = paste(WATERFALL.OUTPUT.DIR, "e",round(ip.summary$abs.err[row]),"_","g",g,"d",d,"q",q,"r",r,".pdf",sep="")
	plot.waterfalls(ip, g, d, q, r, NUM.SLOTS, filename)
}



#============
#PLOT WATERFALLS WHERE WE HAVE HIGH TIME
ip.summary = aggregate(ip[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err")], list(key=ip$key), mean)

#Get list of (g, d, q, r) values for which the ip performs poorly
worst.rows = order(ip.summary$seconds, decreasing=TRUE)
worst.rows = worst.rows[1:30]
for (row in worst.rows) {
	g = ip.summary$game.idx[row]
	d = ip.summary$day.idx[row]
	q = ip.summary$query.idx[row]
	r = ip.summary$our.bid.rank[row]
	filename = paste(WATERFALL.OUTPUT.DIR, "s",ip.summary$seconds[row],"_","g",g,"d",d,"q",q,"r",r,".pdf",sep="")
	plot.waterfalls(ip, g, d, q, r, NUM.SLOTS, filename)
}


#============
#PLOT WATERFALLS WHERE CARLETON IS BETTER
ip.summary = aggregate(ip[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err")], list(key=ip$key), mean)
cjc.summary = aggregate(cjc[,c("game.idx", "day.idx", "query.idx", "our.bid.rank", "seconds", "abs.err")], list(key=cjc$key), mean)
diff.err = ip.summary$abs.err - cjc.summary$abs.err

#Get list of (g, d, q, r) values for which the ip performs poorly
worst.rows = order(  diff.err, decreasing=TRUE)
worst.rows = worst.rows[1:30]
for (row in worst.rows) {
	ip.filename = paste(WATERFALL.OUTPUT.DIR, "de",round(diff.err[row]),"_", ip.summary$key[row],".pdf",sep="")
	cjc.filename = paste(WATERFALL.OUTPUT.DIR, "de",round(diff.err[row]),"_", cjc.summary$key[row],".pdf",sep="")
	plot.waterfalls(ip, ip.summary$game.idx[row], ip.summary$day.idx[row], ip.summary$query.idx[row], ip.summary$our.bid.rank[row], NUM.SLOTS, ip.filename)
	plot.waterfalls(cjc, cjc.summary$game.idx[row], cjc.summary$day.idx[row], cjc.summary$query.idx[row], cjc.summary$our.bid.rank[row], NUM.SLOTS, cjc.filename)
}






