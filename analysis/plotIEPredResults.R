#Reads data from ImpressionEstimatorTest.
#Plots the results.
library(plotrix)
library(hexbin)
library(splus2R)
library(xtable)

TEST.SET = "test2010"

# Create the list of algorithms
ALGORITHMS = c("CP", "MIP", "MIP_LP", "LDSMIP")


# These are the options for the different problems
RANKING.OPTIONS = c("ie", "rie")
#SAMPLING.OPTIONS = c("exact", "sampled")
SAMPLING.OPTIONS = "exact"
#UB.OPTIONS = c("impsPerfect", "impsUB")
UB.OPTIONS = "impsPerfect"
#PRIOR.OPTIONS = c("prior", "noPrior")
PRIOR.OPTIONS = "prior"
NOISE.OPTIONS = c("0.0", "0.5", "1.0", "1.5")


INPUT.DIR = "../" #"~/mydocs/workspace/AA-new2/analysis/input/"
OUTPUT.DIR = "~/mydocs/workspace/AA-new2/analysis/output/"
LATEX.OUTPUT.DIR = paste(OUTPUT.DIR, "latex/", sep="")
WATERFALL.OUTPUT.DIR = "~/mydocs/workspace/AA-new2/analysis/output/waterfall/"
NUM.SLOTS = 5
COLORS = c("red", "blue", "green", "black", "yellow", "magenta", "brown", "purple")






# Create the list of possible problems, based on the given options
PROBLEMS = NULL
for (ranking in RANKING.OPTIONS) {
	for (sampling in SAMPLING.OPTIONS) {
		for (ub in UB.OPTIONS) {
			for (prior in PRIOR.OPTIONS) {
				for (noise in NOISE.OPTIONS) {
					if (prior=="noPrior" && noise!=NOISE.OPTIONS[1]) {
						#skip these -- noise doesn't matter if no priors
					} else {
						noiseStr = paste("noise", noise, sep="")
						problem = paste(ranking, sampling, ub, prior, noiseStr, sep=".")
						PROBLEMS = c(PROBLEMS, problem)
					}
				}
			}
		}
	}
}



#Get statistics on each problem/algorithm combination
all.stats = NULL
for (problem in PROBLEMS) {
	#Keep track of errors for each problem so we can record which error was best
	all.errors.I = NULL
	all.errors.I.a = NULL
	all.errors.I.a.s = NULL
	all.times = NULL
	all.rank.abs.error = NULL
	all.perfect.ranking = NULL
	for (algorithm in ALGORITHMS) {
		file = paste(algorithm, problem, TEST.SET, "txt", sep=".")
		full.file = paste(INPUT.DIR, file, sep="")
		I.a = create.table(full.file)
		I.a.s = compute.slot.prediction.error(I.a) #Are these sorted?
		I = compute.I(I.a.s)

		#Get times
		times = I.a$seconds[I.a$opp.bid.rank==1]
		
		#Append the errors of these predictions to the all.errors matrix
		all.errors.I.a = cbind(all.errors.I.a, I.a$abs.err)
		all.errors.I = cbind(all.errors.I, I$abs.err)
		all.errors.I.a.s = cbind(all.errors.I.a.s, I.a.s$abs.err[I.a.s$opp.bid.rank >= I.a.s$slot]) #Only look at slot predictions that were non-obvious (e.g. 1st slot agent can never be in 2nd slot)
		
		all.times = cbind(all.times, times)
		all.rank.abs.error = cbind(all.rank.abs.error, I.a$rank.abs.err)
		all.perfect.ranking = cbind(all.perfect.ranking, I.a$perfect.ranking)
	}
	
	#Now that we have all errors for this problem (for every algorithm), compute stats.
	for (algorithm.idx in 1:length(ALGORITHMS)) {
		algorithm = ALGORITHMS[algorithm.idx]

		I.stats = quantiles.and.mean(all.errors.I, algorithm.idx)
		I.a.stats = quantiles.and.mean(all.errors.I.a, algorithm.idx)
		I.a.s.stats = quantiles.and.mean(all.errors.I.a.s, algorithm.idx)
		time.stats = sum( all.times[,algorithm.idx] )
		
		
		pct.perfect.rank = sum(all.perfect.ranking[,algorithm.idx] == TRUE) / nrow(all.perfect.ranking)
		avg.rank.dist = mean(all.rank.abs.error[,algorithm.idx])
		
		imperfect.rows = all.perfect.ranking[,algorithm.idx] == FALSE
		imperfect.avg.rank.dist = mean(all.rank.abs.error[imperfect.rows,algorithm.idx])

		names(problem) = "problem"
		names(algorithm) = "algorithm"
		names(time.stats) = "mean.seconds"
		names(pct.perfect.rank) = "pct.perfect.rank"
		names(avg.rank.dist) = "avg.rank.dist"
		names(imperfect.avg.rank.dist) = "imperfect.avg.rank.dist"
		stats = c(problem, algorithm, I.stats, I.a.stats, I.a.s.stats, time.stats, pct.perfect.rank, avg.rank.dist, imperfect.avg.rank.dist)
		all.stats = rbind(all.stats, stats)
	}
}
rownames(all.stats) = NULL
all.stats = as.data.frame(all.stats, stringsAsFactors=FALSE)
for (col in 3:ncol(all.stats)) {
	all.stats[,col] = as.numeric(all.stats[,col])
}


