package cogito.online.registration;

import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * Handles Showmanship Calculation Resource Requests
 * @author jeremydeane
 *
 */
public class ShowmanshipAccessor extends StandardAccessorImpl {
	
	/**
     * Default constructor
     */
	public ShowmanshipAccessor() {	
		this.declareThreadSafe();
	}
	
	/**
	 * Mock Method for creating a Showmanship Calculation Resource
	 * @param context
	 * @throws Exception
	 */
	@Override
	public void onNew(INKFRequestContext context) throws Exception {
		
		mockResourceProcessingTime(12);
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, "Calculated Showmanship");
	}	
	
	/**
	 * Mock resource processing time. While the Fibonacci is used here to 
	 * mock resource processing, the calculation can be optimzed using a
	 * resource oriented approach. See http://1060.org/upload/fibonacci.html
	 * @param integer
	 */
	private void mockResourceProcessingTime(int integer) {
		if (integer <= 40) {
			fibonacci(integer*4);
		} else {
			fibonacci(40);
		}
	}
	
	/**
	 * Calculate Fibonacio
	 * @param n
	 * @return int
	 */
	private int fibonacci(int n) {
		if (n < 2) {
			return n;
		} else {
			return fibonacci(n-1)+fibonacci(n-2);
		}
	}	
}