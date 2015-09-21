/* Copyright 2010,2014 Bank Of Italy
*
* Licensed under the EUPL, Version 1.1 or - as soon they
* will be approved by the European Commission - subsequent
* versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the
* Licence.
* You may obtain a copy of the Licence at:
*
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in
* writing, software distributed under the Licence is
* distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied.
* See the Licence for the specific language governing
* permissions and limitations under the Licence.
*/
package it.bancaditalia.oss.sdmx.client;


import it.bancaditalia.oss.sdmx.api.GenericSDMXClient;
import it.bancaditalia.oss.sdmx.util.Configuration;
import it.bancaditalia.oss.sdmx.util.SdmxException;
import it.bancaditalia.oss.sdmx.util.SdmxProxySelector;

import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Java Factory class for creating the Sdmx Clients.
 *
 * @author Attilio Mattiocco
 *
 */
public class SDMXClientFactory {

	private static final String ECB_PROVIDER = "http://sdw-wsrest.ecb.europa.eu/service";
	private static final String EUROSTAT_PROVIDER = "http://ec.europa.eu/eurostat/SDMX/diss-web/rest";
	private static final String ISTAT_PROVIDER = "http://sdmx.istat.it/SDMXWS/rest";
	private static final String INSEE_PROVIDER = "http://www.bdm.insee.fr/series/sdmx";

	//read the configuration file
	static {
		providers = new HashMap<String, Provider>();
		Configuration.init();
		logger = Configuration.getSdmxLogger();
		initBuiltInProviders();
		initExternalProviders();
	}

	private static final String sourceClass = SDMXClientFactory.class.getSimpleName();
	protected static Logger logger;
	private static Map<String, Provider> providers;


	/**
     * Initialize the internal sdmx providers
     *
     */
	private static void initBuiltInProviders(){
        addBuiltInProvider("ECB", ECB_PROVIDER, false, false, true, "European Central Bank");
        addBuiltInProvider("EUROSTAT", EUROSTAT_PROVIDER, false, false, false, "Eurostat");
        addBuiltInProvider("ISTAT", ISTAT_PROVIDER, false, false, false, "Istituto nazionale di statistica");
        addBuiltInProvider("INSEE", INSEE_PROVIDER, false, false, true, "National Institute of Statistics and Economic Studies");


	    //add internal 2.0 providers
	    addBuiltInProvider("OECD", null, false, false, false, "The Organisation for Economic Co-operation and Development");
	    addBuiltInProvider("OECD_RESTR", null, true, false, false, "The Organisation for Economic Co-operation and Development, RESTRICTED ACCESS");
	    addBuiltInProvider("ILO", null, false, false, false, "International Labour Organization");
	    addBuiltInProvider("IMF", null, false, false, false, "International Monetary Fund");
	    addBuiltInProvider("INEGI", null, false, false, false, "Instituto Nacional de Estadistica y Geografia");
	    addBuiltInProvider("ABS", null, false, false, false, "Australian Bureau of Statistics");
	    addBuiltInProvider("WB", null, false, false, false, "World Bank (BETA provider)");
	    addBuiltInProvider("NBB", null, false, false, false, "National Bank Belgium");
	    addBuiltInProvider("UIS", null, false, false, false, "Unesco Institute for Statistics");

    	//Legacy 2.0
    	ServiceLoader<GenericSDMXClient> ldr = ServiceLoader.load(GenericSDMXClient.class);
        for (GenericSDMXClient provider : ldr) {
            addProvider(provider.getClass().getSimpleName(), null, provider.needsCredentials(), false, false, provider.getClass().getSimpleName());
        }
	}
	
	/**
     * Initialize the sdmx providers from the configuration file
     */
	private static void initExternalProviders(){
	    //external providers set in the configuration file
	    String external = Configuration.getExternalProviders();
	    if(external != null && !external.isEmpty()){
	    	String[] ids = external.trim().split("\\s*,\\s*");
	    	for (int i = 0; i < ids.length; i++) {
				addExternalProvider(ids[i]);
			}
	    }
	}

	/**
     * General method for creating an SdmxClient.
     *
	 * @param name
	 * @param endpoint
	 * @param needsCredentials
	 * @param needsURLEncoding
	 * @param supportsCompression
	 * @param description
	 */
	public static void addProvider(String name, URL endpoint, boolean needsCredentials, boolean needsURLEncoding, boolean supportsCompression, String description){
		Provider p = new Provider(name, endpoint, needsCredentials, needsURLEncoding, supportsCompression, description);
    	providers.put(name, p);
	}

    /**
     * Add a builtin provider and check whether the default values need to be overwritten with values defined in the configuration file.
     */
    private static void addBuiltInProvider(final String name, final String endpoint, final Boolean needsCredentials, final Boolean needsURLEncoding, final Boolean supportsCompression, final String description) {
        try {
            final String providerName = Configuration.getConfiguration().getProperty("providers." + name + ".name", name);
            final String providerEndpoint = Configuration.getConfiguration().getProperty("providers." + name + ".endpoint", endpoint);
            final URL providerURL = null != providerEndpoint ? new URL(providerEndpoint) : null;
            final boolean provdiderNeedsCredentials = Boolean.parseBoolean(Configuration.getConfiguration().getProperty("providers." + name + ".needsCredentials", needsCredentials.toString()));
            final boolean providerNeedsURLEncoding = Boolean.parseBoolean(Configuration.getConfiguration().getProperty("providers." + name + ".needsURLEncoding", needsURLEncoding.toString()));
            final boolean providerSupportsCompression = Boolean.parseBoolean(Configuration.getConfiguration().getProperty("providers." + name + ".supportsCompression", supportsCompression.toString()));
            final String providerDescription = Configuration.getConfiguration().getProperty("providers." + name + ".description", description);
            addProvider(providerName, providerURL, provdiderNeedsCredentials, providerNeedsURLEncoding, providerSupportsCompression, providerDescription);
        } catch (final MalformedURLException e) {
            logger.log(Level.SEVERE, "Exception. Class: {0} .Message: {1}", new Object[]{e.getClass().getName(), e.getMessage()});
            logger.log(Level.FINER, "", e);
        }
    }

