//
//	created:2020.12.21
//	author:why


simple Node{
    parameters :
   	    double x = (uniform(0,10));
    	double y = (uniform(0,10));
   	    
   	    @display("p=$x,$y;i=device/pocketpc_s,black");
        
   	gates:
   	    input in[];
   	    output out[];   	
}
