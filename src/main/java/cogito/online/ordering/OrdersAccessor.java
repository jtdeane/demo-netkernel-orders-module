package cogito.online.ordering;

import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.xml.xda.DOMXDA;
import org.w3c.dom.NodeList;

import cogito.infrastructure.AccessorUtility;

/**
 * Handles processing of order resources
 */
public class OrdersAccessor extends StandardAccessorImpl {
	
	/**
     * Default constructor
     */
	public OrdersAccessor() {	
		this.declareThreadSafe();
	}
	
	@Override
	public void onSource(INKFRequestContext context) throws Exception {
		
		//process batch
		if (context.getThisRequest().argumentExists("batchID")) {
			handleBatch(context);	
		} else {		
			//process individual order
			handleOrder(context);
		}
	}
	
	/**
	 * Handle Batch Request
	 * @param context
	 * @throws Exception
	 */
	private void handleBatch(INKFRequestContext context) throws Exception {
		
		DOMXDA ordersDOMXDA = AccessorUtility.extractDocumentFromHTTPBody(context);
			
		NodeList ordersNodes = ordersDOMXDA.getNodeList("/orders/order");
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, "Started processing batch of " 
				+ ordersNodes.getLength() + " orders");
		
		//for each order, issue an async request
		for (int i=0; i < ordersNodes.getLength(); i++) {
            
			DOMXDA orderDOM = new DOMXDA(ordersDOMXDA.getFragment(
            		ordersNodes.item(i)),false);
            
            String orderID = orderDOM.getText("/order/@id", true);
            
            //issue sub-request - Fire and Forget
            INKFRequest subRequest = context.createRequest
            	("res:/cogito/online/order/" + orderID);
            
            subRequest.setVerb(context.getThisRequest().getVerb());
            subRequest.addPrimaryArgument(orderDOM);
            
            context.issueAsyncRequest(subRequest); 
		}
		
		AccessorUtility.returnMessage(context, "Processing Order");
	}
	
	/**
	 * Handle Order Request
	 * @param context
	 * @throws Exception
	 */
	private void handleOrder(INKFRequestContext context) throws Exception {
		
		DOMXDA orderDOMXDA = (DOMXDA )context.getThisRequest().getPrimary();
		
		//1. lookup the price
        String item = orderDOMXDA.getText("/order/@item", true);
        
        Double price = retrievePricingInformation(context, 
        		"res:/cogito/online/price/" + item.replaceAll("\\W",""));
        
        //2. calculate the base charge
        Double subTotal = price * new Double(orderDOMXDA.getText
        		("/order/@amount", true)) ;
        
        //3. lookup discount and apply
        Double discount = retrievePricingInformation(context, 
        		"res:/cogito/online/discount/" + item.replaceAll("\\W",""));
        
        Double charged=0.00;
        
        if (discount > 0) {		
        	charged = subTotal * (1-discount);
        } else { 	
        	charged = subTotal;
        }
        
        mockResourceProcessingTime (new Integer (orderDOMXDA.getText
        		("/order/@amount", true)).intValue());
        
    	logOrderSummary(context, orderDOMXDA.getText("/order/@id", true), 
    			price, discount, subTotal, charged);  
	}
	
	/**
	 * Retrieve pricing information (item price or discount)
	 * @param context
	 * @param uri
	 * @return Double
	 * @throws Exception
	 */
	private Double retrievePricingInformation (INKFRequestContext context, 
			String uri) throws Exception {
		
		INKFRequest request = context.createRequest(uri);
		
		/*
		 * issue sync request; under the covers this is an asyn request-response
		 * via the kernel.
		 */
		return (Double) context.issueRequest(request);
	}
	
	/**
	 * Log the processed order information
	 * @param context
	 * @param orderID
	 * @param price
	 * @param discount
	 * @param subTotal
	 * @param charged
	 */
	private void logOrderSummary(INKFRequestContext context, String orderID,
			Double price, Double discount, Double subTotal, Double charged) {
		
		StringBuffer output = new StringBuffer(Thread.currentThread().getName());
		
		output.append(" ORD " + orderID);
		output.append(" PRI $" + price.intValue());
		output.append(" ST $" + subTotal.intValue());
		
		if (discount != null) {
			output.append(" DISC " + discount + "%");
		}
		output.append(" T $" + charged.intValue());
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, output.toString());
	}
	
	/**
	 * Mock resource processing time. While the Fibonacci is used here to 
	 * mock resource processing, the calculation can be optimzed using a
	 * resource oriented approach. See http://1060.org/upload/fibonacci.html
	 * @param price
	 */
	private void mockResourceProcessingTime(int amount) {
		
		if (amount <= 40) {
			fibonacci(amount*4);
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