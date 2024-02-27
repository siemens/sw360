package org.eclipse.sw360.keycloak.event.listener;

import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomEventListenerProviderFactorySW360 implements EventListenerProviderFactory {

	public EventListenerProvider create(KeycloakSession session) {
		return new CustomEventListenerSW360(session);
	}

	public void init(Scope config) {

	}

	public void postInit(KeycloakSessionFactory factory) {

	}

	public void close() {

	}

	public String getId() {
		return "sw360-add-user-to-couchdb";
	}
}
