package models.queryanalyzer.riep.iep.mip;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;

import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.riep.iep.IEResult;

public class ImpressionEstimatorSimpleMIPExact extends AbstractImpressionEstimatorSimpleMIP {
	protected final static boolean SUPPRESS_OUTPUT = true;
	
	public QAInstanceExact _instExact;
	
	public ImpressionEstimatorSimpleMIPExact(QAInstanceExact inst){
		super(inst);
		_instExact = inst;
	}

	@Override
	public String getName() {return "SimpleMIPExact";}

	
	
   
	
}
