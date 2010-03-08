library(tree)
library(randomForest)
library(MCMCpack)
library(MSBVAR)
library(gbm)
library(dyn)
actual = vector()
predictions = vector();
TACAA <- read.table("/Users/jordanberg/Documents/workspace/Clients/Rdata.data", header=TRUE)
TACAA$bid = ts(TACAA$bid)
TACAA$cpc = ts(TACAA$cpc)
TACAA$prclick = ts(TACAA$prclick)
TACAA$pos = ts(TACAA$pos)
TACAA$prconv = ts(TACAA$prconv)
for (i in 4:((length(TACAA$pos)-2*16)/16)) {
	minIdx = (i-20)*16;
	if (minIdx < 1) minIdx = 1
	
	bid2pos <- randomForest(pos ~ query/bid,data=TACAA[minIdx:(i*16),],ntree=50,importance=TRUE)
	
	#TACAAFit <- lm(prclick ~ poly(bid,1),data=TACAA[minIdx:(i*16),])
	#TACAAFit <- lm(prclick ~ poly(bid,2),data=TACAA[minIdx:(i*16),])
	#TACAAFit <- lm(prclick ~ poly(bid,1)+query,data=TACAA[minIdx:(i*16),])
	#TACAAFit <- lm(prclick ~ poly(bid,2)+query,data=TACAA[minIdx:(i*16),])
	#TACAAFit <- lm(prclick ~ query/poly(bid,1),data=TACAA[minIdx:(i*16),])
	#TACAAFit <- lm(prclick ~ query/(poly(bid,2)),data=TACAA[minIdx:(i*16),])
	#TACAAFit <- loess(prclick ~ bid,data=TACAA[minIdx:(i*16),], span=.1)
	#TACAAFit <- loess(prclick ~ bid,data=TACAA[minIdx:(i*16),], span=.2)
	#TACAAFit <- loess(prclick ~ bid,data=TACAA[minIdx:(i*16),], span=.3)
	#TACAAFit <- loess(prclick ~ bid,data=TACAA[minIdx:(i*16),], span=.4)
	#TACAAFit <- loess(prclick ~ bid,data=TACAA[minIdx:(i*16),], span=.5)
	#TACAAFit <- loess(prclick ~ bid,data=TACAA[minIdx:(i*16),])
	#TACAAFit <- tree(prclick ~ bid,data=TACAA[minIdx:(i*16),])
	#TACAAFit <- tree(prclick ~ bid+query,data=TACAA[minIdx:(i*16),])
	#TACAAFit <- randomForest(prclick ~ bid,data=TACAA[minIdx:(i*16),],ntree=50,importance=TRUE)
	#TACAAFit <- randomForest(prclick ~ bid+query,data=TACAA[minIdx:(i*16),],ntree=50,importance=TRUE)
	TACAAFit <- randomForest(prclick ~ query/(bid),data=TACAA[minIdx:(i*16),],ntree=50,importance=TRUE)
	#TACAAFit <- gbm(prclick ~ bid,data=TACAA[minIdx:(i*16),],distribution='gaussian',var.monotone=c(1),n.trees=4000,verbos=FALSE)
	#TACAAFit <- gbm(prclick ~ bid+query,data=TACAA[minIdx:(i*16),],distribution='gaussian',var.monotone=c(1,0),n.trees=4000,verbos=FALSE)
	#TACAAFit <- gbm(prclick ~ query/bid,data=TACAA[minIdx:(i*16),],distribution='gaussian',var.monotone=c(1,0),n.trees=4000,verbos=FALSE)
	#TACAAFit <- arima0(TACAA$prclick[minIdx:(i*16)], xreg = cbind(TACAA$bid[minIdx:(i*16)],TACAA$query[minIdx:(i*16)]), order = c(1,1,1));
	actual2 = TACAA[((i+1)*16):((i+2)*16),];
	
	predictions2pos = predict(bid2pos, actual2)
	actual2$pos = predictions2pos;
	
	predictions2 = predict(TACAAFit, actual2)
	#predictions2 = predict(TACAAFit, actual2, n.trees = 4000)
	#predictions2 = predict(TACAAFit, n.ahead = 1, cbind(actual2$bid,actual2$query))$pred
	
	actualDV = actual2$prclick;
	actualIV = actual2$bid;
	
	predictions2[is.na(predictions2)] <- 0
	predictions2[predictions2 < 0] <- 0
	
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