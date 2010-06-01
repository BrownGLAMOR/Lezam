package simulator;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.rosuda.REngine.Rserve.RConnection;

import models.AbstractModel;
import models.avgpostoposdist.AvgPosToPosDist;
import models.bidtocpc.AbstractBidToCPC;
import models.bidtocpc.RegressionBidToCPC;
import models.bidtopos.AbstractBidToPos;
import models.bidtoprclick.AbstractBidToPrClick;
import models.postobid.AbstractPosToBid;
import models.postocpc.AbstractPosToCPC;
import models.postoprclick.AbstractPosToPrClick;
import models.prconv.AbstractConversionModel;
import models.prconv.GoodConversionPrModel;
import models.prconv.HistoricPrConversionModel;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.BasicQueryToNumImp;
import models.targeting.BasicTargetModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.usermodel.AbstractUserModel;
import models.usermodel.BasicUserModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import se.sics.tasim.aw.Message;
import simulator.models.PerfectBidToCPC;
import simulator.models.PerfectBidToPos;
import simulator.models.PerfectBidToPrClick;
import simulator.models.PerfectPosToBid;
import simulator.models.PerfectPosToCPC;
import simulator.models.PerfectPosToPrClick;
import simulator.models.PerfectQueryToPrConv;
import simulator.models.PerfectQueryToNumImp;
import simulator.models.PerfectUnitsSoldModel;
import simulator.models.PerfectUserModel;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import agents.AbstractAgent;
import agents.modelbased.DrMCKPBid;
import agents.modelbased.DynamicMCKP;
import agents.modelbased.ExoMCKPBid;
import agents.modelbased.ExoMCKPBidExhaustive;
import agents.modelbased.G4;
import agents.modelbased.HardMCKPBid;
import agents.modelbased.ILPBidAgent;
import agents.modelbased.ILPBidSearchAgent;
import agents.modelbased.ILPPosAgent;
import agents.modelbased.NewSemiEndoMCKPBid;
import agents.modelbased.SemiEndoMCKPBid;
import agents.modelbased.MCKPBidDynProg;
import agents.modelbased.MCKPBidNoDomElim;
import agents.modelbased.SemiEndoMCKPBidExhaustive;
import agents.modelbased.MCKPPos;
import agents.modelbased.MCKPPosSearch;
import agents.modelbased.simpleAA.SimpleAABidAgent;
import agents.olderagents.Cheap;
import agents.olderagents.ClickProfitC;
import agents.olderagents.ConstantPM;
import agents.olderagents.EquateProfitC;
import agents.olderagents.G3Agent;
import agents.olderagents.GoodSlot;
import agents.olderagents.PortfolioOpt;
import agents.rulebased.AdjustPM;
import agents.rulebased.AdjustPPS;
import agents.rulebased.AdjustPR;
import agents.rulebased.AdjustROI;
import agents.rulebased.EquatePM;
import agents.rulebased.EquatePPS;
import agents.rulebased.EquatePR;
import agents.rulebased.EquateROI;
import agents.rulebased.simple.EquatePMSimple;
import agents.rulebased.simple.EquatePPSSimple;
import agents.rulebased.simple.EquatePRSimple;
import agents.rulebased.simple.EquateROISimple;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.ReserveInfo;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import edu.umich.eecs.tac.props.UserClickModel;

/**
 *
 * @author jberg
 * 
 */
public class BasicSimulator {

	private int NUM_PERF_ITERS = 1000;
	private int _numSplits = 3; //How many bids to consider between slots
	private boolean PERFECTMODELS = false;
	private boolean SOMEREGMODELS = true;
	private boolean _killBudgets = false;
	private boolean is2009 = true;

	/*
	 * Gaussian noise for perf models
	 */
	private boolean _noise;
	private double _noisePrClick;
	private double _noiseImps;

	private boolean DEBUG = false;

	SecureRandom _R;					//Random number generator

	private double _LAMBDA = .995;

	private double _squashing;
	private int _numUsers = 90000;
	private int _numPromSlots;
	private int _numSlots;
	private double _regReserve;
	private double _proReserve;
	private double _targEffect;
	private double _promSlotBonus;
	private double _CSB;
	private double _MSB;
	private String _ourCompSpecialty;
	private String _ourManSpecialty;

	private String[] _agents;
	private HashMap<String,HashMap<Query,Ad>> _adType;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _bidsWithoutOurs;
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<String,String> _manSpecialties;
	private HashMap<String,String> _compSpecialties;
	/*
	 * Index 0: most recent day of Sales
	 * 
	 * Index CapacityWindow-1: Sales from the last day of the window...
	 */
	private HashMap<String,Integer[]> _salesOverWindow;
	private HashMap<String,Integer> _capacities;
	private HashMap<Query,Double> _contProb;
	private Set<Query> _querySpace;

	private HashMap<Query,Double> _ourAdvEffect;

	private RetailCatalog _retailCatalog;

	private HashMap<Product, HashMap<UserState, Integer>> _usersMap;

	private int _ourAdvIdx;

	private int _day;

	private GameStatus _status;

	private PublisherInfo _pubInfo;

	private ReserveInfo _reserveInfo;

	private SlotInfo _slotInfo;

	private UserClickModel _userClickModel;

	private AdvertiserInfo _ourAdvInfo;

	private HashMap<String, AdvertiserInfo> _advInfos;

	private HashMap<Query,HashMap<Double,LinkedList<Reports>>> _singleQueryReports;

	private long lastSeed = 68616;

	private ArrayList<SimUser> _pregenUsers;

	private BidBundle _ourBidBundle;

