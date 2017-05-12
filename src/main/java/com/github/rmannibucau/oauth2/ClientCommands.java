package com.github.rmannibucau.oauth2;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.grants.code.JCacheCodeDataProvider;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import java.net.URISyntaxException;

@Command("client")
public class ClientCommands {
    @Command("create")
    public static void createClient(@Option("client-id") final String clientId,
                                    @Option("client-secret") final String clientSecret,
                                    @Option("confidential") @Default("true") final boolean confidential,
                                    @Option("application-name") final String appName,
                                    @Option("application-web-uri") final String webUri) throws URISyntaxException {
        final Client client = new Client(clientId, clientSecret, confidential, appName, webUri);
        getClients().put(clientId, client);
    }

    private static Cache<String, Client> getClients() throws URISyntaxException {
        // enhancements: ensure config is aligned with meecrowave one, we can also desire to close the provider at the end of the app
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final CacheManager cacheManager = Caching.getCachingProvider().getCacheManager(loader.getResource("default-oauth2.jcs").toURI(), loader);
        try {
            return cacheManager
                    .createCache(JCacheCodeDataProvider.CLIENT_CACHE_KEY, new MutableConfiguration<String, Client>()
                            .setTypes(String.class, Client.class)
                            .setStoreByValue(true)
                            .setStatisticsEnabled(false));
        } catch (final CacheException exists) {
            return cacheManager.getCache(JCacheCodeDataProvider.CLIENT_CACHE_KEY, String.class, Client.class);
        }
    }
}
