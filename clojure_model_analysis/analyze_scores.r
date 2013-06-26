manufacturers <- c("flat","lioneer","pg","null")
products <- c("audio","dvd","tv","null")
indices <- c("bids","budgets")
data <- c("cost","numClicks","weights")

#INPUT: a manufacturer string and a product string
#OUTPUT: the string which heads the data files for a particular query
makeQueryHeader <- function(manufacturer,product) {
	paste("(Query (",manufacturer,",",product,"))",sep="")
}

#INPUT: a string representing a line from a file
#OUTPUT: the appropriate corresponding numeric vector
processRow <- function(str) {
	unlist(lapply(strsplit(str,","),as.numeric))
}

threeDPlot <- function(bidVec,budgetVec,datMatrix,title,subtitle,save=FALSE) {
	if (save) {
		jpeg(filename = paste("pictures/bids_and_budgets/",title," ",subtitle,".jpg",sep=""))
		persp(bidVec,budgetVec,datMatrix,main=title,sub=subtitle,ticktype="detailed",theta=315)
		dev.off()
	} else {
		persp(bidVec,budgetVec,datMatrix,main=title,sub=subtitle,ticktype="detailed",theta=315)
	}
}

#here, axis is either t or f, representing the dimension to be marginalized over
twoDPlot <- function(indexVec,datMatrix,title,subtitle,axis,save=FALSE) {
	marginalVec <- if (axis) {rowSums(datMatrix)} else {colSums(datMatrix)}
	dir = if (axis) {"bids"} else {"budgets"}
	if (save) {
		jpeg(filename = paste("pictures/",dir,"/",title," ",subtitle,".jpg",sep=""))
		plot(indexVec,marginalVec,main=title,sub=subtitle,type="l")
		dev.off()
	} else {
		plot(indexVec,marginalVec,main=title,sub=subtitle,type="l")
	}
}

#INPUT: a manufacturer, a product, and a string representing a dataset
#(one of the elements in the above vector "data")
#ALSO: an optional argument margin which will output a marginal graph of average 
#weights over the axis "bids" or "budgets" (otherwise the plot will be 3d).
#OUTPUT: a chart of the requested type which changes over the 60 (+ one "zero"
#day) period.
makeChartAnimation <- function(manufacturer, product, dat, margin = "",save=FALSE) {
	queryHeader <- makeQueryHeader(manufacturer,product)
	if (!save) {
		plot.new()
		devAskNewPage(TRUE)
	}
	for (i in 0:58) {
		inputFile <- paste(queryHeader,i,".0","_",dat,".dat",sep="")
		con <- file(inputFile, open = "r")
		lineNum <- 1
		bidVec <- c()
		budgetVec <- c()
		lines <- readLines(con, encoding = "UTF-8")
		for (j in 1:2) {
			if (j == 1) {
				bidVec <- processRow(lines[j]);
				next
			}
			if (j == 2) {
				budgetVec <- processRow(lines[j]);
				next
			}
			j <- j + 1
		}
		print(length(bidVec))
		print(length(budgetVec))
		datMatrix <- mat.or.vec(length(bidVec),length(budgetVec))
		for (k in 4:length(lines)) {
			datMatrix[k-3, ] <- processRow(lines[k])
		}
		title <- paste("Query",manufacturer,product)
		subtitle <- paste(dat," Day ",i ,sep = "")
		switch(margin,
			bids=twoDPlot(bidVec,datMatrix,title,subtitle,TRUE,save),
			budgets=twoDPlot(budgetVec,datMatrix,title,subtitle,FALSE,save),
			threeDPlot(bidVec,budgetVec,datMatrix,title,subtitle,save))
	}
	
}
