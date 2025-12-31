package com.arcbank.cbs.transaccion.config;

import feign.Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MTLSConfig {

    @Value("${app.mtls.keystore.path:classpath:certs/bantec-keystore.p12}")
    private Resource keystoreResource;

    @Value("${app.mtls.keystore.password:bantec123}")
    private String keystorePassword;

    @Value("${app.mtls.truststore.path:classpath:certs/bantec-truststore.p12}")
    private Resource truststoreResource;

    @Value("${app.mtls.truststore.password:bantec123}")
    private String truststorePassword;

    @Value("${app.mtls.enabled:false}")
    private boolean mtlsEnabled;

    @Bean
    public Client feignClient() throws Exception {
        if (!mtlsEnabled) {

            return new Client.Default(null, null);
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream keyStoreStream = keystoreResource.getInputStream()) {
            keyStore.load(keyStoreStream, keystorePassword.toCharArray());
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        boolean trustStoreLoaded = false;
        try (InputStream trustStoreStream = truststoreResource.getInputStream()) {
            trustStore.load(trustStoreStream, truststorePassword.toCharArray());
            if (trustStore.size() > 0) {
                trustStoreLoaded = true;
            } else {
                log.warn("El truststore {} está vacío. Se usará el truststore del sistema.", truststoreResource);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el truststore {}: {}. Se usará el truststore del sistema.", truststoreResource,
                    e.getMessage());
        }

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, keystorePassword.toCharArray());

        if (trustStoreLoaded) {
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        } else {
            // Si no hay truststore personalizado o está vacío, cargamos el por defecto del
            // sistema
            sslContextBuilder.loadTrustMaterial((java.security.KeyStore) null,
                    (org.apache.hc.core5.ssl.TrustStrategy) null);
            // Nota: .loadTrustMaterial(null, null) carga las CAs del sistema (trustAnchors)
        }

        SSLContext sslContext = sslContextBuilder.build();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        return new feign.hc5.ApacheHttp5Client(httpClient);
    }
}