	public BasicSimulator() {
		try {
			_R = new SecureRandom().getInstance("SHA1PRNG");
			_R.setSeed(lastSeed);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public HashMap<String,LinkedList<Reports>> runFullSimulation(GameStatus status, AbstractAgent agent, int advertiseridx) {
		if(PERFECTMODELS) {
			System.out.println("****USING \"PERFECT\" MODELS***");
			System.out.println(" Num Iters: " + NUM_PERF_ITERS);
			System.out.println(" NumSplits: " + _numSplits);
			if(_killBudgets) {
				System.out.println(" Removing all budgets");
			}
			if(_noise) {
				System.out.println(" IMP VAR=" + _noiseImps + ",CLICKPR VAR=" + _noisePrClick + " GAUSSIAN ERROR");
			}
		}
		HashMap<String,LinkedList<Reports>> reportsListMap = new HashMap<String, LinkedList<Reports>>();
		initializeBasicInfo(status, advertiseridx);
		agent.sendSimMessage(new Message("doesn't","matter",_pubInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_retailCatalog));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_ourAdvInfo));
		agent.initBidder();
		Set<AbstractModel> models = agent.initModels();
		agent.setModels(models);
		for(int i = 0; i < _agents.length; i++) {
			LinkedList<Reports> reports = new LinkedList<Reports>();
			reportsListMap.put(_agents[i], reports);
		}
		int firstDay = 0;

		//		ArrayList<Double> impSE = new ArrayList<Double>();
		//		ArrayList<Double> impActual = new ArrayList<Double>();

		//		ArrayList<Double> bidToCPCSE = new ArrayList<Double>();
		//		ArrayList<Double> bidToCPCActual = new ArrayList<Double>();
		//
		//		ArrayList<Double> posToCPCSE = new ArrayList<Double>();
		//		ArrayList<Double> posToCPCActual = new ArrayList<Double>();

		//		ArrayList<Double> bidToPrClickSE = new ArrayList<Double>();
		//		ArrayList<Double> bidToPrClickActual = new ArrayList<Double>();

		//		ArrayList<Double> posToPrClickSE = new ArrayList<Double>();
		//		ArrayList<Double> posToPrClickActual = new ArrayList<Double>();

		//		ArrayList<Double> bidToPosSE = new ArrayList<Double>();
		//		ArrayList<Double> bidToPosActual = new ArrayList<Double>();
		//
		//		ArrayList<Double> posToBidSE = new ArrayList<Double>();
		//		ArrayList<Double> posToBidActual = new ArrayList<Double>();
		//
		//		ArrayList<Double> convPrSE = new ArrayList<Double>();
		//		ArrayList<Double> convPrActual = new ArrayList<Double>();

		double totalOverCapPerc = 0.0;
		double totalOverCapAbs = 0.0;

		System.out.println(_agents[_ourAdvIdx]);

		for(int day = firstDay; day < 59; day++) {
			double start = System.currentTimeMillis();
			/*
			 * ENSURE GARBAGE COLLECTOR IS RUN BETWEEN ITERATIONS
			 */
			System.gc(); emptyFunction(); emptyFunction(); emptyFunction(); emptyFunction();

			if(day >= 2) {
				LinkedList<Reports> reportsList = reportsListMap.get(_agents[advertiseridx]);
				/*
				 * Two day delay
				 */
				Reports reports = reportsList.get(day-2);
				SalesReport salesReport = reports.getSalesReport();
				QueryReport queryReport = reports.getQueryReport();
				agent.handleQueryReport(queryReport);
				agent.handleSalesReport(salesReport);
				if(!PERFECTMODELS || SOMEREGMODELS) {
					agent.updateModels(salesReport, queryReport);
				}
			}
			lastSeed = getNewSeed();
			initializeDaySpecificInfo(day, advertiseridx);
			agent.setDay(day);

			_pregenUsers = null;

			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			System.out.println("(" + day + ") time spent updating models  " + (elapsed / 1000) + " seconds");

			start = System.currentTimeMillis();

			HashMap<String, Reports> maps = runSimulation(agent);
			stop = System.currentTimeMillis();
			elapsed = stop - start;
			System.out.println("(" + day + ") time spent bidding  " + (elapsed / 1000) + " seconds");

			start = System.currentTimeMillis();

			//			/*
			//			 * TEST PERFECT MODEL ERROR
			//			 */
			//
			//			//			BidBundle bundle = status.getBidBundles().get(_agents[_ourAdvIdx]).get(day);
			//			//			//No Targeting or Budgets
			//			//			bundle.setCampaignDailySpendLimit(Double.NaN);
			//			//			for(Query query : _querySpace) {
			//			//				bundle.setAd(query, new Ad());
			//			//				bundle.setDailyLimit(query, Double.NaN);
			//			//			}
			//			//
			//			//			HashMap<String, Reports> maps = runSimulation(bundle);
			//			//
			//			//			_ourBidBundle = bundle;
			//			Set<AbstractModel> ourModels = agent.getModels();
			//
			//			Reports ourReports = maps.get(_agents[_ourAdvIdx]);
			//
			//			for(AbstractModel model : ourModels) {
			//				//				if(model instanceof AbstractUserModel) {
			//				//					//Do Nothing
			//				//				}
			//				if(model instanceof AbstractQueryToNumImp) {
			//					AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
			//					System.out.println(queryToNumImp.toString());
			//					for(Query query : _querySpace) {
			//						int numImps = (int) (ourReports.getRegularImpressions(query) + ourReports.getPromotedImpressions(query));
			//						int numImpsPred = queryToNumImp.getPredictionWithBid(query, _ourBidBundle.getBid(query),day+1);
			//						if(numImps == 0 || numImpsPred == 0) {
			//							continue;
			//						}
			//						else {
			//							double diff = numImps - numImpsPred;
			//							double SE = diff*diff;
			//							impSE.add(SE);
			//							impActual.add(numImps*1.0);
			//						}
			//					}
			//				}
			//				//				else if(model instanceof AbstractUnitsSoldModel) {
			//				//					//Do Nothing
			//				//				}
			//				//				else if(model instanceof AbstractBidToCPC) {
			//				//					AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
			//				//					for(Query query : _querySpace) {
			//				//						double CPC = ourReports.getCPC(query);
			//				//						double CPCPred = bidToCPC.getPrediction(query, _ourBidBundle.getBid(query));
			//				//						if(Double.isNaN(CPC) || Double.isNaN(CPCPred)) {
			//				//							continue;
			//				//						}
			//				//						else {
			//				//							double diff = CPC - CPCPred;
			//				//							double SE = diff*diff;
			//				//							bidToCPCSE.add(SE);
			//				//							bidToCPCActual.add(CPC);
			//				//						}
			//				//					}
			//				//				}
			//				else if(model instanceof AbstractBidToPrClick) {
			//					AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
			//					System.out.println(bidToPrClick.toString());
			//					for(Query query : _querySpace) {
			//						double clickPr = ourReports.getClickPr(query);
			//						double prClickPred = bidToPrClick.getPrediction(query, _ourBidBundle.getBid(query), _ourBidBundle.getAd(query));
			//						if(Double.isNaN(clickPr) || Double.isNaN(prClickPred)) {
			//							continue;
			//						}
			//						else {
			//							double diff = clickPr - prClickPred;
			//							double SE = diff*diff;
			//							bidToPrClickSE.add(SE);
			//							bidToPrClickActual.add(clickPr);
			//						}
			//					}
			//				}
			//				//				else if(model instanceof AbstractPosToCPC) {
			//				//					AbstractPosToCPC posToCPC = (AbstractPosToCPC) model;
			//				//					for(Query query : _querySpace) {
			//				//						double CPC = ourReports.getCPC(query);
			//				//						double CPCPred = posToCPC.getPrediction(query, ourReports.getPosition(query));
			//				//						if(Double.isNaN(CPC) || Double.isNaN(CPCPred)) {
			//				//							continue;
			//				//						}
			//				//						else {
			//				//							//							System.out.println(query);
			//				//							//							System.out.println("CPC: " + CPC + ", CPCPred: " + CPCPred);
			//				//							double diff = CPC - CPCPred;
			//				//							double SE = diff*diff;
			//				//							posToCPCSE.add(SE);
			//				//							posToCPCActual.add(CPC);
			//				//						}
			//				//					}
			//				//				}
			//				else if(model instanceof AbstractPosToPrClick) {
			//					AbstractPosToPrClick posToPrClick = (AbstractPosToPrClick) model;
			//					System.out.println(posToPrClick.toString());
			//					for(Query query : _querySpace) {
			//						double clickPr = ourReports.getClickPr(query);
			//						double prClickPred = posToPrClick.getPrediction(query, ourReports.getPosition(query), _ourBidBundle.getAd(query));
			//						if(Double.isNaN(clickPr) || Double.isNaN(prClickPred)) {
			//							continue;
			//						}
			//						else {
			//							double diff = clickPr - prClickPred;
			//							double SE = diff*diff;
			//							posToPrClickSE.add(SE);
			//							posToPrClickActual.add(clickPr);
			//						}
			//					}
			//				}
			//				//				else if(model instanceof AbstractBidToPos) {
			//				//					AbstractBidToPos bidToPos = (AbstractBidToPos) model;
			//				//					for(Query query : _querySpace) {
			//				//						double pos = ourReports.getPosition(query);
			//				//						double posPred = bidToPos.getPrediction(query,_ourBidBundle.getBid(query));
			//				//						if(Double.isNaN(pos) || Double.isNaN(posPred)) {
			//				//							continue;
			//				//						}
			//				//						else {
			//				//							double diff = pos - posPred;
			//				//							double SE = diff*diff;
			//				//							bidToPosSE.add(SE);
			//				//							bidToPosActual.add(pos*1.0);
			//				//						}
			//				//					}
			//				//				}
			//				//				else if(model instanceof AbstractPosToBid) {
			//				//					AbstractPosToBid posToBid = (AbstractPosToBid) model;
			//				//					for(Query query : _querySpace) {
			//				//						double bid = _ourBidBundle.getBid(query);
			//				//						double bidPred = posToBid.getPrediction(query,ourReports.getPosition(query));
			//				//						if(bid == 0 || bidPred == 0) {
			//				//							continue;
			//				//						}
			//				//						else {
			//				//							double diff = bid - bidPred;
			//				//							double SE = diff*diff;
			//				//							posToBidSE.add(SE);
			//				//							posToBidActual.add(bid*1.0);
			//				//						}
			//				//					}
			//				//				}
			//				//				else if(model instanceof AbstractConversionModel) {
			//				//					AbstractConversionModel convPrModel = (AbstractConversionModel) model;
			//				//					for(Query query : _querySpace) {
			//				//						double convPr = ourReports.getConvPr(query);
			//				//						//						double convPrPred = convPrModel.getPrediction(query);
			//				//						double convPrPred = convPrModel.getPredictionWithBid(query, _ourBidBundle.getBid(query));
			//				//						if(Double.isNaN(convPr) ||Double.isNaN(convPrPred)) {
			//				//							continue;
			//				//						}
			//				//						else {
			//				//							//							System.out.println(query + "Bid: " + _ourBidBundle.getBid(query));
			//				//							//							System.out.println("ConvPr: " + convPr + ", Pred: " + convPrPred);
			//				//							double diff = convPr - convPrPred;
			//				//							double SE = diff*diff;
			//				//							convPrSE.add(SE);
			//				//							convPrActual.add(convPr*1.0);
			//				//						}
			//				//					}
			//				//				}
			//				//				else if(model instanceof BasicTargetModel) {
			//				//					//Do nothing
			//				//				}
			//				//				else {
			//				//					throw new RuntimeException("Forgot error check on perfect model");
			//				//				}
			//			}
			//
			//
			//			double impErrorTot = 0.0;
			//			double impActualTot = 0.0;
			//			for (int i = 0; i < impSE.size(); i++) {
			//				impErrorTot += impSE.get(i);
			//				impActualTot += impActual.get(i);
			//			}
			//			impErrorTot /= impSE.size();
			//			impActualTot /= impActual.size();
			//			double impRMSE = Math.sqrt(impErrorTot);
			//			System.out.println("Impressions RMSE: " +  impRMSE + ", %error: " + impRMSE/impActualTot);
			//
			//			//			double bidToCPCErrorTot = 0.0;
			//			//			double bidToCPCActualTot = 0.0;
			//			//			for (int i = 0; i < bidToCPCSE.size(); i++) {
			//			//				bidToCPCErrorTot += bidToCPCSE.get(i);
			//			//				bidToCPCActualTot += bidToCPCActual.get(i);
			//			//			}
			//			//			bidToCPCErrorTot /= bidToCPCSE.size();
			//			//			bidToCPCActualTot /= bidToCPCActual.size();
			//			//			double bidToCPCRMSE = Math.sqrt(bidToCPCErrorTot);
			//			//			System.out.println("Bid-CPC RMSE: " +  bidToCPCRMSE + ", %error: " + bidToCPCRMSE/bidToCPCActualTot);
			//			//
			//			//			double posToCPCErrorTot = 0.0;
			//			//			double posToCPCActualTot = 0.0;
			//			//			for (int i = 0; i < posToCPCSE.size(); i++) {
			//			//				posToCPCErrorTot += posToCPCSE.get(i);
			//			//				posToCPCActualTot += posToCPCActual.get(i);
			//			//			}
			//			//			posToCPCErrorTot /= posToCPCSE.size();
			//			//			posToCPCActualTot /= posToCPCActual.size();
			//			//			double posToCPCRMSE = Math.sqrt(posToCPCErrorTot);
			//			//			System.out.println("Pos-CPC RMSE: " +  posToCPCRMSE + ", %error: " + posToCPCRMSE/posToCPCActualTot);
			//
			//			double bidToPrClickErrorTot = 0.0;
			//			double bidToPrClickActualTot = 0.0;
			//			for (int i = 0; i < bidToPrClickSE.size(); i++) {
			//				bidToPrClickErrorTot += bidToPrClickSE.get(i);
			//				bidToPrClickActualTot += bidToPrClickActual.get(i);
			//			}
			//			bidToPrClickErrorTot /= bidToPrClickSE.size();
			//			bidToPrClickActualTot /= bidToPrClickActual.size();
			//			double bidToPrClickRMSE = Math.sqrt(bidToPrClickErrorTot);
			//			System.out.println("Bid-PrClick RMSE: " +  bidToPrClickRMSE + ", %error: " + bidToPrClickRMSE/bidToPrClickActualTot);
			//
			//			double posToPrClickErrorTot = 0.0;
			//			double posToPrClickActualTot = 0.0;
			//			for (int i = 0; i < posToPrClickSE.size(); i++) {
			//				posToPrClickErrorTot += posToPrClickSE.get(i);
			//				posToPrClickActualTot += posToPrClickActual.get(i);
			//			}
			//			posToPrClickErrorTot /= posToPrClickSE.size();
			//			posToPrClickActualTot /= posToPrClickActual.size();
			//			double posToPrClickRMSE = Math.sqrt(posToPrClickErrorTot);
			//			System.out.println("Pos-PrClick RMSE: " +  posToPrClickRMSE + ", %error: " + posToPrClickRMSE/posToPrClickActualTot);
			//
			//
			//			//			double bidToPosErrorTot = 0.0;
			//			//			double bidToPosActualTot = 0.0;
			//			//			for (int i = 0; i < bidToPosSE.size(); i++) {
			//			//				bidToPosErrorTot += bidToPosSE.get(i);
			//			//				bidToPosActualTot += bidToPosActual.get(i);
			//			//			}
			//			//			bidToPosErrorTot /= bidToPosSE.size();
			//			//			bidToPosActualTot /= bidToPosActual.size();
			//			//			double bidToPosRMSE = Math.sqrt(bidToPosErrorTot);
			//			//			System.out.println("Bid-Pos RMSE: " +  bidToPosRMSE + ", %error: " + bidToPosRMSE/bidToPosActualTot);
			//			//
			//			//
			//			//			double posToBidErrorTot = 0.0;
			//			//			double posToBidActualTot = 0.0;
			//			//			for (int i = 0; i < posToBidSE.size(); i++) {
			//			//				posToBidErrorTot += posToBidSE.get(i);
			//			//				posToBidActualTot += posToBidActual.get(i);
			//			//			}
			//			//			posToBidErrorTot /= posToBidSE.size();
			//			//			posToBidActualTot /= posToBidActual.size();
			//			//			double posToBidRMSE = Math.sqrt(posToBidErrorTot);
			//			//			System.out.println("Pos-Bid RMSE: " +  posToBidRMSE + ", %error: " + posToBidRMSE/posToBidActualTot);
			//			//
			//			//
			//			//			double convPrErrorTot = 0.0;
			//			//			double convPrActualTot = 0.0;
			//			//			for (int i = 0; i < convPrSE.size(); i++) {
			//			//				convPrErrorTot += convPrSE.get(i);
			//			//				convPrActualTot += convPrActual.get(i);
			//			//			}
			//			//			convPrErrorTot /= convPrSE.size();
			//			//			convPrActualTot /= convPrActual.size();
			//			//			double convPrRMSE = Math.sqrt(convPrErrorTot);
			//			//			System.out.println("ConvPr RMSE: " +  convPrRMSE + ", %error: " + convPrRMSE/convPrActualTot);
			//
			//			/*
			//			 * END TEST PERFECT MODEL ERROR
			//			 */

			/*
			 * Keep track of capacities
			 */
			for(int i = 0; i < _agents.length; i++) {
				Integer[] sales = _salesOverWindow.get(_agents[i]);
				Integer[] newSales = new Integer[sales.length];

				int totConversions = 0;

				for(Query query : _querySpace) {
					if(i == _ourAdvIdx) {
						totConversions += maps.get(_agents[i]).getSalesReport().getConversions(query);
					}
					else {
						totConversions += _status.getSalesReports().get(_agents[i]).get(day).getConversions(query);
					}
				}

				newSales[0] = totConversions;

				for(int j = 0; j < sales.length-1; j++) {
					newSales[j+1] = sales[j];
				}
				_salesOverWindow.put(_agents[i], newSales);
			}
			/*
			 * Keep track of all the reports
			 */
			for(int i = 0; i < _agents.length; i++) {
				LinkedList<Reports> reports = reportsListMap.get(_agents[i]);
				reports.add(maps.get(_agents[i]));
				reportsListMap.put(_agents[i],reports);
			}

			//			/*
			//			 * TESTING
			//			 */
			//			LinkedList<Reports> reports = reportsListMap.get(_agents[_ourAdvIdx]);
			//			double totalRevenue = 0.0;
			//			double totalCost = 0.0;
			//			double totalImp = 0.0;
			//			double totalClick = 0.0;
			//			double totalConv = 0.0;
			//			double totalNoneConv = 0.0;
			//			double totalCompConv = 0.0;
			//			double totalManConv = 0.0;
			//			double totalPerfConv = 0.0;
			//			double totalPos = 0.0;
			//			for(Reports report : reports) {
			//				QueryReport queryReport = report.getQueryReport();
			//				SalesReport salesReport = report.getSalesReport();
			//				for(Query query : _querySpace) {
			//					totalRevenue += salesReport.getRevenue(query);
			//					totalCost += queryReport.getCost(query);
			//					totalImp += queryReport.getImpressions(query);
			//					totalClick += queryReport.getClicks(query);
			//					totalConv += salesReport.getConversions(query);
			//					if(Double.isNaN(queryReport.getPosition(query))) {
			//						totalPos += 6.0;
			//					}
			//					else {
			//						totalPos += queryReport.getPosition(query);
			//					}
			//					if(_ourManSpecialty.equals(query.getManufacturer())) {
			//						totalManConv += salesReport.getConversions(query);
			//					}
			//					if(_ourCompSpecialty.equals(query.getComponent())) {
			//						totalCompConv += salesReport.getConversions(query);
			//					}
			//					if(_ourManSpecialty.equals(query.getManufacturer()) && _ourCompSpecialty.equals(query.getComponent())) {
			//						totalPerfConv += salesReport.getConversions(query);
			//					}
			//					if(!(_ourManSpecialty.equals(query.getManufacturer()) || _ourCompSpecialty.equals(query.getComponent()))) {
			//						totalNoneConv += salesReport.getConversions(query);
			//					}
			//				}
			//			}
			//
			//			double totSales = 0.0;
			//			Integer[] salesOverWindow = _salesOverWindow.get(_agents[_ourAdvIdx]);
			//			for(int i = 0; i < salesOverWindow.length; i++) {
			//				totSales += salesOverWindow[i];
			//			}
			//			double capacity = _capacities.get(_agents[_ourAdvIdx]);
			//
			//			if(capacity != 0) {
			//				totalOverCapPerc += (totSales/capacity);
			//			}
			//			if(totSales - capacity > 0) {
			//				totalOverCapAbs += (totSales - capacity);
			//			}
			//
			//			String output = agent + ", ";
			//			output += totalRevenue + ", ";
			//			output += totalCost + ", ";
			//			output += totalImp + ", ";
			//			output += totalClick + ", ";
			//			output += totalConv + ", ";
			//			output += totalNoneConv/totalConv + ", ";
			//			output += totalManConv/totalConv + ", ";
			//			output += totalCompConv/totalConv + ", ";
			//			output += totalPerfConv/totalConv + ", ";
			//			output += totalPos/(reports.size()*16) + ", ";
			//			output += totalOverCapPerc/(reports.size()) + ", ";
			//			output += totalOverCapAbs/(reports.size()) + ", ";
			//			//			System.out.println(output);
			stop = System.currentTimeMillis();
			elapsed = stop - start;
			System.out.println("(" + day + ") time spent doing bullshit  " + (elapsed / 1000) + " seconds");
		}
		return reportsListMap;
	}

