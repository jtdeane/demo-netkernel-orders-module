package cogito.online.registration;

import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.xml.xda.DOMXDA;

import cogito.infrastructure.AccessorUtility;

/**
 * Handles requests for magician registration
 * @author jeremydeane
 */
public class RegistrationAccessor extends StandardAccessorImpl {

	/**
     * Default constructor
     */
	public RegistrationAccessor() {	
		this.declareThreadSafe();
	}
	
	/**
	 * Handles requests for magician registration
	 */
	@Override
	public void onSource(INKFRequestContext context) throws Exception {
		
		//
		DOMXDA domXDA = AccessorUtility.extractDocumentFromHTTPBody(context);
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, "\n" + domXDA.toString());			
		
		//determine the type of registration from URI parameter
        String type = context.getThisRequest().getArgumentValue ("registrationType");
        
        //handle request synchronously...
        if (type.equals("synchronous")) {
        	
        	synchronousRegistration (context);
        	
        } else {
        	
        	//data decomposition
        	asynchronousRegistration(context);
        	
        }
        
		AccessorUtility.returnMessage(context, "Processing Registration");		
	}
	
	/**
	 * Synchronous Registration
	 * @param context
	 * @throws exception
	 */
	private void synchronousRegistration (INKFRequestContext context) throws Exception {
		
		createAccount (context);
		
		calculateMagicalPower (context);
		
		calculateShowmanship (context);
	}
	
	/**
	 * Mock method for creating account
	 * @param context
	 * @throws Exception
	 */
	private void createAccount (INKFRequestContext context) throws Exception {
		
		mockResourceProcessingTime(5);
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, "Created Account");	
	}
	
	/**
	 * Mock method for calculating magical power
	 * @param context
	 * @throws Exception
	 */
	private void calculateMagicalPower (INKFRequestContext context) throws Exception {
		
		mockResourceProcessingTime(12);
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, "Calculated Magical Power");	
	}
	
	/**
	 * Mock method for calculating showmanship power
	 * @param context
	 * @throws Exception
	 */
	private void calculateShowmanship (INKFRequestContext context) throws Exception {
		
		mockResourceProcessingTime(12);
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, "Calculated Showmanship");
	}
	
	/**
	 * Asynchronous Registration
	 * @param context
	 * @throws Exception
	 */	
	private void asynchronousRegistration  (INKFRequestContext context) throws Exception {
		
		createAccount (context);
		
        //issue sub-request - Magical Power Calculation
        INKFRequest magicalSubrequest = context.createRequest
        		("res:/cogito/calculation/magic");
        
        magicalSubrequest.setVerb(INKFRequestReadOnly.VERB_NEW);
        
        context.issueAsyncRequest(magicalSubrequest);
		
		
        //issue sub-request - Showmanship Calculation
        INKFRequest showmanshipSubrequest = context.createRequest
        		("res:/cogito/calculation/showmanship");
        
        showmanshipSubrequest.setVerb(INKFRequestReadOnly.VERB_NEW);
        
        context.issueAsyncRequest(showmanshipSubrequest);
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