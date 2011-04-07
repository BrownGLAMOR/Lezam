
xvals = seq(-2, 2, .01)
yvals = seq(-2, 2, .01)

zvals = matrix(NA, nrow=length(xvals), ncol=length(yvals))
qvals = matrix(NA, nrow=length(xvals), ncol=length(yvals))

xmean=0
ymean=0
xstd = 2
ystd = 1
for (xIdx in 1:length(xvals)) {
	for (yIdx in 1:length(yvals)) {
		x=xvals[xIdx]
		y=yvals[yIdx]
		zvals[xIdx,yIdx] = dnorm(x, mean=xmean, sd=xstd) * dnorm(y, mean=ymean, sd=ystd)
		qvals[xIdx,yIdx] = abs((x-xmean)/xstd) + abs((y-ymean)/ystd)
	}
}
