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
numDays<-10
      baseDir<-"/Users/jordanberg/Documents/workspace/Clients/RData/";
      allAgentActual = vector();
      allAgentPredictions = vector();
      for(each.agent in dir(baseDir)) {
        baseFile<-paste(baseDir, each.agent,"/", sep="");
        allActual = vector();
        allPredictions = vector();
        for(each.dir in dir(baseFile)) {
        #par(mfrow=c(4,4));
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
            #plot(actual,type='l');
            #lines(predictions,col="red");
            
            allActual = c(allActual, actual);
            allPredictions = c(allPredictions, predictions);
          }
        }
        allMeanAct = mean(allActual);
        allSST  = sum((allActual - allMeanAct)^2);
        allSSE = sum((allPredictions - allActual)^2);
        allMSE = allSSE/length(allActual);
        allRMSE = sqrt(allMSE);
        allMAE = 1/length(allActual) * sum(abs(allPredictions - allActual));
        allR2 = 1 - allSSE/allSST;
        print(c('RMSE ', each.agent, allRMSE));
        print(c('MAE ', each.agent, allMAE));
        print(c('R2 ', each.agent, allR2));
        print('')
        
        allAgentActual = c(allAgentActual, allActual);
        allAgentPredictions = c(allAgentPredictions, allPredictions);
      }
      allAgentMeanAct = mean(allAgentActual);
      allAgentSST  = sum((allAgentActual - allMeanAct)^2);
      allAgentSSE = sum((allAgentPredictions - allAgentActual)^2);
      allAgentMSE = allAgentSSE/length(allAgentActual);
      allAgentRMSE = sqrt(allAgentMSE);
      allAgentMAE = 1/length(allAgentActual) * sum(abs(allAgentPredictions - allAgentActual));
      allAgentR2 = 1 - allAgentSSE/allAgentSST;
      print(c('TOTAL RMSE ', allAgentRMSE));
      print(c('TOTAL MAE ', allAgentMAE));
      print(c('TOTAL R2 ', allAgentR2));
      print('');