for (algorithm in ALGORITHMS) {
	latex.table(all.stats, algorithm)
}

#Make a table of the PROBLEMS
print(xtable(as.matrix(PROBLEMS)), include.colnames=FALSE, append=TRUE, file=paste(LATEX.OUTPUT.DIR,"tables.tex",sep=""))



latex.table = function(all.stats, algorithm) {
	mat = NULL
	for (problem in PROBLEMS) {
		stats = all.stats[all.stats$problem==problem & all.stats$algorithm==algorithm , c(-2)]
		
		#Shorten problem names to just be an index
		stats$problem = which(PROBLEMS == stats$problem)
		
		mat = rbind(mat, stats)
	}
	
	
	rownames(mat) = NULL
	latex.mat = xtable(mat)
	clean.algorithm = sub("_", "-", algorithm)
	caption(latex.mat) = clean.algorithm
	digits(latex.mat) = 2
	hlines = c(-1,0,nrow(mat), length(PROBLEMS)/2)
	align(latex.mat) = "l|l|ccccc|ccccc|ccccc|c|ccc|"
	header.sub = "mean & med & max & \\%$C$ & \\%$B$"
	header.multi = "& \\multicolumn{5}{c|}{Total Imps} & \\multicolumn{5}{c|}{Per Agent Imps} & \\multicolumn{5}{c|}{Per Slot Imps} & Time & \\multicolumn{3}{c|}{Rank Error} \\\\"
	header=paste(header.multi, " & ", header.sub, " & ", header.sub, " & ", header.sub, " & ", "secs &  \\%C & d1 & d2 \\\\", sep="")
	print(latex.mat, file=paste(LATEX.OUTPUT.DIR,"tables.tex",sep=""), hline.after=hlines, append=TRUE, include.rownames=FALSE, include.colnames=FALSE, floating=TRUE, add.to.row=list(pos=list(0), command=c(header)))
}




#Input: vector v
#Output: (mean, median, min, max)
quantiles.and.mean = function(val.matrix, col.idx) {

	v = val.matrix[,col.idx]
	val.min = apply(val.matrix, 1, function(x) min(x))
	
	pct.lowest = sum(v==val.min) / length(v)
	pct.zero = sum(v==0) / length(v)
	#stats = c(mean(v), median(v), min(v), max(v), pct.zero)
	#names(stats) = c("mean", "median", "min", "max", "pct.zero")
	stats = c(mean(v), median(v), max(v), pct.zero, pct.lowest)
	names(stats) = c("mean", "median", "max", "pct.zero", "pct.lowest")
	stats
}





compute.I = function(I.a.s) {
	#Get I predictions (i.e. total imps in the 1st slot)
	I = aggregate(I.a.s[,c("predicted.imps", "actual.imps")], list(game.idx=I.a.s$game.idx, day.idx=I.a.s$day.idx, query.idx=I.a.s$query.idx, 	our.bid.rank=I.a.s$our.bid.rank, slot=I.a.s$slot), sum)
	I = I[I$slot==1,]
	I$abs.err = abs(I$predicted.imps - I$actual.imps)
	I
}






