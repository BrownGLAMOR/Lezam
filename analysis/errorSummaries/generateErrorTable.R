#Read in files to create a latex table
library(xtable)

A1 = c("exact", "sampled")
A2 = c("uniform", "historical")
A3 = c("CP", "MIP", "MIP_LP")
A4 = c("ie", "rie")
ENTRIES.PER.RESULT = 9
IDX.TO.REMOVE.FROM.IE = c(8,9)
IDX.TO.REMOVE.FROM.RIE = c(9)
BASE.DIR="/Users/sodomka/mydocs/workspace/AA-new2/analysis/errorSummaries/data/"
AGENT="Schlemazl"

ROUNDING = rep(0, ENTRIES.PER.RESULT)
ROUNDING[c(2, 4, 6)] = 2
ROUNDING[(ENTRIES.PER.RESULT-1):ENTRIES.PER.RESULT] = 2
ROUNDING[ENTRIES.PER.RESULT-2] = 1

TIME.IDX = 4
NUM.GAMES = 10

rename = function(a3) {
	new.name = a3
	if (a3 == "MIP") new.name = "IP"
	if (a3 == "MIP_LP") new.name = "MIP"
	new.name
}


vals = NULL
for (a1 in A1) {
	for (a2 in A2) {
		for (a3 in A3) {
			vals.row = NULL
			for (a4 in A4) {
				fname = paste(AGENT, a1, a2, a3, a4, "txt", sep=".")
				fname = paste(BASE.DIR, fname, sep="")
				result = try( read.table(fname) )
				if (class(result) == "try-error") {
					#Give default 0 vals
					result = as.data.frame(t(as.matrix(rep(0, ENTRIES.PER.RESULT))))
				}
				
				#DIVIDE TIME ENTRY BY 10 (#games)
				#TODO: This should be done when the data is generated, not the tables.
				result[TIME.IDX] = result[TIME.IDX]/NUM.GAMES
				
				#round values
				for (idx in 1:length(result)) {
					if (!is.na(result[idx])) {
						result[idx] = round(result[idx], ROUNDING[idx])
					}
				}
				
				if (a4 == "ie") {
					result = result[-IDX.TO.REMOVE.FROM.IE]
					result = cbind(rename(a3), as.matrix(result))
				}
				if (a4 == "rie") {
					result = result[-IDX.TO.REMOVE.FROM.RIE]
				}
				vals.row = cbind(vals.row, as.matrix(result))			
			}
			vals = rbind(vals, vals.row)
		}
	}
}



#Collapse mean and se into a single column
#Actually, just put parens around se
se.cols = c(3, 5, 7, 10, 12, 14)
for (col in se.cols) {
	vals[,col] = paste("(",vals[,col],")", sep="")
	vals[,col-1] = paste(vals[,col-1], vals[,col])
}
vals = vals[,-se.cols]





LATEX.OUTPUT.DIR = "/Users/sodomka/mydocs/workspace/AA-new2/analysis/errorSummaries/latex/"


rvals = matrix("", nrow(vals), 2)
grp = nrow(vals)/4
rvals[1 + 0*grp,] = c( paste("\\multirow{", 2*grp, "}{*}{Exact}", sep=""), paste("\\multirow{", grp, "}{*}{Uniform}", sep=""))
rvals[1 + 1*grp,] = c("", paste("\\multirow{", grp, "}{*}{Historical}", sep=""))
rvals[1 + 2*grp,] = c( paste("\\multirow{", 2*grp, "}{*}{Sampled}", sep=""), paste("\\multirow{", grp, "}{*}{Uniform}", sep=""))
rvals[1 + 3*grp,] = c("", paste("\\multirow{", grp, "}{*}{Historical}", sep=""))
latex.vals = cbind(rvals, vals)
latex.mat = xtable(latex.vals)

caption(latex.mat) = "IE and RIE results."
digits(latex.mat) = rep(1, ncol(latex.vals)+1)

hlines = c(-1,0,nrow(latex.vals), 2*grp)
align(latex.mat) = "|l|c|c|r||c|c|c|c||c|c|c|c|c|"

header1 = "Average & Impressions & Algorithm & \\multicolumn{4}{c||}{IE} & \\multicolumn{5}{c|}{RIE} \\\\ \\cline{4-12} "
header2 = "Position & Priors & & $\\varepsilon(I^a_s)$ & $\\varepsilon(I^a)$ & $\\varepsilon(I)$ & time & $\\varepsilon(I^a_s)$ & $\\varepsilon(I^a)$ & $\\varepsilon(I)$ & time & rank\\\\"
header = paste(header1, header2, sep="")

cline = "\\cline{2-12}"

#header.sub = "mean & med & max & \\%$C$ & \\%$B$"
#header.multi = "& \\multicolumn{5}{c|}{Total Imps} & \\multicolumn{5}{c|}{Per Agent Imps} & \\multicolumn{5}{c|}{Per Slot Imps} & Time & \\multicolumn{3}{c|}{Rank Error} \\\\"
#header=paste(header.multi, " & ", header.sub, " & ", header.sub, " & ", header.sub, " & ", "secs &  \\%C & d1 & d2 \\\\", sep="")
		
#print(latex.mat, file=paste(LATEX.OUTPUT.DIR,"tables.tex",sep=""), hline.after=hlines, append=TRUE, include.rownames=FALSE, include.colnames=FALSE, floating=TRUE, add.to.row=list(pos=list(0), command=c(header)))
print(latex.mat, hline.after=hlines, include.rownames=FALSE, include.colnames=FALSE, floating=TRUE, add.to.row=list(pos=list(0, grp, 3*grp), command=c(header, cline, cline)), sanitize.text.function=function(x){x})




