	private HashMap<Query, double[]> getBidForEachSlot() {
		HashMap<Query,double[]> bids = new HashMap<Query, double[]>();
		for(Query query : _querySpace) {
			double[] bidArr = new double[6];
			bidArr[0] = 0.0;
			ArrayList<Double> squashedBids = new ArrayList<Double>();
			for(int i = 0; i < _agents.length; i++) {
				String agent = _agents[i];
				if(i != _ourAdvIdx) {
					HashMap<Query, Double> bidMaps = _bids.get(agent);
					double bid = bidMaps.get(query);
					double squashedBid = bid * Math.pow(_advEffect.get(agent).get(query), _squashing);
					squashedBids.add(squashedBid);
				}
			}
			Collections.sort(squashedBids);
			double ourAdvEff = Math.pow(_advEffect.get(_agents[_ourAdvIdx]).get(query),_squashing);
			int start = Math.max(0, squashedBids.size()-5);
			for(int i = start; i < squashedBids.size(); i++) {
				double squashedBid = squashedBids.get(i);
				double bid;
				if(squashedBid >= _regReserve) {
					bid = squashedBid/ourAdvEff + .00001;
				}
				else {
					bid = _regReserve/ourAdvEff + .00001;
				}
				bidArr[i+1-start] = bid;
			}
			bids.put(query, bidArr);
		}
		return bids;
	}

	private long getNewSeed() {
		Random rand = new Random(lastSeed);
		long seed = rand.nextLong();
		return Math.abs(seed);
	}

	/**
	 * This initializes all of the one time info that is received at the beginning of the game.
	 * Basic things like the retail catalogs, to more game specific (but still one time things) such
	 * as advertiser effects
	 * @param status
	 * @param advertiseridx
	 */
	public void initializeBasicInfo(GameStatus status, int advertiseridx) {
		_status = status;
		_ourAdvIdx = advertiseridx;

		_advEffect = new HashMap<String,HashMap<Query,Double>>();
		_manSpecialties = new HashMap<String,String>();
		_compSpecialties = new HashMap<String,String>();
		_contProb = new HashMap<Query,Double>();
		_salesOverWindow = new HashMap<String, Integer[]>();
		_capacities = new HashMap<String, Integer>();

		_advInfos = _status.getAdvertiserInfos();
		_agents = _status.getAdvertisers();
		_ourAdvInfo = _advInfos.get(_agents[_ourAdvIdx]);
		_targEffect = _ourAdvInfo.getTargetEffect();
		_CSB = _ourAdvInfo.getComponentBonus();
		_MSB = _ourAdvInfo.getManufacturerBonus();

		_pubInfo = _status.getPubInfo();
		_reserveInfo = _status.getReserveInfo();
		_retailCatalog = _status.getRetailCatalog();
		_slotInfo = _status.getSlotInfo();
		_userClickModel = _status.getUserClickModel();

		_squashing = _pubInfo.getSquashingParameter();
		_numPromSlots = _slotInfo.getPromotedSlots();
		_numSlots = _slotInfo.getRegularSlots();
		_promSlotBonus = _slotInfo.getPromotedSlotBonus();
		_regReserve = _reserveInfo.getRegularReserve();
		_proReserve = _reserveInfo.getPromotedReserve();


		_querySpace = new LinkedHashSet<Query>();
		_querySpace.add(new Query(null, null));
		for(Product product : _retailCatalog) {
			// The F1 query classes
			// F1 Manufacturer only
			_querySpace.add(new Query(product.getManufacturer(), null));
			// F1 Component only
			_querySpace.add(new Query(null, product.getComponent()));

			// The F2 query class
			_querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
		}

		for(Query query : _querySpace) {
			_contProb.put(query, _userClickModel.getContinuationProbability(_userClickModel.queryIndex(query)));
		}

		for(int i = 0; i < _agents.length; i++) {
			AdvertiserInfo adInfo = _advInfos.get(_agents[i]);
			String manfactBonus = adInfo.getManufacturerSpecialty();
			_manSpecialties.put(_agents[i], manfactBonus);
			String compBonus = adInfo.getComponentSpecialty();
			_compSpecialties.put(_agents[i], compBonus);

			HashMap<Query,Double> advEffect = new HashMap<Query, Double>();

			for(Query query : _querySpace) {
				advEffect.put(query, _userClickModel.getAdvertiserEffect(_userClickModel.queryIndex(query), i));
			}
			_advEffect.put(_agents[i], advEffect);
			Integer[] sales = new Integer[adInfo.getDistributionWindow()];
			for(int j = 0; j < adInfo.getDistributionWindow(); j++) {
				sales[j] = adInfo.getDistributionCapacity()/adInfo.getDistributionWindow();
			}
			_salesOverWindow.put(_agents[i], sales);
			if(i == _ourAdvIdx) {
				//				System.out.println("Our capacity" + adInfo.getDistributionCapacity());
			}
			_capacities.put(_agents[i],adInfo.getDistributionCapacity());
		}
		_ourAdvEffect = _advEffect.get(_agents[_ourAdvIdx]);
		_ourCompSpecialty = _compSpecialties.get(_agents[_ourAdvIdx]);
		_ourManSpecialty = _manSpecialties.get(_agents[_ourAdvIdx]);
	}

	/**
	 * This initializes all the day specific info that we need
	 * @param day
	 * @param advertiseridx
	 */
	public void initializeDaySpecificInfo(int day, int advertiseridx) {
		_ourAdvIdx = advertiseridx;
		_day = day;
		_adType = new HashMap<String,HashMap<Query,Ad>>();
		_bids = new HashMap<String,HashMap<Query,Double>>();
		_bidsWithoutOurs = new HashMap<String,HashMap<Query,Double>>();
		_budgets = new HashMap<String,HashMap<Query,Double>>();
		_totBudget = new HashMap<String,Double>();

		LinkedList<HashMap<Product, HashMap<UserState, Integer>>> userDists = _status.getUserDistributions();
		HashMap<String, LinkedList<BidBundle>> bidBundles = _status.getBidBundles();
		_usersMap = userDists.get(_day);

		for(int i = 0; i < _agents.length; i++) {
			BidBundle bidBundleTemp = bidBundles.get(_agents[i]).get(_day);
			_totBudget.put(_agents[i], bidBundleTemp.getCampaignDailySpendLimit());
			HashMap<Query,Ad> adType = new HashMap<Query, Ad>();
			HashMap<Query,Double> bids = new HashMap<Query, Double>();
			HashMap<Query,Double> budgets = new HashMap<Query, Double>();
			HashMap<Query,Double> bidsWithoutOurs = new HashMap<Query, Double>();
			for(Query query : _querySpace) {
				adType.put(query, bidBundleTemp.getAd(query));
				bids.put(query, bidBundleTemp.getBid(query));
				if(i != _ourAdvIdx) {
					bidsWithoutOurs.put(query, bidBundleTemp.getBid(query));
				}
				budgets.put(query,bidBundleTemp.getDailyLimit(query));
			}
			_adType.put(_agents[i], adType);
			_bids.put(_agents[i], bids);
			if(i != _ourAdvIdx) {
				_bidsWithoutOurs.put(_agents[i], bids);
			}
			_budgets.put(_agents[i], budgets);
		}
	}

