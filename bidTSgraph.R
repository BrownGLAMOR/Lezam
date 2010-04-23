library(stats)
#TACAA <- read.table("/Users/jordanberg/Documents/workspace/Clients/RData/Rdata1452TacTexaudioflat.data", header=TRUE)
#TACAA <- read.table("/Users/jordanberg/Documents/workspace/Clients/RData/Rdata1452TacTexaudionull.data", header=TRUE)

#bids <-ts(TACAA$bid,start=1,freq=5)
#-------------------------------------------------------------------------------------------------
#plot(stl(bids,s.window="periodic"))
#-------------------------------------------------------------------------------------------------
#bids.hw <- HoltWinters(bids)
#plot(bids.hw)
#-------------------------------------------------------------------------------------------------
#plot(bids,xlim=c(1,15))
#lines(predict(bids.hw,n.ahead=15),col=2)
#-------------------------------------------------------------------------------------------------
#bids.arfit <- arima(bids,order=c(2,1,2))
#tsdiag(bids.arfit)
#-------------------------------------------------------------------------------------------------
#plot(bids,xlim=c(1,15))
#bids.pred = predict(bids.arfit,n.ahead=15)
#lines(bids.pred$pred,col="red")
#lines(bids.pred$pred+2*bids.pred$se,col="red",lty=3)
#lines(bids.pred$pred-2*bids.pred$se,col="red",lty=3)
#-------------------------------------------------------------------------------------------------
AR <- 2;
I <- 1;
MA <- 1;
#"FINAL" "2"     "1"     "1"    
#"FINAL" "2"     "0"     "0"    
#"FINAL" "4"     "1"     "1"    
#"FINAL" "3"     "1"     "1"    
#"FINAL" "3"     "0"     "0" 
#"FINAL" "2"     "1"     "2"
#"FINAL" "4"     "1"     "0"   
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/aston/";
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/epfl/";
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/MetroClick/";
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/munsey/";
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/quakTAC/";
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/Schlemazl/";
#baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/TacTex/";
baseFile <-"/Users/jordanberg/Documents/workspace/Clients/RData/UMTac/";
numDays<-20
for(each.dir in dir(baseFile)) {
  par(mfrow=c(4,4),title=each.dir);
  newBaseFile<-paste(baseFile,each.dir,"/", sep="");
  files<-list.files(newBaseFile);
  for(each.file in files) {
    TACAA <- read.table(paste(newBaseFile ,each.file, sep=""), header=TRUE)
    bids <-ts(TACAA$bid,start=1,freq=5);
    actual = vector();
    predictions = vector();
    
    for (day in 11:(length(bids)-2)) {
      predictions2 <- bids[day];
      tryCatch(
               {
                 TACAAFit <- arima(bids[max(1,day-numDays):day], order = c(AR,I,MA));
                 predictions2 = predict(TACAAFit, n.ahead = 1)$pred;
                 predictions2[is.na(predictions2)] <- bids[day];
                 predictions2[predictions2 < 0] <- 0;
               },
               interrupt=function(err)
               { 
                 flag <- 1;
               },
               error=function(err)
               {
                 flag <- 1;
               }
               );
      actual2 = bids[day+1];
      
      actual = c(actual, actual2);
      predictions = c(predictions,predictions2);
    }
    #meanAct = mean(actual);
    #SST  = sum((actual - meanAct)^2);
    #SSE = sum((predictions - actual)^2);
    #MSE = SSE/length(actual);
    #RMSE = sqrt(MSE);
    #MAE = 1/length(actual) * sum(abs(predictions - actual));
    #R2 = 1 - SSE/SST;
    #print(c('RMSE', RMSE));
    #print(c('MAE', MAE));
    #print(c('R2', R2));
    plot(actual,type='l',xlab='Time',ylab='Bid');
    lines(predictions,col="red");
    title(main=each.file)
  }
  break;
}