
a1 = "exact"
a2 = "uniform"
a3 = "CP"
a4 = "ie"
BASE.DIR="/Users/sodomka/mydocs/workspace/AA-new2/analysis/errorSummaries/data/byAgent/"
AGENTS = c("crocodileagent", "TacTex", "tau", "MetroClick", "Schlemazl", "McCon", "Mertacor")

vals = NULL
for (agent in AGENTS) {
	fname = paste(agent, a1, a2, a3, a4, "txt", sep=".")
	fname = paste(BASE.DIR, fname, sep="")
	result = try( read.table(fname) )
	if (class(result) == "try-error") {
		#Give default 0 vals
		result = as.data.frame(t(as.matrix(rep(0, ENTRIES.PER.RESULT))))
	}
	rownames(result) = agent
	vals = rbind(vals, result)
}

vals = vals[,1:3]
latex.mat = xtable(vals)
caption(latex.mat) = paste("Results for the", a4, "problem with", a2, "priors, by agent, using the", a3, "algorithm.")
digits(latex.mat) = 0
print(latex.mat)