package ch.uzh.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

public class App 
{

	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) {
        // System.out.println( "Hello World!" );
        HFClient client = HFClient.createNewInstance();
	    try {
	      client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
	    } catch (CryptoException ex) {
	      LOGGER.error("CryptoException setting crypto suite", ex);
	    } catch (InvalidArgumentException ex) {
	      LOGGER.error("InvalidArgumentException setting crypto suite", ex);
	    }
    }
}