#output: g, d, q, myRank, oppRank, slotPredictionError
#where slotPredictionError = mean_{slot} abs(I_a_S - predI_a_s)
compute.slot.prediction.error = function(data) {
	
	#For each game, day, query, ourRank:
	#compute predicted waterfall
	#compute actual waterfall
	#Create summary stat for each predicted agent: mean absolute impsPerSlot error
	#Return a data frame containing (g, d, q, r, oppRank, impsPerSlotErrorMetric)
	#NO: instead, return a data frame containing (g, d, q, r, oppRank, oppPossibleSlot, oppPredictedImps, oppActualImps)
	#  should oppPossibleSlot always be <= oppRank? (The agent can't possibly be in slots above where it started)
	#  (but if you incorrectly guess it's starting rank, it could actually see some there)

	error.summary = NULL
	games = unique(data$game.idx)
	for (g in games) {
		data1 = subset(data, data$game.idx==g)
		days = unique(data1$day.idx)
		for (d in days) {
			data2 = subset(data1, data1$day.idx==d)
			queries = unique(data2$query.idx) 
			for (q in queries) {
				data3 = subset(data2, data2$query.idx==q)
				our.bid.ranks = unique(data3$our.bid.rank) 
				for (r in our.bid.ranks) {
					data4 = subset(data3, data3$our.bid.rank==r)
					data5 = data4[order(data4$opp.bid.rank),] #make data rank-ordered
					actual.waterfall = greedy.waterfall.alg(data5$actual.imps, NUM.SLOTS)
					predicted.waterfall = greedy.waterfall.alg(data5$predicted.imps, NUM.SLOTS)
					
					opponent.vector = rep(1:nrow(actual.waterfall), times=ncol(actual.waterfall))
					slot.vector = rep(1:ncol(actual.waterfall), each=nrow(actual.waterfall))
					predicted.vector = as.vector(predicted.waterfall)
					actual.vector = as.vector(actual.waterfall)
					abs.err.vector = abs(predicted.vector - actual.vector)
					
					new.error.data = cbind(g, d, q, r, opponent.vector, slot.vector, predicted.vector, actual.vector, abs.err.vector)
					error.summary = rbind(error.summary, new.error.data)					
				}	
			}
		}
	}
	colnames(error.summary) = c("game.idx", "day.idx", "query.idx", "our.bid.rank", "opp.bid.rank", "slot", "predicted.imps", "actual.imps", "abs.err")
	error.summary = as.data.frame(error.summary)
}







#################################
# MAKE TABLE OF RESULTS

get.output.table.row = function(I.a) {
#Make sure results are sorted (note: we only really have to do this when comparing across predictors. And we could just merge)
I.a = I.a[order(I.a$game.idx, I.a$day.idx, I.a$query.idx, I.a$our.bid.rank, I.a$opp.bid.rank),]

#Get times 
times = I.a$seconds[I.a$opp.bid.rank==1]

#Get slot predictions
I.a.s = compute.slot.prediction.error(I.a) #Are these sorted?

#Get I predictions (i.e. total imps in the 1st slot)
I = aggregate(I.a.s[,c("predicted.imps", "actual.imps")], list(game.idx=I.a.s$game.idx, day.idx=I.a.s$day.idx, query.idx=I.a.s$query.idx, our.bid.rank=I.a.s$our.bid.rank, slot=I.a.s$slot), sum)
I = I[I$slot==1,]
I$abs.err = abs(I$predicted.imps - I$actual.imps)

#sanity check. should give the same as the original I.a data file
#I.a = aggregate(I.a.s[,c("predicted.imps", "actual.imps")], list(game.idx=I.a.s$game.idx, day.idx=I.a.s$day.idx, query.idx=I.a.s$query.idx, our.bid.rank=I.a.s$our.bid.rank, opp.bid.rank=I.a.s$opp.bid.rank), sum)
#I.a$abs.err = abs(I.a$predicted.imps - I.a$actual.imps)
#I.a[order(I.a$game.idx, I.a$day.idx, I.a$query.idx, I.a$our.bid.rank, I.a$opp.bid.rank),]

I.stats = quantiles.and.mean(I$abs.err)
I.a.stats = quantiles.and.mean(I.a$abs.err)
I.a.s.stats = quantiles.and.mean( I.a.s$abs.err[I.a.s$opp.bid.rank >= I.a.s$slot] ) #Only look at slot predictions that were non-obvious (e.g. 1st slot agent can never be in 2nd slot)
time.stats = mean(times)
names(time.stats) = "mean.seconds"

stats = c(I.stats, I.a.stats, I.a.s.stats, time.stats)
}















#iePred.CP.exact.impsPerfect.noPrior.test2010.txt
#iePred.CP.exact.impsPerfect.prior.test2010.txt
#iePred.CP.exact.impsUB.noPrior.test2010.txt
#iePred.CP.exact.impsUB.prior.test2010.txt
#iePred.CP.sampled.impsPerfect.noPrior.test2010.txt
#iePred.CP.sampled.impsPerfect.prior.test2010.txt
#iePred.CP.sampled.impsUB.noPrior.test2010.txt
#iePred.CP.sampled.impsUB.prior.test2010.txt
#iePred.MIP.exact.impsPerfect.noPrior.test2010.txt
#iePred.MIP.exact.impsPerfect.prior.test2010.txt
#iePred.MIP.exact.impsUB.noPrior.test2010.txt
#iePred.MIP.exact.impsUB.prior.test2010.txt
#iePred.MIP.sampled.impsPerfect.noPrior.test2010.txt
#iePred.MIP.sampled.impsPerfect.prior.test2010.txt
#iePred.MIP.sampled.impsUB.noPrior.test2010.txt
#iePred.MIP.sampled.impsUB.prior.test2010.txt

