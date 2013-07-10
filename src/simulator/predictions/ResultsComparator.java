/**
 * 
 */
package simulator.predictions;

import java.util.Comparator;

/**
 * @author betsy
 *
 */
public class ResultsComparator implements Comparator<ResultValues> {

	/**
	 * 
	 */
	String value; 
	public ResultsComparator(String value) {
		this.value = value;
	}

	@Override
	public int compare(ResultValues result1, ResultValues result2) {
		
		if(value.compareToIgnoreCase("stat")==0){
			if(result1.getStat()>result2.getStat()){
				return 1;
			}else if(result1.getStat()<result2.getStat()){
				return -1;
			}else{
				return 0;
			}
		}else if(value.compareToIgnoreCase("absError")==0) {
			if(result1.getAbsError()>result2.getAbsError()){
				return 1;
			}else if(result1.getAbsError()<result2.getAbsError()){
				return -1;
			}else{
				return 0;
			}
		}else {
			if(result1.getTime()>result2.getTime()){
				return 1;
			}else if(result1.getTime()<result2.getTime()){
				return -1;
			}else{
				return 0;
			}
		}
	}

}
