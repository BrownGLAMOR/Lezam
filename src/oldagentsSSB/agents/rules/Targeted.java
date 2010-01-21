package oldagentsSSB.agents.rules;

import oldagentsSSB.strategies.GenericBidStrategy;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;

/*
 * sets the ad as targeted
 */

public class Targeted extends StrategyTransformation{
	private Product _product;
	
	public Targeted(Product product){
		_product = product;
	}
	
	public Targeted(){
		this(null);
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		if(_product != null){
			strategy.setQueryAd(q, new Ad(_product));
		}
		else {
			strategy.setQueryAd(q, new Ad(new Product(q.getManufacturer(), q.getComponent())));
		}
		
	}
	
}