#==============CONFIGUATION================
cjc.filename.part = "CP.ie.sampled.impsPerfect.noPrior.noise0.5.test2010.txt"
ip.filename.part = "MIP_LP.ie.sampled.impsPerfect.noPrior.noise0.5.test2010.txt"
lp.filename.part = "MIP_LP.ie.sampled.impsPerfect.noPrior.noise0.5.test2010.txt"

#======= INFERRED BY CONFIGURATION =====
CJC.FILENAME = paste(INPUT.DIR, cjc.filename.part, sep="");
IP.FILENAME = paste(INPUT.DIR, ip.filename.part, sep="");
LP.FILENAME = paste(INPUT.DIR, lp.filename.part, sep="");



#==============DATA MANIPULATION================
create.table = function(filename) {
	data = read.table(filename, sep=",", header=TRUE)
	data$err = data$predicted.imps - data$actual.imps
	data$abs.err = abs(data$predicted.imps - data$actual.imps)
	data$log.abs.err = log10(data$abs.err+1)
	data$our.bid.rank = data$our.bid.rank + 1 #get rid of 0-indexing
	data$opp.bid.rank = data$opp.bid.rank + 1 #get rid of 0-indexing
	data$predicted.opp.bid.rank = data$predicted.opp.bid.rank + 1 #get rid of 0-indexing
	data$key = paste(data$model,"-",data$game.idx,"-",data$day.idx,"-",data$query.idx,"-",data$our.bid.rank,sep="")
	data$rank.abs.err = abs(data$opp.bid.rank - data$predicted.opp.bid.rank)
	
	#Determine if the ordering was perfect
	a = aggregate(data$rank.abs.err, list(game.idx=data$game.idx, day.idx=data$day.idx, query.idx=data$query.idx, our.bid.rank=data$our.bid.rank), mean)
	a$perfect.ranking = (a$x == 0)
	a = subset(a, select = -x)
	data = merge(data, a) #merge by common variable names

	#Determine if any predictions were made
	a = aggregate(data$predicted.imps, list(game.idx=data$game.idx, day.idx=data$day.idx, query.idx=data$query.idx, our.bid.rank=data$our.bid.rank), mean)
	a$made.predictions = (a$x > 0)
	a = subset(a, select = -x)
	data = merge(data, a) #merge by common variable names

	#Determine how many impressions were seen by any agents in this auction
	
	
	data
}




















plot.waterfalls = function(data, g, d, q, r, NUM.SLOTS, filename) {
	#Input: dataset, game g, day d, query q, our rank r.
	#Output: waterfall data
	data.sub = subset(data, game.idx==g & day.idx==d & query.idx==q & our.bid.rank==r)
	data.sub = data.sub[order(data.sub$opp.bid.rank),] #make data rank-ordered
	
	#Add column for observed number of impressions (for plotted table)
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
	#Row i corresponds to the agent that started in position i
	#Column j corresponds to slot j
	
	#Add to the impressions of anyone starting out of the slots.
	if (length(imps) > NUM.SLOTS) {
		for (i in 1:(length(imps)-NUM.SLOTS)) {
			imps[NUM.SLOTS+i] = imps[NUM.SLOTS+i] + sort(imps[1:(NUM.SLOTS+i-1)])[i]
		}
	}
	#print(imps)

	tot = matrix(0, length(imps), length(imps))
	for(a in 1:length(imps)) {
		for (s in a:1) {
			my.remaining.imps = imps[a]-sum(tot[a,])
			opp.remaining.above = sum(tot[,s-1])-sum(tot[a,])
			if(s==1) opp.remaining.above = Inf
			tot[a,s] = max( min(my.remaining.imps, opp.remaining.above), 0 )
			#print(paste(a, s, my.remaining.imps, opp.remaining.above, tot[a,s]))
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


plot(as.factor(ip$num.participants), ip$abs.err, main="Num participants vs. runtime", xlab="Num participants", ylab="log10(seconds+1)")

#plot(as.factor(cjc$num.participants), log10(cjc$seconds+1), main="Num participants vs. runtime", xlab="Num participants", ylab="log10(seconds+1)", ylim=c(0,.05))
#hist(cjc$num.participants)

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



