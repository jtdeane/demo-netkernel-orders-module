package cogito.online.ordering;

import org.netkernel.layer0.nkf.INKFAsyncRequestHandle;
import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.xml.xda.DOMXDA;
import org.w3c.dom.NodeList;

import cogito.infrastructure.AccessorUtility;


/**
 * Handles processing of taxed order resource
 */
public class TaxedOrdersAccessor extends StandardAccessorImpl {
	
	/**
     * Default constructor
     */
	public TaxedOrdersAccessor() {
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
		
		Double ordersTotal = 0.0;
		
		//for each order, issue an async request
		for (int i=0; i < ordersNodes.getLength(); i++) {
            DOMXDA orderDOM = new DOMXDA(ordersDOMXDA.getFragment(
            		ordersNodes.item(i)),false);
            
            String orderID = orderDOM.getText("/order/@id", true);
            
            //issue sub-request - request-response (async under c
            INKFRequest subRequest = context.createRequest
            	("res:/cogito/online/taxed/order/" + orderID);
            
            subRequest.setVerb(context.getThisRequest().getVerb());
            subRequest.addPrimaryArgument(orderDOM);
            
    		/*
    		 * issue sync request; under the covers this is an asyn 
    		 * request-response via the kernel.
    		 */
            Double orderTotal = (Double)context.issueRequest(subRequest);
            ordersTotal = ordersTotal + orderTotal;
		}
		
		AccessorUtility.returnMessage(context, "Total Batch Charges: $" 
				+ ordersTotal.intValue());
	}
	
	/**
	 * Handle Order Request
	 * @param context
	 * @throws Exception
	 */
	private void handleOrder(INKFRequestContext context) throws Exception {
		
		DOMXDA orderDOMXDA = (DOMXDA )context.getThisRequest().getPrimary();
		
		//1. lookup the price, tax and discount in parallel 
        String item = orderDOMXDA.getText("/order/@item", true);
        String customer = orderDOMXDA.getText("/order/@customer", true);
        
        INKFAsyncRequestHandle priceHandle = requestPrice(context, item);
        INKFAsyncRequestHandle discountHandle = requestDiscount(context, item);
        INKFAsyncRequestHandle taxHandle = requestTax(context, customer);  
        
        //2. Join back on the price request (within 200ms) and calculate base charge
        Object price = priceHandle.join(200);
        
        //price request took too long
        if (price == null) {
        	throw new Exception ("Unable to retrieve price for " + item);
        }
        
        Double subTotal = (Double)price * new Double(orderDOMXDA.getText
        		("/order/@amount", true));
        
        //3. Join back on the discount request within 200ms and apply discount
        Object discount = discountHandle.join(200);
        
        //discount request took too long
        if (discount == null) {
        	throw new Exception ("Unable to retrieve discount for " + item);
        }
        
        Double discountedTotal = 0.00;
        
        //discount gets applied to subTotal
        if ((discount != null) && ((Double)discount > 0)) {		
        	discountedTotal = subTotal * (1-(Double)discount);
        } else { 	
        	discountedTotal = subTotal;
        }
        
        //4. Join back on the tax request within 200ms and calculate the tax
        Object tax = taxHandle.join(200);
        
        //tax request took too long
        if (tax == null) {
        	throw new Exception ("Unable to retrieve tax for " + customer);
        }
        
        Double taxedTotal = discountedTotal * (1 + (Double)tax);
        
        mockResourceProcessingTime (new Integer (orderDOMXDA.getText
        		("/order/@amount", true)).intValue());
        
        //5. log the order summary and return the charged amount
    	logOrderSummary(context, orderDOMXDA.getText("/order/@id", true), tax, 
    			discount, subTotal, discountedTotal, taxedTotal);
    	
    	context.createResponseFrom(taxedTotal);
	}
	
	/**
	 * Request the price for an item
	 * @param context
	 * @param item
	 * @return INKFAsyncRequestHandle
	 * @throws Exception
	 */
	private INKFAsyncRequestHandle requestPrice (INKFRequestContext context, 
			String item) throws Exception {

        String priceURI = "res:/cogito/online/price/" 
        	+ item.replaceAll("\\W","");
        
        return context.issueAsyncRequest (context.createRequest(priceURI));
	}
	
	/**
	 * Request the discount for an item
	 * @param context
	 * @param item
	 * @return INKFAsyncRequestHandle
	 * @throws Exception
	 */
	private INKFAsyncRequestHandle requestDiscount (INKFRequestContext context, 
			String item) throws Exception {

        String priceURI = "res:/cogito/online/discount/" + item.replaceAll("\\W","");
        
        return context.issueAsyncRequest (context.createRequest(priceURI));
	}
	
	/**
	 * Request the tax for an item
	 * @param context
	 * @param customer
	 * @return INKFAsyncRequestHandle
	 * @throws Exception
	 */
	private INKFAsyncRequestHandle requestTax (INKFRequestContext context, 
			String customer) throws Exception {
		
        String customerURI ="res:/cogito/online/tax/" + customer;
        
        return context.issueAsyncRequest(context.createRequest(customerURI));	
	}

	/**
	 * Log the processed order information
	 * @param context
	 * @param orderID
	 * @param tax
	 * @param discount
	 * @param subTotal
	 * @param taxedTotal
	 * @param charged
	 */
	private void logOrderSummary(INKFRequestContext context, String orderID,
			Object tax, Object discount, Double subTotal,
			Double discountedTotal, Double taxedTotal) {
		
		StringBuffer output = new StringBuffer("ORD ");
		
		output.append(orderID);
		output.append(" ST $" + subTotal.intValue());
		
		if (discount != null) {
			output.append(" DISC " + ((Double)discount) + "%");
			output.append(" AT $" + discountedTotal.intValue());
		}
		
		output.append(" TX " + ((Double)tax) + "%");
		output.append(" TXT $" + taxedTotal.intValue());
		
		context.logRaw(INKFRequestContext.LEVEL_DEBUG, output.toString());
	}
	
	/**
	 * Mock resource processing time. While the Fibonacci is used here to 
	 * mock resource processing the calculation can be optimzed using a
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