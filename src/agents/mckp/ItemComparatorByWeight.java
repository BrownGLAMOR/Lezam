package agents.mckp;

import java.util.Comparator;

import props.Misc;

public class ItemComparatorByWeight implements Comparator<Item>{
	/*
	 * sorts by weight in INCREASING order
	 */
	public int compare(Item i1, Item i2) {
		if(i1.w() > i2.w()) {
			return 1;
		}else if(i1.w() == i2.w()) {
			return 0;
		}else if(i1.w() < i2.w()) {
			return -1;
		}else{
			Misc.myassert(false);
			return 0;
		}
	}
}
