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
import models.queryanalyzer.ds.QAInstanceSampled;
import models.queryanalyzer.riep.iep.IEResult;

public class ImpressionEstimatorSimpleMIPSampled extends AbstractImpressionEstimatorSimpleMIP {
	protected final static boolean SUPPRESS_OUTPUT = true;
	
	public QAInstanceSampled _instSampled;
	
	public ImpressionEstimatorSimpleMIPSampled(QAInstanceSampled inst){
		super(inst);
		_instSampled = inst;
	}

	@Override
	public String getName() {return "SimpleMIPSampled";}

	
	
   
	
}
