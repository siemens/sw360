package org.eclipse.sw360.keycloak.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class Sw360UserStorageProviderFactory implements UserStorageProviderFactory<Sw360UserStorageProvider> {
    public static final String PROVIDER_ID = "example-user-storage-jpa";

    private static final Logger logger = Logger.getLogger(Sw360UserStorageProviderFactory.class);

    @Override
    public Sw360UserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new Sw360UserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "JPA Example User Storage Provider";
    }

    @Override
    public void close() {
        logger.info("<<<<<< Closing factory");

    }
}
