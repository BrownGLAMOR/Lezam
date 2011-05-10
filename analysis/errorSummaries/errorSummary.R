#This gets results for our final table
#USAGE:
# R CMD BATCH --no-save --no-restore "--args GAMES.TO.FILTER=15128 FILE=in.txt" errorSummary.R out.txt



############# CONFIG ##############
dir = "~/Desktop/" #"/home/sodomka/workspace/trunk/" #"~/Desktop/" #
FILE = "CP.rie.exact.noPrior.finals2010.games-15127-15136.days-0-57.queries-0-15.agents-all.txt" #"null" 
OUTFILE = "out.txt"
MIN.GAME.TO.FILTER = 15127
MAX.GAME.TO.FILTER = 15136
AGENTS.TO.FILTER = c("Schlemazl")
NUM.SLOTS = 5


######### OVERRIDE CONFIG
##First read in the arguments listed at the command line
# Read in command line args
args=(commandArgs(TRUE))
if (length(args) > 0) {
for (i in 1:length(args)) { eval(parse(text=args[[i]])) }
}

## INFERRED BY CONFIG
filename = paste(dir, FILE, sep='')

print(FILE)
print(filename)


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

	data
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




#Input: vector v
#Output: (mean, median, min, max)
quantiles.and.mean = function(v) {
	pct.zero = sum(v==0) / length(v)
	#stats = c(mean(v), median(v), min(v), max(v), pct.zero)
	#names(stats) = c("mean", "median", "min", "max", "pct.zero")
	
	#stats = c(mean(v), pct.zero, length(v), quantile(v))
	#names(stats) = c("mean", "pct.zero", "N", names(quantile(v)))
	
	#stats = c(mean(v), length(v))
	#names(stats) = c("mean", "N")
	
	#stats = c(mean(v), sd(v))
	#names(stats) = c("mean", "sd")
	
	
	stats = c(mean(v), sd(v)/sqrt(length(v)))
	names(stats) = c("mean", "se")
	
	stats
}



############### MAIN METHOD ##################



I.a = create.table(filename)
#do some filtering

I.a = subset(I.a, I.a$game.idx >= MIN.GAME.TO.FILTER & I.a$game.idx <= MAX.GAME.TO.FILTER & I.a$our.agent %in% AGENTS.TO.FILTER)
I.a.s = compute.slot.prediction.error(I.a) #Are these sorted?
I = compute.I(I.a.s)

#Get times
times = I.a$seconds[I.a$opp.bid.rank==1]

#Print statistics of error values
I.a.s.stats = quantiles.and.mean(I.a.s$abs.err[I.a.s$opp.bid.rank >= I.a.s$slot]) #Only look at slot predictions that were non-obvious (e.g. 1st slot agent can never be in 2nd slot)
names(I.a.s.stats) = paste("Ias.", names(I.a.s.stats), sep="")

I.a.stats = quantiles.and.mean(I.a$abs.err)
names(I.a.stats) = paste("Ia.", names(I.a.stats), sep="")

I.stats = quantiles.and.mean(I$abs.err)
names(I.stats) = paste("I.", names(I.stats), sep="")

time.stats = quantiles.and.mean(times)
names(time.stats) = paste("t.", names(time.stats), sep="")

total.time = sum(times)
names(total.time) = "total.time"


pct.perfect.rank = sum(I.a$perfect.ranking[I.a$opp.bid.rank==1]) / sum(I.a$opp.bid.rank==1) #Only look at 1st slot (since other values will be identical)
names(pct.perfect.rank) = "pct.rank"

avg.rank.dist = mean(I.a$rank.abs.err)
names(avg.rank.dist) = "rank.dist"

imperfect.rows = which(I.a$perfect.ranking == FALSE)
imperfect.avg.rank.dist = mean(I.a$rank.abs.err[imperfect.rows])
names(imperfect.avg.rank.dist) = "rank.dist.when.wrong"

stats = c(I.a.s.stats, I.a.stats, I.stats, total.time, pct.perfect.rank, imperfect.avg.rank.dist)




#Output a file of the name that generateErrorTable.R can easily read.
#A1 = c("exact", "sampled")
#A2 = c("uniform", "historical")
#A3 = c("CP", "MIP")
#A4 = c("ie", "rie")

fsplit = strsplit(FILE, '\\.')
a1 = fsplit[[1]][3]
prior = fsplit[[1]][4]
a3 = fsplit[[1]][1]
a4 = fsplit[[1]][2]
if(substr(prior, 1, 2) == "hi") {
	a2 = "historical"
}
if(substr(prior, 1, 2) == "no") {
	a2 = "uniform"
}

OUTFILE = paste(AGENTS.TO.FILTER, a1, a2, a3, a4, "txt", sep=".")
write.table(t(as.data.frame(stats)), file=OUTFILE)

#stats2 = read.table("stats")

