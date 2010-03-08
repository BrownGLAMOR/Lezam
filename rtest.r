actual = vector()
predictions = vector();
TACAA <- read.table("/Users/jordanberg/Documents/workspace/Clients/Rdata.data", header=TRUE)
for (i in 4:((length(TACAA$pos)-2*16)/16)) {
	TACAAFit <- lm(cpc ~ poly(bid,1),data=TACAA[1:(i*16),])
	TACAAFit <- lm(cpc ~ poly(bid,2),data=TACAA[1:(i*16),])
	TACAAFit <- lm(cpc ~ poly(bid,3),data=TACAA[1:(i*16),])
	#TACAAFit <- lm(cpc ~ bid+query,data=TACAA[1:(i*16),])
	#TACAAFit <- lm(cpc ~ query/bid,data=TACAA[1:(i*16),])
	#TACAAFit <- loess(cpc ~ bid,data=TACAA[1:(i*16),])
	actual2 = TACAA[((i+1)*16):((i+2)*16),];
	predictions2 = predict(TACAAFit, actual2)
	
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