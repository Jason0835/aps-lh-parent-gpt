package com.zlt.framework.cas;

import lombok.extern.slf4j.Slf4j;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.callback.CallbackUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.InternalAttributeHandler;
import org.pac4j.core.profile.ProfileHelper;

import java.util.HashMap;
import java.util.Map;
@Slf4j
public class CasAuthenticator extends org.pac4j.cas.credentials.authenticator.CasAuthenticator{

    public CasAuthenticator(CasConfiguration configuration, String clientName, UrlResolver urlResolver, CallbackUrlResolver callbackUrlResolver, String callbackUrl) {
       super(configuration, clientName, urlResolver, callbackUrlResolver, callbackUrl);
    }

    @Override
    public void validate(final TokenCredentials credentials, final WebContext context) {
        init();

        final String ticket = credentials.getToken();
        try {
            final String finalCallbackUrl = callbackUrlResolver.compute(urlResolver, callbackUrl, clientName, context);
            final Assertion assertion = configuration.retrieveTicketValidator(context).validate(ticket, finalCallbackUrl);
            final AttributePrincipal principal = assertion.getPrincipal();
            log.debug("principal: {}", principal);

            final String id = principal.getName();
            final Map<String, Object> newPrincipalAttributes = new HashMap<>();
            final Map<String, Object> newAuthenticationAttributes = new HashMap<>();
            // restore both sets of attributes
            final Map<String, Object> oldPrincipalAttributes = principal.getAttributes();
            final Map<String, Object> oldAuthenticationAttributes = assertion.getAttributes();
            final InternalAttributeHandler attrHandler = ProfileHelper.getInternalAttributeHandler();
            if (oldPrincipalAttributes != null) {
                oldPrincipalAttributes.entrySet().stream()
                        .forEach(e -> newPrincipalAttributes.put(e.getKey(), attrHandler.restore(e.getValue())));
            }
            if (oldAuthenticationAttributes != null) {
                oldAuthenticationAttributes.entrySet().stream()
                        .forEach(e -> newAuthenticationAttributes.put(e.getKey(), attrHandler.restore(e.getValue())));
            }

            final CommonProfile profile;
            // in case of CAS proxy, don't restore the profile, just build a CAS one
            if (configuration.getProxyReceptor() != null) {
                profile = getProfileDefinition().newProfile(principal, configuration.getProxyReceptor());
                profile.setId(ProfileHelper.sanitizeIdentifier(profile, id));
                getProfileDefinition().convertAndAdd(profile, newPrincipalAttributes, newAuthenticationAttributes);
            } else {
                profile = ProfileHelper.restoreOrBuildProfile(getProfileDefinition(), id, newPrincipalAttributes,
                        newAuthenticationAttributes, principal, configuration.getProxyReceptor());
            }
            log.debug("profile returned by CAS: {}", profile);

            credentials.setUserProfile(profile);
        } catch (final TicketValidationException e) {
            String message = "cannot validate CAS ticket: " + ticket;
            throw new TechnicalException(message, e);
        }
    }
}
