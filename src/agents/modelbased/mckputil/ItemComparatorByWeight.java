package agents.modelbased.mckputil;

import java.util.Comparator;

public class ItemComparatorByWeight implements Comparator<Item>{
	/*
	 * sorts by weight in INCREASING order
	 */
	public int compare(Item i1, Item i2) {
		if(Double.isNaN(i1._weight) || Double.isNaN(i2._weight)){
			if(Double.isNaN(i1._weight) && Double.isNaN(i2._weight)){
				return 0;
			} else {
				if(Double.isNaN(i1._weight)){
					return -1;
				}
				else {
					return 1;
				}
			}
		}
		if(i1.w() > i2.w()) {
			return 1;
		}else if(i1.w() == i2.w()) {
			return 0;
		}else if(i1.w() < i2.w()) {
			return -1;
		}else{
			return 0;
		}
	}
}
