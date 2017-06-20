/*******************************************************************************
 * Copyright 2017 The MIT Internet Trust Consortium
 *
 * Portions copyright 2011-2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
/**
 *
 */
package org.mitre.openid.connect.client.service.impl;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.utils.URIBuilder;
import org.mitre.jwt.encryption.service.JWTEncryptionAndDecryptionService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.service.AuthRequestUrlBuilder;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.security.authentication.AuthenticationServiceException;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;

/**
 * @author jricher
 *
 */
public class EncryptedAuthRequestUrlBuilder implements AuthRequestUrlBuilder {

	private JWKSetCacheService encrypterService;

	private JWEAlgorithm alg;
	private EncryptionMethod enc;


	/* (non-Javadoc)
	 * @see org.mitre.openid.connect.client.service.AuthRequestUrlBuilder#buildAuthRequestUrl(org.mitre.openid.connect.config.ServerConfiguration, org.mitre.oauth2.model.RegisteredClient, java.lang.String, java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public String buildAuthRequestUrl(ServerConfiguration serverConfig, RegisteredClient clientConfig, String redirectUri, String nonce, String state, Map<String, String> options, String loginHint) {

		// create our signed JWT for the request object
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();

		//set parameters to JwtClaims
		claims.claim("response_type", "code");
		claims.claim("client_id", clientConfig.getClientId());
		claims.claim("scope", Joiner.on(" ").join(clientConfig.getScope()));

		// build our redirect URI
		claims.claim("redirect_uri", redirectUri);

		// this comes back in the id token
		claims.claim("nonce", nonce);

		// this comes back in the auth request return
		claims.claim("state", state);

		// Optional parameters
		for (Entry<String, String> option : options.entrySet()) {
			claims.claim(option.getKey(), option.getValue());
		}

		// if there's a login hint, send it
		if (!Strings.isNullOrEmpty(loginHint)) {
			claims.claim("login_hint", loginHint);
		}

		EncryptedJWT jwt = new EncryptedJWT(new JWEHeader(alg, enc), claims.build());

		JWTEncryptionAndDecryptionService encryptor = encrypterService.getEncrypter(serverConfig.getJwksUri());

		encryptor.encryptJwt(jwt);

		try {
			URIBuilder uriBuilder = new URIBuilder(serverConfig.getAuthorizationEndpointUri());
			uriBuilder.addParameter("request", jwt.serialize());

			// build out the URI
			return uriBuilder.build().toString();
		} catch (URISyntaxException e) {
			throw new AuthenticationServiceException("Malformed Authorization Endpoint Uri", e);
		}
	}

	/**
	 * @return the encrypterService
	 */
	public JWKSetCacheService getEncrypterService() {
		return encrypterService;
	}

	/**
	 * @param encrypterService the encrypterService to set
	 */
	public void setEncrypterService(JWKSetCacheService encrypterService) {
		this.encrypterService = encrypterService;
	}

	/**
	 * @return the alg
	 */
	public JWEAlgorithm getAlg() {
		return alg;
	}

	/**
	 * @param alg the alg to set
	 */
	public void setAlg(JWEAlgorithm alg) {
		this.alg = alg;
	}

	/**
	 * @return the enc
	 */
	public EncryptionMethod getEnc() {
		return enc;
	}

	/**
	 * @param enc the enc to set
	 */
	public void setEnc(EncryptionMethod enc) {
		this.enc = enc;
	}

}