	private boolean doubleEquals(double position, double d) {
		double epsilon = .05;
		double diff = Math.abs(position - d);
		if(diff <= epsilon) {
			return true;
		}
		return false;
	}

	/*
	 * Generates the perfect models to pass to the bidder
	 */
	public Set<AbstractModel> generatePerfectModels() {
		double start = System.currentTimeMillis();
		SecureRandom random = null;
		try {
			random = new SecureRandom().getInstance("SHA1PRNG");
			random.setSeed(lastSeed);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		HashMap<Query, double[]> potentialBidsMap = getBidForEachSlot();
		for(Query query : _querySpace) {
			double[] potentialBids = potentialBidsMap.get(query);
			ArrayList<Double> splitPotentialBids = new ArrayList<Double>();
			splitPotentialBids.add(potentialBids[0]);
			for(int i = 0; i < potentialBids.length-1; i++) {
				double bid1 = potentialBids[i];
				double bid2 = potentialBids[i+1];
				double bidDiff = bid2-bid1;
				splitPotentialBids.add(bid2);
				for(int j = 1; j <= _numSplits; j++) {
					splitPotentialBids.add(bid1+bidDiff*((j*1.0)/(_numSplits+1)));
				}
			}
			HashSet removeDupesSet = new HashSet(splitPotentialBids);
			splitPotentialBids.clear();
			splitPotentialBids.addAll(removeDupesSet);
			Collections.sort(splitPotentialBids);
			double[] newPotentialBids = new double[splitPotentialBids.size()];
			for(int i = 0; i < newPotentialBids.length;i++) {
				newPotentialBids[i] = splitPotentialBids.get(i);
			}
			potentialBidsMap.put(query, newPotentialBids);
		}
		HashMap<Query,HashMap<Double,Reports>> allReportsMap = new HashMap<Query,HashMap<Double,Reports>>();
		for(Query query : _querySpace) {
			HashMap<Double, Reports> queryReportsMap = new HashMap<Double,Reports>();
			allReportsMap.put(query, queryReportsMap);
		}
		for(int i = 0; i < NUM_PERF_ITERS; i++) {
			BidBundle bundle = new BidBundle();
			bundle.setCampaignDailySpendLimit(700);
			for(Query query : _querySpace) {
				double[] potentialBids = potentialBidsMap.get(query);
				double rand = random.nextDouble() * potentialBids.length;
				int idx = (int) rand;
				bundle.addQuery(query, potentialBids[idx], new Ad());
			}
			HashMap<String, Reports> reportsMap = runSimulation(bundle);
			Reports baseReports = reportsMap.get(_agents[_ourAdvIdx]);
			for(Query query : _querySpace) {
				Reports ourReports = new Reports(baseReports);
				double bid = bundle.getBid(query);
				HashMap<Double, Reports> queryReportsMap = allReportsMap.get(query);
				Reports reports = queryReportsMap.get(bid);
				if(reports == null) {
					queryReportsMap.put(bid, ourReports);
				}
				else {
					//					System.out.println(query + " bid: " + bid);
					reports.addReport(ourReports,query);
					queryReportsMap.put(bid, reports);
				}
				allReportsMap.put(query, queryReportsMap);
			}
		}
		HashMap<Query,double[]> potentialPositionsMap = new HashMap<Query, double[]>();
		HashMap<Query,HashMap<Double,Double>> bidToPosMap = new HashMap<Query,HashMap<Double,Double>>();
		HashMap<Query,HashMap<Double,Double>> bidToCPCMap = new HashMap<Query,HashMap<Double,Double>>();
		HashMap<Query,HashMap<Double,Double>> posToBidMap = new HashMap<Query,HashMap<Double,Double>>();
		for(Query query : _querySpace) {
			HashMap<Double, Reports> queryReportsMap = allReportsMap.get(query);
			double[] potentialBids = potentialBidsMap.get(query);
			HashMap<Double,Double> bidToPosQueryMap = new HashMap<Double,Double>();
			HashMap<Double,Double> bidToCPCQueryMap = new HashMap<Double,Double>();
			HashMap<Double,Double> posToBidQueryMap = new HashMap<Double,Double>();
			ArrayList<Double> potentialPositionsList = new ArrayList<Double>();
			for(int i = 0; i < potentialBids.length; i++) {
				double bid = potentialBids[i];
				Reports reports = queryReportsMap.get(bid);
				double pos = reports.getPosition(query);
				if(Double.isNaN(pos)) {
					pos = 6.0;
				}
				double cpc = reports.getCPC(query);
				if(Double.isNaN(cpc)) {
					cpc = 0.0;
				}
				potentialPositionsList.add(pos);
				bidToPosQueryMap.put(bid, pos);
				bidToCPCQueryMap.put(bid,cpc);
				posToBidQueryMap.put(pos,bid);
			}
			Collections.sort(potentialPositionsList);
			double[] potentialPositions = new double[potentialPositionsList.size()];
			for(int i = 0; i < potentialPositions.length; i++) {
				potentialPositions[i] = potentialPositionsList.get(i);
			}
			potentialPositionsMap.put(query, potentialPositions);
			bidToPosMap.put(query, bidToPosQueryMap);
			bidToCPCMap.put(query, bidToCPCQueryMap);
			posToBidMap.put(query, posToBidQueryMap);
		}


		/*
		 * Create Gaussian noise for models
		 * 
		 */

		HashMap<Query,Double> bidToPrClickNoise = new HashMap<Query, Double>();
		HashMap<Query,Double> posToPrClickNoise = new HashMap<Query, Double>();
		HashMap<Query,Double> queryToNumImpsNoise = new HashMap<Query, Double>();

		if(_noise) {
			for(Query q : _querySpace) {
				bidToPrClickNoise.put(q, random.nextGaussian()*_noisePrClick);
				posToPrClickNoise.put(q, random.nextGaussian()*_noisePrClick);
				queryToNumImpsNoise.put(q, random.nextGaussian()*_noiseImps);
			}
		}
		else {
			for(Query q : _querySpace) {
				bidToPrClickNoise.put(q, 0.0);
				posToPrClickNoise.put(q, 0.0);
				queryToNumImpsNoise.put(q, 0.0);
			}
		}


		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();

		PerfectUserModel userModel = new PerfectUserModel(_numUsers,_usersMap);
		PerfectQueryToNumImp queryToNumImp = new PerfectQueryToNumImp(allReportsMap,potentialBidsMap,queryToNumImpsNoise);
		PerfectUnitsSoldModel unitsSold = new PerfectUnitsSoldModel(_salesOverWindow.get(_agents[_ourAdvIdx]), _ourAdvInfo.getDistributionCapacity(), _ourAdvInfo.getDistributionWindow());
		AbstractBidToCPC bidToCPCModel = new PerfectBidToCPC(allReportsMap,potentialBidsMap);
		AbstractBidToPrClick bidToClickPrModel = new PerfectBidToPrClick(allReportsMap,potentialBidsMap,bidToPrClickNoise);
		AbstractPosToCPC posToCPCModel = new PerfectPosToCPC(allReportsMap,potentialBidsMap, posToBidMap);
		AbstractPosToPrClick posToClickPrModel = new PerfectPosToPrClick(allReportsMap,potentialBidsMap, posToBidMap,posToPrClickNoise);
		AbstractConversionModel queryToConvPrModel = new PerfectQueryToPrConv(allReportsMap, potentialBidsMap, posToBidMap);
		AbstractBidToPos bidToPosModel = new PerfectBidToPos(bidToPosMap);
		AbstractPosToBid posToBidModel = new PerfectPosToBid(posToBidMap);
		BasicTargetModel basicTargModel = new BasicTargetModel(_ourAdvInfo.getManufacturerSpecialty(),_ourAdvInfo.getComponentSpecialty());

		models.add(userModel);
		models.add(queryToNumImp);
		models.add(unitsSold);
		models.add(bidToCPCModel);
		models.add(bidToClickPrModel);
		models.add(posToCPCModel);
		models.add(posToClickPrModel);
		models.add(queryToConvPrModel);
		models.add(bidToPosModel);
		models.add(posToBidModel);
		models.add(basicTargModel);

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");

		return models;
	}

	/*
	 * Gets the bids from the agent using perfect models
	 */
	public BidBundle getBids(AbstractAgent agentToRun) {
		if(PERFECTMODELS) {
			//			Set<AbstractModel> perfectModels = generatePerfectModels();
			Set<AbstractModel> regModels = agentToRun.getModels();

			//			//OPTION 1 (MOSTLY PERF MODELS)
			//
			//			//Remove models we want reg in perfect models:
			//			ArrayList<AbstractModel> modelsToRemove = new ArrayList<AbstractModel>();
			//			for(AbstractModel model : perfectModels) {
			//				//				if(model instanceof AbstractQueryToNumImp) {
			//				//					modelsToRemove.add(model);
			//				//				}
			//				if(model instanceof AbstractBidToPrClick) {
			//					modelsToRemove.add(model);
			//				}
			//				if(model instanceof AbstractPosToPrClick) {
			//					modelsToRemove.add(model);
			//				}
			//			}
			//			perfectModels.removeAll(modelsToRemove);
			//
			//			//add regmodels models we want
			//			for(AbstractModel model : regModels) {
			//				//				if(model instanceof AbstractQueryToNumImp) {
			//				//					perfectModels.add(model);
			//				//				}
			//				if(model instanceof AbstractBidToPrClick) {
			//					perfectModels.add(model);
			//				}
			//				if(model instanceof AbstractPosToPrClick) {
			//					perfectModels.add(model);
			//				}
			//			}
			//			agentToRun.setModels(perfectModels);




			//OPTION 2 (MOSTLY REG MODELs)

			//Remove models we want perfect in reg models:
			ArrayList<AbstractModel> modelsToRemove = new ArrayList<AbstractModel>();
			for(AbstractModel model : regModels) {
				//				if(model instanceof AbstractQueryToNumImp) {
				//					modelsToRemove.add(model);
				//				}
				if(model instanceof AbstractUserModel) {
					modelsToRemove.add(model);
				}
				if(model instanceof AbstractQueryToNumImp) {
					modelsToRemove.add(model);
				}
			}
			regModels.removeAll(modelsToRemove);
			PerfectUserModel userModel = new PerfectUserModel(_numUsers,_usersMap);
			AbstractQueryToNumImp queryToNumImp = new BasicQueryToNumImp(userModel);
			regModels.add(userModel);
			regModels.add(queryToNumImp);

			//			//add perfect models we want
			//			for(AbstractModel model : perfectModels) {
			//				//				if(model instanceof AbstractQueryToNumImp) {
			//				//					regModels.add(model);
			//				//				}
			//				if(model instanceof AbstractBidToPrClick) {
			//					regModels.add(model);
			//				}
			//				if(model instanceof AbstractPosToPrClick) {
			//					regModels.add(model);
			//				}
			//			}
			agentToRun.setModels(regModels);


			//OPTION 3 (ALL PERF MODELS)
			//			agentToRun.setModels(perfectModels);

		}
		BidBundle bidBundle = agentToRun.getBidBundle(agentToRun.getModels());
		return bidBundle;
	}

	public ArrayList<SimAgent> buildAgents(AbstractAgent agentToRun) {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				BidBundle bundle = null;
				bundle = getBids(agentToRun);
				_ourBidBundle = bundle;
				agentToRun.handleBidBundle(bundle);
				double totBudget;
				if(!_killBudgets) {
					totBudget = bundle.getCampaignDailySpendLimit();
				}
				else {
					totBudget = Double.NaN;
				}
				HashMap<Query,Double> bids = new HashMap<Query, Double>();
				HashMap<Query,Double> budgets = new HashMap<Query, Double>();
				HashMap<Query,Ad> adTypes = new HashMap<Query, Ad>();
				for(Query query : _querySpace) {
					bids.put(query, bundle.getBid(query));
					if(!_killBudgets) {
						budgets.put(query,bundle.getDailyLimit(query));
					}
					else {
						budgets.put(query,Double.NaN);
					}
					adTypes.put(query, bundle.getAd(query));
				}
				agent = new SimAgent(bids,budgets,totBudget,_advEffect.get(_agents[i]),adTypes,_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
			}
			else {
				if(!_killBudgets) {
					agent = new SimAgent(_bids.get(_agents[i]),_budgets.get(_agents[i]),_totBudget.get(_agents[i]),_advEffect.get(_agents[i]),_adType.get(_agents[i]),_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);					
				}
				else {
					HashMap<Query,Double> budgets = new HashMap<Query, Double>();
					for(Query query : _querySpace) {
						budgets.put(query,Double.NaN);
					}
					agent = new SimAgent(_bids.get(_agents[i]),budgets,Double.NaN,_advEffect.get(_agents[i]),_adType.get(_agents[i]),_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
				}
			}
			agents.add(agent);
		}
		return agents;
	}

	public ArrayList<SimAgent> buildAgents(BidBundle agentToRun) {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				BidBundle bundle = agentToRun;
				double totBudget;
				if(!_killBudgets) {
					totBudget = bundle.getCampaignDailySpendLimit();
				}
				else {
					totBudget = Double.NaN;
				}
				HashMap<Query,Double> bids = new HashMap<Query, Double>();
				HashMap<Query,Double> budgets = new HashMap<Query, Double>();
				HashMap<Query,Ad> adTypes = new HashMap<Query, Ad>();
				for(Query query : _querySpace) {
					bids.put(query, bundle.getBid(query));
					if(!_killBudgets) {
						budgets.put(query,bundle.getDailyLimit(query));
					}
					else {
						budgets.put(query,Double.NaN);
					}
					adTypes.put(query, bundle.getAd(query));
				}
				agent = new SimAgent(bids,budgets,totBudget,_advEffect.get(_agents[i]),adTypes,_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
			}
			else {
				if(!_killBudgets) {
					agent = new SimAgent(_bids.get(_agents[i]),_budgets.get(_agents[i]),_totBudget.get(_agents[i]),_advEffect.get(_agents[i]),_adType.get(_agents[i]),_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);					
				}
				else {
					HashMap<Query,Double> budgets = new HashMap<Query, Double>();
					for(Query query : _querySpace) {
						budgets.put(query,Double.NaN);
					}
					agent = new SimAgent(_bids.get(_agents[i]),budgets,Double.NaN,_advEffect.get(_agents[i]),_adType.get(_agents[i]),_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
				}
			}
			agents.add(agent);
		}
		return agents;
	}

	public ArrayList<SimUser> buildSearchingUserBase(HashMap<Product, HashMap<UserState, Integer>> usersMap) {
		ArrayList<SimUser> usersList = new ArrayList<SimUser>();
		_R.setSeed(lastSeed);
		for(Product prod : _retailCatalog) {
			for(UserState state : UserState.values()) {
				int users = usersMap.get(prod).get(state);
				for(int i = 0; i < users; i++) {
					SimUser user = new SimUser(prod,state,_R.nextLong());
					usersList.add(user);
				}
			}
		}
		return usersList;
	}

	/*
	 * Runs the simulation and generates reports
	 */
	public HashMap<String, Reports> runSimulation(Object agentToRun) {
		_R.setSeed(lastSeed);
		ArrayList<SimAgent> agents;
		if(agentToRun instanceof AbstractAgent) {
			agents = buildAgents((AbstractAgent) agentToRun);
		}
		else if(agentToRun instanceof BidBundle) {
			agents = buildAgents((BidBundle) agentToRun);
		}
		else {
			throw new RuntimeException("Build agents can only take type SimAbstractAgent or BidBundle");			
		}
		ArrayList<SimUser> users;
		if(_pregenUsers != null) {
			users = _pregenUsers;
		}
		else {
			users = buildSearchingUserBase(_usersMap);
			Random randGen = new Random(lastSeed);
			Collections.shuffle(users,randGen);
			_pregenUsers  = users;
		}
		long lastMiniSeed = lastSeed;
		for(int i = 0; i < users.size(); i++) {
			SimUser user = users.get(i);
			Query query = user.getUserQuery();
			if(query == null) {
				//This means the user is IS or T
				continue;
			}

			_R.setSeed(lastMiniSeed);
			lastMiniSeed = _R.nextLong();

			ArrayList<AgentBidPair> pairList = new ArrayList<AgentBidPair>();
			for(int j = 0; j < agents.size(); j++) {
				SimAgent agent = agents.get(j);
				double bid = agent.getBid(query);
				double budget = agent.getBudget(query);
				double cost = agent.getCost(query);
				if(budget > cost + bid || Double.isNaN(budget)) {
					double totBudget = agent.getTotBudget();
					double totCost = agent.getTotCost();
					if(totBudget > totCost + bid || Double.isNaN(totBudget)) {
						double squashedBid = agent.getSquashedBid(query);
						if(squashedBid >= _regReserve) {
							AgentBidPair pair = new AgentBidPair(agents.get(j),squashedBid);
							pairList.add(pair);
						}
					}
				}
			}
			/*
			 * This sorts the list by squashed bid
			 */
			Collections.sort(pairList);
			/*
			 * Adds impressions
			 */
			for(int j = 1; j <= _numSlots && j < pairList.size(); j++) {
				AgentBidPair pair = pairList.get(j-1);
				double squashedBid = pair.getSquashedBid();
				if(j <= _numPromSlots && squashedBid >= _proReserve) {
					pair.getAgent().addImpressions(query, 0, 1, j);
				}
				else {
					pair.getAgent().addImpressions(query, 1, 0, j);
				}

			}
			/*
			 * Actually generates clicks and what not
			 */
			for(int j = 1; j <= _numSlots && j < pairList.size(); j++) {
				AgentBidPair pair = pairList.get(j-1);
				SimAgent agent = pair.getAgent();
				double squashedBid = pair.getSquashedBid();
				double contProb = _contProb.get(query);
				Ad ad = agent.getAd(query);
				double advEffect = agent.getAdvEffect(query);
				double fTarg = 1;
				if(ad != null && !ad.isGeneric()) {
					if(ad.getProduct() == new Product(query.getManufacturer(),query.getComponent())) {
						fTarg = 1 + _targEffect;
					}
					else {
						fTarg = 1/(1+_targEffect);
					}
				}

				double fProm = 1;
				if(j <= _numPromSlots && squashedBid >= _proReserve) {
					fProm = 1 + _promSlotBonus;
				}
				double clickPr = eta(advEffect,fTarg*fProm);
				double rand = _R.nextDouble();
				if(clickPr >= rand) {
					AgentBidPair underPair = pairList.get(j);
					SimAgent agentUnder = underPair.getAgent();
					double bidUnder = agentUnder.getBid(query);
					double advEffUnder = agentUnder.getAdvEffect(query);
					double squashedBidUnder = Math.pow(advEffUnder, _squashing) * bidUnder;
					if(j <= _numPromSlots && squashedBid >= _proReserve && squashedBidUnder <= _proReserve) {
						squashedBidUnder = _proReserve;
					}
					double cpc = squashedBidUnder / Math.pow(advEffect, _squashing) + .01;
					cpc = ((int) ((cpc * 100) + 0.5)) / 100.0;
					agent.addCost(query, cpc);

					double baselineConv;
					if(is2009 || (user.getUserState() != UserState.IS)) {

						if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
							baselineConv = .1;
						}
						else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
							baselineConv = .2;
						}
						else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
							baselineConv = .3;
						}
						else {
							throw new RuntimeException("Malformed Query");
						}

						double overCap = agent.getOverCap();
						if(agent.getAdvId().equals(_agents[_ourAdvIdx])) {
							//						debug(agent.getAdvId() + " is overcap by " + overCap);
						}
						double convPr = Math.pow(_LAMBDA, Math.max(0.0, overCap))*baselineConv;

						String queryComp = query.getComponent();
						String compSpecialty = agent.getCompSpecialty();

						if(compSpecialty.equals(queryComp)) {
							convPr = eta(convPr,1+_CSB);
						}

						rand = _R.nextDouble();

						if(convPr >= rand) {
							String queryMan = query.getManufacturer();
							String manSpecialty = agent.getManSpecialty();
							double revenue = 10;
							if(manSpecialty.equals(queryMan)) {
								revenue = (1+_MSB)*10;
							}
							agent.addRevenue(query, revenue);
							break;
						}
						else {
							rand = _R.nextDouble();
							if(contProb >= rand) {
								continue;
							}
							else {
								break;
							}
						}
					}
					else {
						rand = _R.nextDouble();
						if(contProb >= rand) {
							continue;
						}
						else {
							break;
						}
					}
				}
			}
		}
		if(DEBUG) {
			//			AvgPosToPosDist avgPosModel20 = new AvgPosToPosDist(20);
			//			AvgPosToPosDist avgPosModel40 = new AvgPosToPosDist(40);
			//			AvgPosToPosDist avgPosModel80 = new AvgPosToPosDist(80);
			//			AvgPosToPosDist avgPosModel160 = new AvgPosToPosDist(160);
			//			AvgPosToPosDist avgPosModel320 = new AvgPosToPosDist(320);
			//			AvgPosToPosDist avgPosModel640 = new AvgPosToPosDist(640);
			//			AvgPosToPosDist avgPosModelall = new AvgPosToPosDist(1000000);
			for(int i = 0; i < agents.size(); i++) {
				SimAgent agent = agents.get(i);
				if(i == _ourAdvIdx) {
					debug("****US****");
					//				}
					debug("Adv Id: " + agent.getAdvId());
					debug("Total Cost: " + agent.getTotCost());
					debug("Total Budget: " + agent.getTotBudget());
					debug("Total revenue: " + agent.getTotRevenue());
					debug("Total Units Sold: " + agent.getTotUnitsSold());
					for(Query query : _querySpace) {
						debug(" Query: " + query);
						debug(" Bid: " + agent.getBid(query));
						debug(" CPC: " + agent.getCPC(query));
						debug(" Cost: " + agent.getCost(query));
						debug(" Budget: " + agent.getBudget(query));
						debug(" Revenue: " + agent.getRevenue(query));
						debug(" Units Sold: " + agent.getUnitsSold(query));
						debug(" Num Clicks: " + agent.getNumClicks(query));
						debug(" Num Prom Slots: " + _numPromSlots);
						debug(" Prom Impressions: " + agent.getNumPromImps(query));
						debug(" Reg Impressions: " + agent.getNumRegImps(query));
						debug(" Avg Pos per Imp: " + (agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))));
						double[] perQPos = agent.getPerQPosSum(query);
						for(int j = 0; j < 5; j++) {
							debug(" Imps in Slot " + (j+1) + ": " + (perQPos[j]));
						}
						//						if(!Double.isNaN((agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))))) {
						//							double[] expPos = avgPosModel80.getPrediction(query, agent.getNumRegImps(query), agent.getNumPromImps(query), (agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))), agent.getNumClicks(query), _numPromSlots);
						//							for(int j = 0; j < 5; j++) {
						//								debug(" Estimated Imps in Slot " + (j+1) + ": " + (expPos[j]));
						//							}
						//							System.out.println("Likelihood: " + KLLikelihood(normalizeArr(perQPos),normalizeArr(expPos)));
						//						}
					}
				}
			}
		}
		//		////TESTIOGs
		//		for(Query q : _querySpace) {
		//			System.out.println(q);
		//			for(int i = 0; i < agents.size(); i++) {
		//				if(i == _ourAdvIdx) {
		//					SimAgent agent = agents.get(i);
		//					System.out.println("Slot " + (agent.getPosSum(q)/(agent.getNumPromImps(q)+agent.getNumRegImps(q))) + "  conversions " + agent.getUnitsSold(q));
		//				}
		//			}
		//		}
		//
		//		//ASPIDMPASD
		HashMap<String,Reports> reportsMap = new HashMap<String, Reports>();
		for(SimAgent agent : agents) {
			QueryReport queryReport = agent.buildQueryReport();
			SalesReport salesReport = agent.buildSalesReport();
			Reports reports = new Reports(queryReport,salesReport);
			reportsMap.put(agent.getAdvId(),reports);
		}
		return reportsMap;
	}

	public String[] getUsableAgents() {
		String[] agentStrings = { "MCKP", "Cheap" , "Crest", "ILP", "newSSB"};
		return agentStrings;
	}

	public AbstractAgent stringToAgent(String string) {
		if(string.equals("MCKP")) {
			//			return new SemiEndoMCKPBid(false,false,false);
		}
		else if(string.equals("Cheap")) {
			return new Cheap();
		}
		else if(string.equals("Crest")) {
			return new ConstantPM();
		}
		else if(string.equals("ILP")) {
			return new ILPBidAgent();
		}
		else if(string.equals("newSSB")) {
			return new Cheap();
		}
		else {
			//			return new SemiEndoMCKPBid(false,false,false);
		}
		return new Cheap();
	}

	private double eta(double p, double x) {
		return (p*x) / (p*x + (1-p));
	}

	public void debug(Object str) {
		if(DEBUG) {
			System.out.println(str);
		}
	}

	public Set<Query> getQuerySpace() {
		return _querySpace;
	}

	/*
	 * Returns the means and std deviation
	 * index 0 is the mean
	 * index 1 is the std deviation
	 */
	public static double[] stdDeviation(Double[] revenueErrorArr) {
		double[] meanAndStdDev = new double[2];
		meanAndStdDev[0] = 0.0;
		meanAndStdDev[1] = 0.0;
		for(int i = 0; i < revenueErrorArr.length; i++) {
			meanAndStdDev[0] += revenueErrorArr[i];
		}
		meanAndStdDev[0] /= revenueErrorArr.length;
		for(int i = 0; i < revenueErrorArr.length; i++) {
			meanAndStdDev[1] +=  (revenueErrorArr[i] - meanAndStdDev[0])*(revenueErrorArr[i] - meanAndStdDev[0]);
		}
		meanAndStdDev[1] /= revenueErrorArr.length;
		meanAndStdDev[1] = Math.sqrt(meanAndStdDev[1]);
		return meanAndStdDev;
	}


	public ArrayList<Double> runSimulations(String baseFile, int min, int max, double noiseImps, double noisePrClick, AbstractAgent agent) throws IOException, ParseException {
		//		String baseFile = "/Users/jordan/Downloads/aa-server-0.9.6/logs/sims/localhost_sim";
		//		String baseFile = "/games/game";
		//		String baseFile = "/home/jberg/mckpgames/localhost_sim";
		//		String baseFile = "/Users/jordanberg/Desktop/mckpgames/localhost_sim";
		//		String baseFile = "/pro/aa/usr/jberg/mckpgames/localhost_sim";
		//		String baseFile = "C:/mckpgames/localhost_sim";

		//		int min = 454;
		//		int max = 455;

		//		String baseFile = "/Users/jordanberg/Desktop/lalagames/localhost_sim";
		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/test/game";
		//				String baseFile = "/pro/aa/finals/day-2/server-1/game";

		HashMap<String,HashMap<String, LinkedList<Reports>>> reportsListMegaMap = new HashMap<String, HashMap<String,LinkedList<Reports>>>();
		_noise = false;
		if(noiseImps != 0 || noisePrClick != 0) {
			_noise = true;
			_noiseImps = noiseImps;
			_noisePrClick = noisePrClick;
		}

		String[] filenames;
		if(min == max) {
			filenames = new String[1];
			filenames[0] = baseFile;
		}
		else {
			filenames = new String[max-min];
			//			System.out.println("Min: " + min + "  Max: " + max);
			for(int i = min; i < max; i++) { 
				filenames[i-min] = baseFile + i + ".slg";
			}
		}
		for(int fileIdx = 0; fileIdx < filenames.length; fileIdx++) {

			/*
			 * ENSURE GARBAGE COLLECTOR IS RUN BETWEEN ITERATIONS
			 */
			System.gc(); emptyFunction(); emptyFunction(); emptyFunction(); emptyFunction();

			String filename = filenames[fileIdx];
			int advId = 0;  //Set to 1 for schlemazl on day-2/server1
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			_querySpace = new LinkedHashSet<Query>();
			_querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				_querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				_querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				_querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			HashMap<String,LinkedList<Reports>> reportsListMap = new HashMap<String,LinkedList<Reports>>();
			for(int i = 0; i < agents.length; i++) {
				LinkedList<Reports> reportsList = new LinkedList<Reports>();
				reportsListMap.put(agents[i], reportsList);
			}
			System.out.println(filename);
			//			System.out.println(status.getAdvertiserInfos().get(agents[advId]).getDistributionCapacity());
			HashMap<String, LinkedList<Reports>> maps = runFullSimulation(status, agent.getCopy(), advId);
			for(int j = 0; j < agents.length; j++) {
				reportsListMap.put(agents[j],maps.get(agents[j]));
			}
			reportsListMegaMap.put(filename, reportsListMap);
		}

		double capacity = _capacities.get(_agents[_ourAdvIdx]);
		double totalRevenue = 0;
		double totalCost = 0;
		double totalImp = 0;
		double totalClick = 0;
		double totalConv = 0;
		double totalNoneConv = 0.0;
		double totalCompConv = 0.0;
		double totalManConv = 0.0;
		double totalPerfConv = 0.0;
		double totalPos = 0;
		double percInAuctions = 0;
		double totDays = 0;
		double totOverCap = 0;
		double percOverCap = 0;
		System.out.println("Generating Reports");
		String profits = "";
		for(String filename : reportsListMegaMap.keySet()) {
			System.out.println(filename);
			HashMap<String, LinkedList<Reports>> map = reportsListMegaMap.get(filename);
			LinkedList<Reports> reports = map.get(_agents[_ourAdvIdx]);
			totDays = reportsListMegaMap.size() * reports.size();
			double profitTot = 0;
			int[] sales = new int[reports.size()];
			int i = 0;
			for(Reports report : reports) {
				QueryReport queryReport = report.getQueryReport();
				SalesReport salesReport = report.getSalesReport();
				int totSales = 0;
				for(Query query : _querySpace) {
					profitTot += salesReport.getRevenue(query)-queryReport.getCost(query);
					totalRevenue += salesReport.getRevenue(query);
					totalCost += queryReport.getCost(query);
					totalImp += queryReport.getImpressions(query);
					totalClick += queryReport.getClicks(query);
					totalConv += salesReport.getConversions(query);
					totSales += salesReport.getConversions(query);
					if(!Double.isNaN(queryReport.getPosition(query))) {
						totalPos += queryReport.getPosition(query);
						percInAuctions++;
					}
					if(_ourManSpecialty.equals(query.getManufacturer())) {
						totalManConv += salesReport.getConversions(query);
					}
					if(_ourCompSpecialty.equals(query.getComponent())) {
						totalCompConv += salesReport.getConversions(query);
					}
					if(_ourManSpecialty.equals(query.getManufacturer()) && _ourCompSpecialty.equals(query.getComponent())) {
						totalPerfConv += salesReport.getConversions(query);
					}
					if(!(_ourManSpecialty.equals(query.getManufacturer()) || _ourCompSpecialty.equals(query.getComponent()))) {
						totalNoneConv += salesReport.getConversions(query);
					}
				}
				sales[i] = totSales;
				i++;
			}
			profits += profitTot + ", ";
			for(int j = 0; j < sales.length; j++) {
				double convs = 0;
				for(int idx = j-4; idx < j; idx++) {
					if(idx < 0) {
						convs += capacity/5.0;
					}
					else {
						convs += sales[idx];
					}
				}
				convs += sales[j];
				totOverCap += convs-capacity;
				percOverCap += convs/capacity;
			}
		}
		System.out.println("Profit per game: , " + profits);
		String header = "Agent,Avg Profit,Avg Revenue,Avg Cost,Avg Impressions,Avg Clicks,Avg Conversions,Avg Position,% in auction,CPC,ClickPr,ConvPr,ConvPr neither,ConvPr in comp" +
		",ConvPr in man,ConvPr in both, # Overcap, % Overcap";
		String output = agent + ",";

		output += ((totalRevenue-totalCost)/reportsListMegaMap.size()) + ",";
		output +=  (totalRevenue/reportsListMegaMap.size()) + ",";
		output +=  (totalCost/reportsListMegaMap.size()) + ",";
		output +=  (totalImp/reportsListMegaMap.size()) + ",";
		output +=  (totalClick/reportsListMegaMap.size()) + ",";
		output +=  (totalConv/reportsListMegaMap.size()) + ",";
		output +=  (totalPos/(percInAuctions)) + ",";
		output +=  (percInAuctions/(totDays*16)) + ",";
		output += (totalCost/totalClick) + ",";
		output += (totalClick/totalImp) + ",";
		output += (totalConv/totalClick) + ",";
		output += (totalNoneConv/totalConv) + ",";
		output += (totalCompConv/totalConv) + ",";
		output += (totalManConv/totalConv) + ",";
		output += (totalPerfConv/totalConv) + ",";
		output += (totOverCap/(totDays)) + ",";
		output += (percOverCap/(totDays)) + ",";
		System.out.println(header + "\n" + output);

		ArrayList<Double> returnVals = new ArrayList<Double>();
		returnVals.add(((totalRevenue-totalCost)/reportsListMegaMap.size()));
		returnVals.add((totalRevenue/reportsListMegaMap.size()));
		returnVals.add((totalCost/reportsListMegaMap.size()));
		returnVals.add((totalImp/reportsListMegaMap.size()));
		returnVals.add((totalClick/reportsListMegaMap.size()));
		returnVals.add((totalConv/reportsListMegaMap.size()));
		returnVals.add((totalPos/(percInAuctions)));
		returnVals.add((percInAuctions/(totDays*16)));
		returnVals.add((totalCost/totalClick));
		returnVals.add((totalClick/totalImp));
		returnVals.add((totalConv/totalClick));
		returnVals.add((totalNoneConv/totalConv));
		returnVals.add((totalCompConv/totalConv));
		returnVals.add((totalManConv/totalConv));
		returnVals.add((totalPerfConv/totalConv));
		returnVals.add((totOverCap/(totDays)));
		returnVals.add((percOverCap/(totDays)));
		returnVals.add(capacity);

		return returnVals;
	}


	public double[] percentErrorOfSim() throws IOException, ParseException {
		double RMSRevenue = 0.0;
		double RMSCost = 0.0;
		double RMSImp = 0.0;
		double RMSClick = 0.0;
		double RMSConv = 0.0;
		double totAvgRevenue = 0.0;
		double totAvgCost = 0.0;
		double totAvgImp = 0.0;
		double totAvgClick = 0.0;
		double totAvgConv = 0.0;
		int numSims = 1;
		//		String baseFile = "/Users/jordan/Downloads/aa-server-0.9.6/logs/sims/localhost_sim";
		//		String baseFile = "/games/game";
		//		String baseFile = "/home/jberg/mckpgames/localhost_sim";
		//		String baseFile = "/Users/jordanberg/Desktop/mckpgames/localhost_sim";
		//		String baseFile = "/pro/aa/usr/jberg/mckpgames/localhost_sim";
		//		String baseFile = "C:/mckpgames/localhost_sim";

		//		int min = 454;
		//		int max = 455;

		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";

		int min = 1425;
		int max = 1440;

		String[] filenames = new String[max-min];
		System.out.println("Min: " + min + "  Max: " + max + "  Num Sims: " + numSims);
		for(int i = min; i < max; i++) { 
			filenames[i-min] = baseFile + i + ".slg";
		}
		for(int fileIdx = 0; fileIdx < filenames.length; fileIdx++) {
			String filename = filenames[fileIdx];
			int advId = 4;
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			_querySpace = new LinkedHashSet<Query>();
			_querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				_querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				_querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				_querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			double[] totalRevenueList = new double[agents.length];
			double[] totalCostList = new double[agents.length];
			double[] totalImpList = new double[agents.length];
			double[] totalClickList = new double[agents.length];
			double[] totalConvList = new double[agents.length];		

			HashMap<String, LinkedList<QueryReport>> queryReportListMap = status.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReportListMap = status.getSalesReports();
			for(int i = 0; i < agents.length; i++ ) {
				LinkedList<QueryReport> queryReportList = queryReportListMap.get(agents[i]);
				LinkedList<SalesReport> salesReportList = salesReportListMap.get(agents[i]);
				double totalRevenue = 0;
				double totalCost = 0;
				double totalImp = 0;
				double totalClick = 0;
				double totalConv = 0;
				for(int j = 0; j < queryReportList.size(); j++) {
					QueryReport queryReport = queryReportList.get(j);
					SalesReport salesReport = salesReportList.get(j);
					for(Query query : _querySpace) {
						totalRevenue += salesReport.getRevenue(query);
						totalCost += queryReport.getCost(query);
						totalImp += queryReport.getImpressions(query);
						totalClick += queryReport.getClicks(query);
						totalConv += salesReport.getConversions(query);
					}
				}
				totalRevenueList[i] = totalRevenue;
				totalCostList[i] = totalCost;
				totalImpList[i] = totalImp;
				totalClickList[i] = totalClick;
				totalConvList[i] = totalConv;
				totAvgRevenue += totalRevenue;
				totAvgCost += totalCost;
				totAvgImp += totalImp;
				totAvgClick += totalClick;
				totAvgConv += totalConv;
			}

			HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap = new HashMap<String,LinkedList<LinkedList<Reports>>>();
			for(int i = 0; i < agents.length; i++) {
				LinkedList<LinkedList<Reports>> reportsList = new LinkedList<LinkedList<Reports>>();
				reportsListMap.put(agents[i], reportsList);
			}
			for(int i = 0; i < numSims; i++) {
				HashMap<String, LinkedList<Reports>> maps = runFullSimulation(status, new GoodSlot(), advId);
				for(int j = 0; j < agents.length; j++) {
					LinkedList<LinkedList<Reports>> reportsList = reportsListMap.get(agents[j]);
					reportsList.add(maps.get(agents[j]));
					reportsListMap.put(agents[j], reportsList);
				}
			}

			for(int i = 0; i < _agents.length; i++ ) {
				double totRevenueReal = totalRevenueList[i];
				double totCostReal = totalCostList[i];
				double totImpReal = totalImpList[i];
				double totClickReal = totalClickList[i];
				double totConvReal = totalConvList[i];
				LinkedList<LinkedList<Reports>> reportsList = reportsListMap.get(_agents[i]);
				for(int j = 0; j < reportsList.size(); j++) {
					LinkedList<Reports> reports = reportsList.get(j);
					double totalRevenue = 0;
					double totalCost = 0;
					double totalImp = 0;
					double totalClick = 0;
					double totalConv = 0;
					for(Reports report : reports) {
						QueryReport queryReport = report.getQueryReport();
						SalesReport salesReport = report.getSalesReport();
						for(Query query : _querySpace) {
							totalRevenue += salesReport.getRevenue(query);
							totalCost += queryReport.getCost(query);
							totalImp += queryReport.getImpressions(query);
							totalClick += queryReport.getClicks(query);
							totalConv += salesReport.getConversions(query);
						}
					}
					RMSRevenue += ((totalRevenue-totRevenueReal)*(totalRevenue-totRevenueReal));
					RMSCost += ((totalCost-totCostReal)*(totalCost-totCostReal));
					RMSImp += ((totalImp-totImpReal)*(totalImp-totImpReal));
					RMSClick += ((totalClick-totClickReal)*(totalClick-totClickReal));
					RMSConv += ((totalConv-totConvReal)*(totalConv-totConvReal));
				}
			}
		}

		/*
		 * Average actual values
		 * We divide by the number of sims times the number of advertisers
		 */
		totAvgRevenue /= ((max-min)*(_agents.length));
		totAvgCost /= ((max-min)*(_agents.length));
		totAvgImp /= ((max-min)*(_agents.length));
		totAvgClick /= ((max-min)*(_agents.length));
		totAvgConv /= ((max-min)*(_agents.length));


		/*
		 * Mean Square calculation
		 * need to divide by the number of samples
		 */
		RMSRevenue /= ((max-min)*(_agents.length)*numSims);
		RMSCost /= ((max-min)*(_agents.length)*numSims);
		RMSImp /= ((max-min)*(_agents.length)*numSims);
		RMSClick /= ((max-min)*(_agents.length)*numSims);
		RMSConv /= ((max-min)*(_agents.length)*numSims);

		//RMS
		RMSRevenue = Math.sqrt(RMSRevenue);
		RMSCost = Math.sqrt(RMSCost);
		RMSImp = Math.sqrt(RMSImp);
		RMSClick = Math.sqrt(RMSClick);
		RMSConv = Math.sqrt(RMSConv);

		double[] error = new double[5];
		error[0] = RMSRevenue/totAvgRevenue;
		error[1] = RMSCost/totAvgCost;
		error[2] = RMSImp/totAvgImp;
		error[3] = RMSClick/totAvgClick;
		error[4] = RMSConv/totAvgConv;

		return error;
	}

	public void setSeed(long seed) {
		lastSeed = seed;
	}

	public static void main(String[] args) throws IOException, ParseException {
		main2(args);
	}

	public static void main1(String[] args) throws IOException, ParseException {
		BasicSimulator sim = new BasicSimulator();
		double start = System.currentTimeMillis();
		ArrayList<AbstractAgent> agentList = new ArrayList<AbstractAgent>();
		Random r = new Random(68616);

		//		AbstractAgent agent = new HardMCKPBid(Double.parseDouble(args[0]));
		//				AbstractAgent agent = new EquatePPSSimple(Double.parseDouble(args[0]));
		AbstractAgent agent = new EquatePMSimple(Double.parseDouble(args[0]));
		agentList.add(agent);

		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server2/game";
		String baseFile = "/u/jberg/finals/day-2/server-2/game";

		//		int min = 1425;
		//		int max = 1465;

		int min = 297;
		int max = 337;

		ArrayList<Double> returnVals = new ArrayList<Double>();

		for(int i = 0; i < 17; i++) {
			returnVals.add(0.0);
		}

		for(int i = min; i < max; i++) {
			sim.setSeed(r.nextLong());
			ArrayList<Double> val = sim.runSimulations(baseFile,i,i+1,0,0, agent);
			for(int j = 0; j < 17; j++) {
				returnVals.set(j,returnVals.get(j) + val.get(j));
			}
		}

		for(int i = 0; i < 17; i++) {
			returnVals.set(i,returnVals.get(i)/((double)(max-min)));
		}

		System.out.println("**, " + Double.parseDouble(args[0]) + ", " + returnVals.get(returnVals.size()-1) + ", " + returnVals.get(0));

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

	public static void main2(String[] args) throws IOException, ParseException {
		BasicSimulator sim = new BasicSimulator();
		double start = System.currentTimeMillis();

		Random r = new Random(68616);

		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server2/game";
		String baseFile = "/u/jberg/finals/day-2/server-2/game";

		//		int min = 1440;
		//		int max = 1441;

		int min = 297;
		int max = 298;
		//				int max = 337;


		//		String baseFile = "/Users/jordanberg/TADAGAMES/MCKP/cslab1a_sim";
		//		int min = 3;
		//		int max = 63;

		//				String baseFile = "/Users/jordanberg/TADAGAMES/EquatePM/cslab2a_sim";
		//				int min = 8;
		//				int max = 63;

		//		String baseFile = "/Users/jordanberg/TADAGAMES/EquatePPS/cslab3a_sim";
		//		int min = 3;
		//		int max = 63;



		//		AbstractAgent agent = new EquatePMSimple(0.797475,1.02,1.525);
		//						AbstractAgent agent = new EquatePRSimple(4.9376,1.02,1.375);
		//				AbstractAgent agent = new EquatePPSSimple(9.9684,1.03,1.375);
		//				AbstractAgent agent = new EquateROISimple(3.9376,1.03,1.525);


		//				AbstractAgent agent = new SemiEndoMCKPBid();
		AbstractAgent agent = new SimpleAABidAgent();
		//		AbstractAgent agent = new NewSemiEndoMCKPBid();
		//				AbstractAgent agent = new EquatePMSimple(0.797475,1.02,1.525);
		//		AbstractAgent agent = new EquatePPSSimple(9.9684,1.03,1.375);



		//		AbstractAgent agent = new DynamicMCKP(.075,7,10,5);
		//		AbstractAgent agent = new DynamicMCKP(.075,7,15,5);
		//		AbstractAgent agent = new DynamicMCKP(0.075,21,15,5);11
		//		AbstractAgent agent = new SemiEndoMCKPBidExhaustive(14, 0.075, 14, 10);
		//		AbstractAgent agent = new SemiEndoMCKPBidExhaustive(21, 0.075, 21, 15);
		//		AbstractAgent agent = new SemiEndoMCKPBidExhaustive(21, 0.075, 14, 10);
		//		AbstractAgent agent = new EquatePM(0.797475, 0.00244287, -0.14746, 0.974011, 0.961644, 0.0164416, 0.251168, 1.01301, 0.887057, 40, 1.74482, 1.54988, 0.976145, 1.05998, 1.91675, 1.8596, 0.00204649, 2.96835);
		//		AbstractAgent agent = new EquatePR(3.67805, 0.00255884, -0.258908, 0.954777, 0.826462, 0.00445251, -0.298878, 0.973704, 0.576163, 2.05602, 1.44894, 1.32086, 1.61395, 0.957746, 1.43458, 1.02511, 0.040663, 1.90321);
		//		AbstractAgent agent = new EquatePR(3.67805, 0.00255884, -0.258908, 0.954777, 0.826462, 0.00445251, -0.298878, 0.973704, 0.576163, 2.05602, .25, .25, .25, 0.957746, 1.43458, 1.02511, 0.040663, 1.90321);
		//		AbstractAgent agent = new EquateROI(3.96915, 0.0090931, -0.11022, 0.955372, 0.711505, 0.0175136, -0.287934, 1.01525, 0.926097, 16.6729, 1.42595, 1.46134, 1.34566, 1.1253, 1.70769, 1.93906, 0.278101, 2.88159);
		//		AbstractAgent agent = new EquatePPS(11.3055, 0.0020563, -0.140335, 0.95, 0.999023, 0.00216053, -0.0519994, 0.952448, 0.686301, 0, 1.1427, 1.03945, 1.28737, 0.940937, 0.833736, 1.40315, 0.383182, 1.33115);

		ArrayList<Double> returnVals = new ArrayList<Double>();
		ArrayList<Double> lowVals = new ArrayList<Double>();
		ArrayList<Double> medVals = new ArrayList<Double>();
		ArrayList<Double> highVals = new ArrayList<Double>();
		ArrayList<Double> allVals = new ArrayList<Double>();

		for(int i = 0; i < 17; i++) {
			returnVals.add(0.0);
		}

		for(int i = min; i < max; i++) {
			sim.setSeed(r.nextLong());
			ArrayList<Double> val = sim.runSimulations(baseFile,i,i+1,0,0, agent);
			for(int j = 0; j < 17; j++) {
				returnVals.set(j,returnVals.get(j) + val.get(j));
			}

			if(val.get(17) == 300) {
				lowVals.add(val.get(0));
			}
			else if(val.get(17) == 400) {
				medVals.add(val.get(0));
			}
			else {
				highVals.add(val.get(0));
			}
		}

		for(int i = 0; i < 17; i++) {
			returnVals.set(i,returnVals.get(i)/((double)(max-min)));
		}

		allVals.addAll(lowVals);
		allVals.addAll(medVals);
		allVals.addAll(highVals);

		double[] stdDevLow = stdDeviation(lowVals.toArray(new Double[lowVals.size()]));
		double[] stdDevMed = stdDeviation(medVals.toArray(new Double[medVals.size()]));
		double[] stdDevHigh = stdDeviation(highVals.toArray(new Double[highVals.size()]));
		double[] stdDevAll = stdDeviation(allVals.toArray(new Double[allVals.size()]));

		System.out.println("FINAL VALUES: " + returnVals);
		System.out.println(stdDevLow[0] + ", " + stdDevLow[1]);
		System.out.println(stdDevMed[0] + ", " + stdDevMed[1]);
		System.out.println(stdDevHigh[0] + ", " + stdDevHigh[1]);
		System.out.println(stdDevAll[0] + ", " + stdDevAll[1]);

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

	public static void main3(String[] args) throws IOException, ParseException {
		BasicSimulator sim = new BasicSimulator();
		double start = System.currentTimeMillis();

		Random r = new Random(68616);

		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server2/game";
		String baseFile = "/u/jberg/finals/day-2/server-2/game";

		//		int min = 1425;
		//		int max = 1465;

		int min = 297;
		//		int max = 301;
		int max = 337;

		/*
		 * Create Gaussian noise for models
		 * 
		 */
		LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
		String[] components = {null,"tv","dvd","audio"};
		String[] manufacturers = {null,"lioneer","flat","pg"};
		for(int i = 0; i < components.length; i++) {
			for(int j = 0; j < manufacturers.length; j++) {
				querySpace.add(new Query(manufacturers[j], components[i]));
			}
		}

		//		AbstractAgent agent = new EquatePMSimple(0.797475,1.02,1.525);
		//						AbstractAgent agent = new EquatePRSimple(4.9376,1.02,1.375);
		//				AbstractAgent agent = new EquatePPSSimple(9.9684,1.03,1.375);
		//				AbstractAgent agent = new EquateROISimple(3.9376,1.03,1.525);
		//				AbstractAgent agent = new SemiEndoMCKPBid(.05,10,10,5);
		//		AbstractAgent agent = new DynamicMCKP(.075,7,10,5);
		//		AbstractAgent agent = new DynamicMCKP(.075,7,15,5);
		//		AbstractAgent agent = new DynamicMCKP(0.075,21,15,5);11
		//		AbstractAgent agent = new SemiEndoMCKPBidExhaustive(14, 0.075, 14, 10);
		//		AbstractAgent agent = new SemiEndoMCKPBidExhaustive(21, 0.075, 21, 15);
		//		AbstractAgent agent = new SemiEndoMCKPBidExhaustive(21, 0.075, 14, 10);
		//		AbstractAgent agent = new EquatePM(0.797475, 0.00244287, -0.14746, 0.974011, 0.961644, 0.0164416, 0.251168, 1.01301, 0.887057, 40, 1.74482, 1.54988, 0.976145, 1.05998, 1.91675, 1.8596, 0.00204649, 2.96835);
		//		AbstractAgent agent = new EquatePR(3.67805, 0.00255884, -0.258908, 0.954777, 0.826462, 0.00445251, -0.298878, 0.973704, 0.576163, 2.05602, 1.44894, 1.32086, 1.61395, 0.957746, 1.43458, 1.02511, 0.040663, 1.90321);
		//		AbstractAgent agent = new EquatePR(3.67805, 0.00255884, -0.258908, 0.954777, 0.826462, 0.00445251, -0.298878, 0.973704, 0.576163, 2.05602, .25, .25, .25, 0.957746, 1.43458, 1.02511, 0.040663, 1.90321);
		//		AbstractAgent agent = new EquateROI(3.96915, 0.0090931, -0.11022, 0.955372, 0.711505, 0.0175136, -0.287934, 1.01525, 0.926097, 16.6729, 1.42595, 1.46134, 1.34566, 1.1253, 1.70769, 1.93906, 0.278101, 2.88159);
		//		AbstractAgent agent = new EquatePPS(11.3055, 0.0020563, -0.140335, 0.95, 0.999023, 0.00216053, -0.0519994, 0.952448, 0.686301, 0, 1.1427, 1.03945, 1.28737, 0.940937, 0.833736, 1.40315, 0.383182, 1.33115);

		ArrayList<Double> returnVals = new ArrayList<Double>();
		ArrayList<Double> lowVals = new ArrayList<Double>();
		ArrayList<Double> medVals = new ArrayList<Double>();
		ArrayList<Double> highVals = new ArrayList<Double>();
		ArrayList<Double> allVals = new ArrayList<Double>();

		for(int i = 0; i < 17; i++) {
			returnVals.add(0.0);
		}

		//		double noisePrClick = 0.3;
		//		double noiseCPC = 0.0;
		//		double noiseConvPr = 0.0;

		double noisePrClick = Double.parseDouble(args[0]);
		double noiseCPC = Double.parseDouble(args[1]);
		double noiseConvPr = Double.parseDouble(args[2]);

		for(int i = min; i < max; i++) {
			HashMap<Query,LinkedList<Double>> prClickNoise = new HashMap<Query, LinkedList<Double>>();
			HashMap<Query,LinkedList<Double>> CPCNoise = new HashMap<Query, LinkedList<Double>>();
			HashMap<Query,LinkedList<Double>> convPrNoise = new HashMap<Query, LinkedList<Double>>();
			LinkedList<Double> prClickNoiseList = new LinkedList<Double>();
			LinkedList<Double> CPCNoiseList = new LinkedList<Double>();
			LinkedList<Double> convPrNoiseList = new LinkedList<Double>();
			for(int days = 0; days < 61; days++) {
				prClickNoiseList.add(r.nextGaussian()*noisePrClick);
				CPCNoiseList.add(r.nextGaussian()*noiseCPC);
				convPrNoiseList.add(r.nextGaussian()*noiseConvPr);
			}
			for(Query q : querySpace) {
				prClickNoise.put(q, prClickNoiseList);
				CPCNoise.put(q, CPCNoiseList);
				convPrNoise.put(q, convPrNoiseList);
			}

			AbstractAgent agent = new DynamicMCKP(prClickNoise,CPCNoise,convPrNoise);

			sim.setSeed(r.nextLong());
			ArrayList<Double> val = sim.runSimulations(baseFile,i,i+1,0,0, agent);
			for(int j = 0; j < 17; j++) {
				returnVals.set(j,returnVals.get(j) + val.get(j));
			}

			if(val.get(17) == 300) {
				lowVals.add(val.get(0));
			}
			else if(val.get(17) == 400) {
				medVals.add(val.get(0));
			}
			else {
				highVals.add(val.get(0));
			}
		}

		for(int i = 0; i < 17; i++) {
			returnVals.set(i,returnVals.get(i)/((double)(max-min)));
		}

		allVals.addAll(lowVals);
		allVals.addAll(medVals);
		allVals.addAll(highVals);

		double[] stdDevLow = stdDeviation(lowVals.toArray(new Double[lowVals.size()]));
		double[] stdDevMed = stdDeviation(medVals.toArray(new Double[medVals.size()]));
		double[] stdDevHigh = stdDeviation(highVals.toArray(new Double[highVals.size()]));
		double[] stdDevAll = stdDeviation(allVals.toArray(new Double[allVals.size()]));

		System.out.println("FINAL VALUES: " + returnVals);
		System.out.println(stdDevLow[0] + ", " + stdDevLow[1]);
		System.out.println(stdDevMed[0] + ", " + stdDevMed[1]);
		System.out.println(stdDevHigh[0] + ", " + stdDevHigh[1]);
		System.out.println(stdDevAll[0] + ", " + stdDevAll[1]);

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

	public void emptyFunction() {}

}