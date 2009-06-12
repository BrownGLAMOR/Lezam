package newmodels.prconv;

import agents.Pair;
import usermodel.UserState;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class TrinaryPrConversion extends AbstractPrConversionModel{

	private double _lambda;
	private double _baselineConv;
	private double _componentBonus;
	public enum GetsBonus {YES,NO,MAYBE};
	private GetsBonus _getBonus;
	protected UserState _userState;
	protected Product _product;

	public TrinaryPrConversion(Product product, UserState us, double lambda, String componentSpeciality, double componentBonus) {
		super(new Query());
		_getBonus = GetsBonus.MAYBE;
		_lambda = lambda;
		_componentBonus = componentBonus;
		_userState = us;
		_product = product;
		String component = product.getComponent();
		if(us.equals(UserState.F0)) {
			_baselineConv = .1;
		}
		else {
			if(us.equals(UserState.F1)) {
				_baselineConv = .2;
				if (componentSpeciality.equals(component)) {
					_getBonus = GetsBonus.YES;
				} else if (component == null) _getBonus = GetsBonus.NO;
			} else if(us.equals(UserState.F2)) {
				_baselineConv = .3;
				if (componentSpeciality.equals(component)) _getBonus = GetsBonus.YES;
			} else if (us.equals(UserState.IS)){
				_baselineConv = 0.0;
			} else {
				throw new RuntimeException("Malformed query");
			}	
		}		
	}

	@Override
	public double getPrediction(double overcap) {
		double capdiscount = Math.pow(_lambda,Math.max(overcap, 0));
		double firstTerm = _baselineConv * capdiscount;
		double secondTerm = 1 + _componentBonus;
		double nuo = (firstTerm * secondTerm) / (firstTerm * secondTerm + (1 - firstTerm));
		
		if (_getBonus.equals(GetsBonus.YES)) return nuo;
		if (_getBonus.equals(GetsBonus.NO)) return firstTerm;
		
		return (nuo + 3*firstTerm)/4;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public Pair<Product,UserState> getPair() {
		Pair<Product,UserState> pus = new Pair<Product, UserState>(_product, _userState);
		return pus;
	}

}
