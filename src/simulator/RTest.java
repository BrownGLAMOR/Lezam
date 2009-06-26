package simulator;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;


public class RTest {

	/**
	 * @param args
	 * @throws RserveException 
	 * @throws REXPMismatchException 
	 * @throws RSrvException 
	 */
	public static void main(String[] args) throws RserveException, REXPMismatchException {
		RConnection c = new RConnection();
		
		double start = System.currentTimeMillis();
		REXP mean = c.eval("sd(rnorm(100000))");
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
		
		System.out.println(mean.asDouble());

	}

}
