# NestedOAuthCustomScope

* Custom OpenAM Scope validator <br />
* This plugin sets custom nested OAuth scopes.

    
Pre-requisites :
================
1. OpenAM has been installed and configured.
2. OpenAM has been configured as OIDC provider. Check https://backstage.forgerock.com/docs/am/5/oauth2-guide#configure-oauth2-authz 
3. Maven has been installed and configured.

OpenAM Configuration:
=====================
1. Build NestedOAuthCustomScope by running 'mvn clean install'. This will build openam-nested-oauth-scopes-1.0.0-SNAPSHOT.jar under /target directory.
2. Stop OpenAM. 
3. Copy openam-nested-oauth-scopes-1.0.0-SNAPSHOT.jar to (OpenAM-TomcatHome)/webapps/ROOT/WEB-INF/lib
4. Restart OpenAM
5. Specify "org.forgerock.openam.examples.NestedOAuthScopeATValidator" under Realms> (Specific realm)> Services> OAuth2 Provider> Scope Implementation Class
  
Testing:
======== 
* Resource Owner Password Credentials Grant:
```
Get Access token:
curl -X POST -H "Authorization: BASIC bXlDbGllbnRJRDpwYXNzd29yZA==" -H "Content-Type: application/x-www-form-urlencoded" -d 'grant_type=password&username=app1&password=password&scope=mail paymentInfo' "http://openam135.sample.com:8080/openam/oauth2/app/access_token"

{
  "scope": "mail paymentInfo",
  "expires_in": 3599,
  "token_type": "Bearer",
  "refresh_token": "18d36e70-b618-4f5a-bd44-e35000445b50",
  "access_token": "30288750-7a9d-4586-ba0b-2e814913708a"
}

Invoke Token Info: 
curl -X GET -H "Authorization: Bearer 30288750-7a9d-4586-ba0b-2e814913708a" "http://openam135.sample.com:8080/openam/oauth2/app/tokeninfo"

{
  "paymentInfo": {
    "sub": "app1",
    "phone_number": "2223334444",
    "email": "app1@sample.com",
    "address": "222 Main St, Somewhere"
  },
  "scope": [
    "paymentInfo",
    "openid",
    "profile"
  ],
  "grant_type": "password",
  "realm": "/app",
  "openid": "",
  "token_type": "Bearer",
  "expires_in": 3591,
  "client_id": "myClientID",
  "access_token": "4ec72443-0267-471c-83c0-20efa76cd6b7",
  "profile": ""
}


Invoke Introspection 
curl -X POST -H "Authorization: BASIC bXlDbGllbnRJRDpwYXNzd29yZA==" "http://openam135.sample.com:8080/openam/oauth2/app/introspect?token=4ec72443-0267-471c-83c0-20efa76cd6b7"

{
  "active": true,
  "scope": "mail paymentInfo",
  "client_id": "myClientID",
  "user_id": "app1",
  "token_type": "access_token",
  "exp": 1487268165,
  "sub": "app1",
  "iss": "http://openam135.sample.com:8080/openam/oauth2/app"
}
```

* * *

Copyright © 2016 ForgeRock, AS.

This is unsupported code made available by ForgeRock for community development subject to the license detailed below. The code is provided on an "as is" basis, without warranty of any kind, to the fullest extent permitted by law. 

ForgeRock does not warrant or guarantee the individual success developers may have in implementing the code on their development platforms or in production configurations.

ForgeRock does not warrant, guarantee or make any representations regarding the use, results of use, accuracy, timeliness or completeness of any data or information relating to the alpha release of unsupported code. ForgeRock disclaims all warranties, expressed or implied, and in particular, disclaims all warranties of merchantability, and warranties related to the code, or any service or software related thereto.

ForgeRock shall not be liable for any direct, indirect or consequential damages or costs of any type arising out of any action taken by you or others related to the code.

The contents of this file are subject to the terms of the Common Development and Distribution License (the License). You may not use this file except in compliance with the License.

You can obtain a copy of the License at https://forgerock.org/cddlv1-0/. See the License for the specific language governing permission and limitations under the License.

Portions Copyrighted 2016 Charan Mann
