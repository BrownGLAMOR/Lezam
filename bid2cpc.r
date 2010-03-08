library(tree)
library(randomForest)
library(MCMCpack)
library(MSBVAR)
library(gbm)
library(dynlm)
actual = vector()
predictions = vector();
TACAA <- read.table("/Users/jordanberg/Documents/workspace/Clients/Rdata.data", header=TRUE)
TACAA$bid = ts(TACAA$bid)
TACAA$cpc = ts(TACAA$cpc)
TACAA$prclick = ts(TACAA$prclick)
TACAA$pos = ts(TACAA$pos)
TACAA$prconv = ts(TACAA$prconv)
for (i in 4:((length(TACAA$bid)-2*16)/16)) {
	#TACAAFit <- lm(cpc ~ poly(bid,1),data=TACAA[1:(i*16),])
	#TACAAFit <- lm(cpc ~ poly(bid,2),data=TACAA[1:(i*16),])
	#TACAAFit <- lm(cpc ~ poly(bid,3),data=TACAA[1:(i*16),])
	#TACAAFit <- lm(cpc ~ bid+query,data=TACAA[1:(i*16),])
	#TACAAFit <- lm(cpc ~ query/bid,data=TACAA[1:(i*16),])
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),], span=.1)
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),], span=.2)
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),], span=.3)
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),], span=.4)
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),], span=.5)
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),])
	#TACAAFit <- tree(cpc ~ bid,data=TACAA[1:(i*16),])
	#TACAAFit <- tree(cpc ~ bid+query,data=TACAA[1:(i*16),])
	#TACAAFit <- randomForest(cpc ~ bid,data=TACAA[1:(i*16),],ntree=50,importance=TRUE)
	#TACAAFit <- randomForest(cpc ~ bid+query,data=TACAA[1:(i*16),],ntree=50,importance=TRUE)
	#TACAAFit <- randomForest(cpc ~ query/bid,data=TACAA[1:(i*16),],ntree=50,importance=TRUE)
	#TACAAFit <- gbm(cpc ~ bid,data=TACAA[1:(i*16),],distribution='gaussian',var.monotone=c(1),n.trees=4000,verbos=FALSE)
	#TACAAFit <- gbm(cpc ~ bid+query,data=TACAA[1:(i*16),],distribution='gaussian',var.monotone=c(1,0),n.trees=4000,verbos=FALSE)
	#TACAAFit <- gbm(cpc ~ query/bid,data=TACAA[1:(i*16),],distribution='gaussian',var.monotone=c(1,0),n.trees=4000,verbos=FALSE)
	TACAAFit <- arima0(TACAA$cpc[1:(i*16)], xreg = cbind(TACAA$bid[1:(i*16)],TACAA$query[1:(i*16)]), order = c(2,0,2));
	actual2 = TACAA[((i+1)*16):((i+2)*16),];
	#predictions2 = predict(TACAAFit, actual2)
	#predictions2 = predict(TACAAFit, actual2, n.trees = 4000)
	predictions2 = predict(TACAAFit, n.ahead = 1, cbind(actual2$bid,actual2$query))$pred
	
	actualDV = actual2$cpc;
	actualIV = actual2$bid;
	
	predictions2[is.na(predictions2)] <- 0
	predictions2[predictions2 < 0] <- 0
	predictions2[predictions2 > actualIV] <- actualIV[predictions2 > actualIV]
	
	actual = c(actual,actualDV)
	predictions = c(predictions,predictions2)
}
meanAct = mean(actual)
SST  = sum((actual - meanAct)^2)
SSE = sum((predictions - actual)^2)
MSE = SSE/length(actual)
RMSE = sqrt(MSE)
MAE = 1/length(actual) * sum(abs(predictions - actual))
R2 = 1 - SSE/SST
print(c('RMSE', RMSE))
print(c('MAE', MAE))
print(c('R2', R2))