    /**
     * Add a external provider and check whether the default values need to be overwritten with values defined in the configuration file.
     */
    private static void addExternalProvider(final String id) {
        try {
            final String providerName = Configuration.getConfiguration().getProperty("providers." + id + ".name", id);
            final String providerEndpoint = Configuration.getConfiguration().getProperty("providers." + id + ".endpoint");
            if(providerEndpoint != null && !providerEndpoint.isEmpty()){
            	final URL providerURL = new URL(providerEndpoint);
		        final boolean provdiderNeedsCredentials = Boolean.parseBoolean(Configuration.getConfiguration().getProperty("providers." + id + ".needsCredentials", "false"));
		        final boolean providerNeedsURLEncoding = Boolean.parseBoolean(Configuration.getConfiguration().getProperty("providers." + id + ".needsURLEncoding", "false"));
		        final boolean providerSupportsCompression = Boolean.parseBoolean(Configuration.getConfiguration().getProperty("providers." + id + ".supportsCompression", "false"));
		        final String providerDescription = Configuration.getConfiguration().getProperty("providers." + id + ".description", id);
		        addProvider(providerName, providerURL, provdiderNeedsCredentials, providerNeedsURLEncoding, providerSupportsCompression, providerDescription);
            }
            else{
            	logger.warning("No URL has been configured for the external provider: '" + id + "'. It will be skipped.");
            	return;
            }
        } catch (final MalformedURLException e) {
            logger.log(Level.SEVERE, "Exception. Class: {0} .Message: {1}", new Object[]{e.getClass().getName(), e.getMessage()});
            logger.log(Level.FINER, "", e);
        }
    }

	/**
     * General method for creating an SdmxClient.
     *
	 * @param provider
	 * @param user
	 * @param pw
	 * @return
	 */
	public static GenericSDMXClient createClient(String provider) throws SdmxException{
		final String sourceMethod = "createClient";

		logger.entering(sourceClass, sourceMethod);
		logger.fine("Create an SDMX client for '" + provider + "'");
		GenericSDMXClient client = null;
		Provider p = providers.get(provider);
		String hostname = null;

		String errorMsg = "The provider '" + provider + "' is not available in this configuration.";
		if(p != null && p.getEndpoint() != null){
			hostname = p.getEndpoint().getHost();
			if(p.getEndpoint().getProtocol().equals("http")){
				client = new RestSdmxClient(p.getName(), p.getEndpoint(), p.isNeedsCredentials(), p.isNeedsURLEncoding(), p.isSupportsCompression());
			}
			else if(p.getEndpoint().getProtocol().equals("https")){
				try {
					client = new HttpsSdmxClient(p.getName(), p.getEndpoint(), p.isNeedsCredentials(), p.isNeedsURLEncoding(), p.isSupportsCompression());
				} catch (KeyManagementException e) {
					logger.severe("Exception. Class: " + e.getClass().getName() + " .Message: " + e.getMessage());
					logger.log(Level.FINER, "", e);
					throw new SdmxException(errorMsg);
				} catch (NoSuchAlgorithmException e) {
					logger.severe("Exception. Class: " + e.getClass().getName() + " .Message: " + e.getMessage());
					logger.log(Level.FINER, "", e);
					throw new SdmxException(errorMsg);
				}
			}
			else {
				logger.severe("The protocol '" + p.getEndpoint().getProtocol() + "' is not supported.");
				throw new SdmxException(errorMsg);
			}
		}
		else {
			///legacy 2.0
			try{
				Class<?> clazz = Class.forName("it.bancaditalia.oss.sdmx.client.custom." + provider);
				client = (GenericSDMXClient)clazz.newInstance();
				hostname = client.getEndpoint().getHost();
			}
			catch (ClassNotFoundException e) {
				logger.severe("The provider '" + provider + "' is not available in this configuration.");
				throw new SdmxException(errorMsg);
			}
			catch (Exception e) {
				logger.severe("Exception caught. Class: " + e.getClass().getName() + " .Message: " + e.getMessage());
				logger.log(Level.FINER, "Exception caught. ", e);
				throw new SdmxException(errorMsg);
			}
		}

		// now set default proxy if necessary
    	ProxySelector ps = ProxySelector.getDefault();
    	if(ps != null && ps instanceof SdmxProxySelector){
    		((SdmxProxySelector)ps).addToDefaultProxy(hostname);
    	}

		logger.exiting(sourceClass, sourceMethod);
		return client;
	}

	/**
	 * Get the list of all available SDMX Providers
	 * @return
	 */
	public static Map<String, Provider> getProviders() {
        return providers;
    }
}
