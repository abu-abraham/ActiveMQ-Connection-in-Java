

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureSocketUtil {
	
	private final static Logger logger = LoggerFactory.getLogger(SecureSocketUtil.class);
	
	 
    private static KeyStore getKeyStoreInstance(final String type)
	{			
		if (type == null || "Default".equals(type))
		{
			try {
				return KeyStore.getInstance(KeyStore.getDefaultType());
			} catch (KeyStoreException e) {
				logger.error("Failed to read key store (only .ks files supported)");
			}
		}	
		
		return null;
	}

	private static KeyStore loadKeystore(final String keyStoreFile, final String keyStorePassword, final String keyStoreType) {
		try {
			final FileInputStream inputStream = new FileInputStream(keyStoreFile);

			KeyStore keyStore = null;

			try {
				keyStore = getKeyStoreInstance(keyStoreType);
				keyStore.load(inputStream, keyStorePassword.toCharArray());
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}

			}
			return keyStore;
		} catch (Exception e) {
			logger.error("Error in loadin keystore, make sure password is correct");
		}
		return null;

	}
    
	public static SSLSocketFactory getSocketFactory()
	{
		String protocolVersion = "SSLv3";
		String caKeyStoreFile = PropertyFetcher.getValue("caKeyStoreFile");
		String clientKeyStoreFile = PropertyFetcher.getValue("clientKeyStoreFile");
		String clientKeyPassword= PropertyFetcher.getValue("clientKeyPassword");
		String caKeyStorePassword = PropertyFetcher.getValue("caKeyStorePassword");
		String clientKeyStorePassword = PropertyFetcher.getValue("clientKeyStorePassword");
		try {
			Security.addProvider(new BouncyCastleProvider());
			final KeyStore keyStore = loadKeystore(clientKeyStoreFile, clientKeyStorePassword, "Default");			
			
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, clientKeyPassword.toCharArray());
			
			
			final KeyManager[] km = kmf.getKeyManagers();
			
			final KeyStore keyStore1 = loadKeystore(caKeyStoreFile, caKeyStorePassword, "Default");
			
			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore1);
			final TrustManager[] tm = tmf.getTrustManagers();
			
			Security.addProvider(new BouncyCastleProvider());
			
			// Create SSL/TLS socket factory
			final SSLContext context = SSLContext.getInstance(protocolVersion);
			
			context.init(km, tm, null);
	
			return context.getSocketFactory();
			
			} 
		catch(Exception e) {
			logger.error("Failed to get SSLSocket factory " + e.getMessage());
		}
		return null;
			
				
	}
}
