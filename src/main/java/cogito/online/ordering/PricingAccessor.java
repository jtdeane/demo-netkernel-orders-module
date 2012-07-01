package cogito.online.ordering;

import java.util.concurrent.ConcurrentHashMap;

import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;


/**
 * Provides access to the pricing resources
 */
public class PricingAccessor extends StandardAccessorImpl {
	
	//pricing lookup Data
    private ConcurrentHashMap<String, Double> prices = 
        new ConcurrentHashMap<String, Double>();
    
    //discount lookup data
    private ConcurrentHashMap<String, Double> discounts = 
        new ConcurrentHashMap<String, Double>();
    
    //tax lookup data
    private ConcurrentHashMap<String, Double> taxes = 
        new ConcurrentHashMap<String, Double>();
	
	/**
     * Default constructor
     */
	public PricingAccessor() {
		
		populateLookupData();
		
		this.declareThreadSafe();
	}
	
	/**
	 * 
	 * Handle requests for pricing information
	 * @param context
	 * @throws Exception
	 */
	public void onSource(INKFRequestContext context) throws Exception {
        
		//price request
		if (context.getThisRequest().argumentExists("priceID")) {
			
			//price identifier is extracted by the grammar defined in the module.xml
	        String priceID = context.getThisRequest().getArgumentValue("priceID");
	        
            context.createResponseFrom(prices.get(priceID));

		} else if (context.getThisRequest().argumentExists("customer")) {
			
			//customer is extracted by the grammar defined in the module.xml
	        String customer = context.getThisRequest().getArgumentValue("customer");
	        
            context.createResponseFrom(taxes.get(customer));
            
		} else {
			
			//discount identifier is extracted by the grammar defined in the module.xml
	        String discountID = context.getThisRequest().getArgumentValue("discountID");
	        
	        Double discount = discounts.get(discountID);
	        
	        if (discount == null) {
	        	discount = 0.00;
	        }
	        
            context.createResponseFrom(discount);
		}
	}
	
	/**
	 * Populate the lookup data
	 */
	private void populateLookupData () {
		
		//set prices		
		prices.put("Dice", 10.00);
		prices.put("CardDeck", 6.00);
		prices.put("Rings", 12.00);
		prices.put("Quarters", 6.00);
		prices.put("Marbles",8.00);
		prices.put("Rabbit", 45.00);
		prices.put("TopHat", 100.00);
		prices.put("RainbowScarf", 15.00);
		
		//set discounts
		discounts.put("Rings", .10);
		discounts.put("Quarters", .15);
		discounts.put("Marbles", .50);
		discounts.put("Rabbit", .5);
		discounts.put("TopHat", .25);
		
		//set taxes
		taxes.put("Copperfield", .06); //LA
		taxes.put("Houdini", .07); //NY
		taxes.put("Blain", .06); //LA
		taxes.put("Teller", .04); //NV
		taxes.put("Blackstone", .07); //NY
		taxes.put("Angel", .07); //NY;
		taxes.put("Harary", .06); //LA
		taxes.put("Calvert", .06); //LA
		taxes.put("Turner", .07); //NY
		taxes.put("Palmer", .06); //LA
		taxes.put("Burton", .04); //NV
		taxes.put("Klok", .07); //NY
		taxes.put("Penn", .04); //NV
	}
}