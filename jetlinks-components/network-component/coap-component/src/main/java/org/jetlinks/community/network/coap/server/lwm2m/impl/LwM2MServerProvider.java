package org.jetlinks.community.network.coap.server.lwm2m.impl;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.FileSecurityStore;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public class LwM2MServerProvider {

    public static final String[] MODEL_PATHS = new String[]{
        "19.mxl"
    };

    public static LeshanServer  createLeshanServer() throws Exception {
// Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", MODEL_PATHS));
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore = new FileSecurityStore();
        builder.setSecurityStore(securityStore);

        // use RPK only
        builder.setPublicKey(cli.identity.getPublicKey());
        builder.setPrivateKey(cli.identity.getPrivateKey());

        // Create Californium Endpoints Provider:
        // ------------------
        // Create Server Endpoints Provider

        CaliforniumServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(
            // Add coap Protocol support
            new CoapServerProtocolProvider(),

            // Add coaps protocol support
            new CoapsServerProtocolProvider(c -> {
                // Add MDC for connection logs
                if (cli.helpsOptions.getVerboseLevel() > 0)
                    c.setConnectionListener(new PrincipalMdcConnectionListener());

            }));

        // Create Californium Configuration
        Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        // These configuration values are always overwritten by CLI therefore set them to transient.
        serverCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        serverCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
        serverCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
        if (cli.dtls.cid != null) {
            serverCoapConfig.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);
        }

        // Persist configuration
        File configFile = new File(CF_CONFIGURATION_FILENAME);
        if (configFile.isFile()) {
            serverCoapConfig.load(configFile);
        } else {
            serverCoapConfig.store(configFile, CF_CONFIGURATION_HEADER);
        }

        // Enforce DTLS role to ServerOnly if needed
        if (cli.identity.isx509()) {
            X509Certificate serverCertificate = cli.identity.getCertChain()[0];
            if (serverCoapConfig.get(DtlsConfig.DTLS_ROLE) == DtlsConfig.DtlsRole.BOTH) {
                if (serverCertificate != null) {
                    if (CertPathUtil.canBeUsedForAuthentication(serverCertificate, false)) {
                        if (!CertPathUtil.canBeUsedForAuthentication(serverCertificate, true)) {
                            serverCoapConfig.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.SERVER_ONLY);
                            LOG.warn("Server certificate does not allow Client Authentication usage."
                                + "\nThis will prevent this LWM2M server to initiate DTLS connection."
                                + "\nSee : https://github.com/eclipse/leshan/wiki/Server-Failover#about-connections");
                        }
                    }
                }
            }
        }

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(serverCoapConfig);

        // Create CoAP endpoint
        int coapPort = cli.main.localPort == null ? serverCoapConfig.get(CoapConfig.COAP_PORT) : cli.main.localPort;
        InetSocketAddress coapAddr = cli.main.localAddress == null ? new InetSocketAddress(coapPort)
            : new InetSocketAddress(cli.main.localAddress, coapPort);
        if (cli.main.disableOscore) {
            endpointsBuilder.addEndpoint(coapAddr, Protocol.COAP);
        } else {
            endpointsBuilder.addEndpoint(new CoapOscoreServerEndpointFactory(
                EndpointUriUtil.createUri(Protocol.COAP.getUriScheme(), coapAddr)));
        }

        // Create CoAP over DTLS endpoint
        int coapsPort = cli.main.secureLocalPort == null ? serverCoapConfig.get(CoapConfig.COAP_SECURE_PORT)
            : cli.main.secureLocalPort;
        InetSocketAddress coapsAddr = cli.main.secureLocalAddress == null ? new InetSocketAddress(coapsPort)
            : new InetSocketAddress(cli.main.secureLocalAddress, coapsPort);
        endpointsBuilder.addEndpoint(coapsAddr, Protocol.COAPS);

        // Create LWM2M server
        builder.setEndpointsProvider(endpointsBuilder.build());
        return builder.build();
    }
}
