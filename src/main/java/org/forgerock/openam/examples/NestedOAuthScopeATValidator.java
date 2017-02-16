/*
 * Copyright © 2016 ForgeRock, AS.
 *
 * This is unsupported code made available by ForgeRock for community development subject to the license detailed below. 
 * The code is provided on an "as is" basis, without warranty of any kind, to the fullest extent permitted by law. 
 *
 * ForgeRock does not warrant or guarantee the individual success developers may have in implementing the code on their 
 * development platforms or in production configurations.
 *
 * ForgeRock does not warrant, guarantee or make any representations regarding the use, results of use, accuracy, timeliness 
 * or completeness of any data or information relating to the alpha release of unsupported code. ForgeRock disclaims all 
 * warranties, expressed or implied, and in particular, disclaims all warranties of merchantability, and warranties related 
 * to the code, or any service or software related thereto.
 *
 * ForgeRock shall not be liable for any direct, indirect or consequential damages or costs of any type arising out of any 
 * action taken by you or others related to the code.
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution License (the License). 
 * You may not use this file except in compliance with the License.
 * 
 * You can obtain a copy of the License at https://forgerock.org/cddlv1-0/. See the License for the specific language governing 
 * permission and limitations under the License.
 *
 * Portions Copyrighted 2016 Charan Mann
 *
 * OIDCSessionStatePlugin: Created by Charan Mann on 12/20/16 , 11:59 AM.
 */

package org.forgerock.openam.examples;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OpenAMScopeValidator;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openidconnect.OpenIDTokenIssuer;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;
import static org.forgerock.openam.scripting.ScriptConstants.OIDC_CLAIMS_NAME;

/**
 * Custom OpenAM Scope validator: This sets "session_state" in token response for OAuth Authorization Code and implicit flows.
 */
@Singleton
public class NestedOAuthScopeATValidator extends OpenAMScopeValidator {

    private static final Map<String, String> PROFILE_ATTR_MAPPING = new HashMap<>();
    private static final String MULTI_ATTRIBUTE_SEPARATOR = ",";

    static {
        PROFILE_ATTR_MAPPING.put("email", "mail");
        PROFILE_ATTR_MAPPING.put("address", "postaladdress");
        PROFILE_ATTR_MAPPING.put("phone_number", "telephonenumber");
        PROFILE_ATTR_MAPPING.put("given_name", "givenname");
        PROFILE_ATTR_MAPPING.put("zoneinfo", "preferredtimezone");
        PROFILE_ATTR_MAPPING.put("family_name", "sn");
        PROFILE_ATTR_MAPPING.put("locale", "preferredlocale");
        PROFILE_ATTR_MAPPING.put("name", "cn");
        PROFILE_ATTR_MAPPING.put("sub", "uid");
        PROFILE_ATTR_MAPPING.put("user_id", "uid");
    }

    private final IdentityManager identityManager;
    private final Debug logger = Debug.getInstance("NestedOAuthScopeATValidator");


    /**
     * Constructs a new OIDCSessionStateValidator.
     *
     * @param identityManager         An instance of the IdentityManager.
     * @param openIDTokenIssuer       An instance of the OpenIDTokenIssuer.
     * @param providerSettingsFactory An instance of the CTSPersistentStore.
     * @param openAMSettings          An instance of the OpenAMSettings.
     * @param scriptEvaluator         An instance of the OIDC Claims ScriptEvaluator.
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param scriptingServiceFactory An instance of the ScriptingServiceFactory.
     */
    @Inject
    public NestedOAuthScopeATValidator(IdentityManager identityManager, OpenIDTokenIssuer openIDTokenIssuer,
                                       OAuth2ProviderSettingsFactory providerSettingsFactory, OpenAMSettings openAMSettings,
                                       @Named(OIDC_CLAIMS_NAME) ScriptEvaluator scriptEvaluator,
                                       OpenIdConnectClientRegistrationStore clientRegistrationStore,
                                       ScriptingServiceFactory scriptingServiceFactory, ResourceOwnerSessionValidator resourceOwnerSessionValidator) {
        super(identityManager, openIDTokenIssuer, providerSettingsFactory, openAMSettings,
                scriptEvaluator, clientRegistrationStore, scriptingServiceFactory);
        this.identityManager = identityManager;
    }


    @Override
    public Map<String, Object> evaluateScope(AccessToken accessToken) {
        Map<String, Object> scopesMap = super.evaluateScope(accessToken);
        Set<String> scopes = accessToken.getScope();
        Map<String, Object> profileMap = new HashMap<>();

        final String resourceOwner = accessToken.getResourceOwnerId();
        final String clientId = accessToken.getClientId();
        final String realm = accessToken.getRealm();

        AMIdentity id = null;
        try {
            if (clientId != null && CLIENT_CREDENTIALS.equals(accessToken.getGrantType())) {
                id = identityManager.getClientIdentity(clientId, realm);
            } else if (resourceOwner != null) {
                id = identityManager.getResourceOwnerIdentity(resourceOwner, realm);
            }
        } catch (Exception e) {
            logger.error("Unable to get user identity", e);
        }

        if (scopes.contains("profile")) {
            for (String profileAttr : PROFILE_ATTR_MAPPING.keySet()) {

                try {
                    Set<String> attributes = id.getAttribute(PROFILE_ATTR_MAPPING.get(profileAttr));
                    StringBuilder builder = new StringBuilder();
                    if (CollectionUtils.isNotEmpty(attributes)) {
                        Iterator<String> attrValues = attributes.iterator();
                        while (attrValues.hasNext()) {
                            builder.append(attrValues.next());
                            if (attrValues.hasNext()) {
                                builder.append(MULTI_ATTRIBUTE_SEPARATOR);
                            }
                        }
                    }
                    profileMap.put(profileAttr, builder);
                } catch (IdRepoException | SSOException e) {
                    logger.error("Unable to get attribute", e);
                }
            }
        }

        scopesMap.put("profile", profileMap);

        return scopesMap;
    }
}
