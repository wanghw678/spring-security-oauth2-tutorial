/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.felord.oauth2.wechat;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 兼容微信请求参数的请求参数封装工具类,扩展了{@link OAuth2AuthorizationCodeGrantRequestEntityConverter}
 *
 * @author felord.cn
 * @see OAuth2AuthorizationCodeGrantRequestEntityConverter
 * @see Converter
 * @see OAuth2AuthorizationCodeGrantRequest
 * @see RequestEntity
 * @since 5.1
 */
public class WechatOAuth2AuthorizationCodeGrantRequestEntityConverter
        implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {
    private static final HttpHeaders DEFAULT_TOKEN_REQUEST_HEADERS = getDefaultTokenRequestHeaders();
    private static final ClientAuthenticationMethod JWT_CLIENT_ASSERTION_AUTHENTICATION_METHOD = new ClientAuthenticationMethod("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
    private static final String WECHAT_ID = "wechat";
    private final OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();

    /**
     * Returns the {@link RequestEntity} used for the Access Token Request.
     *
     * @param authorizationCodeGrantRequest the authorization code grant request
     * @return the {@link RequestEntity} used for the Access Token Request
     */
    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        ClientRegistration clientRegistration = authorizationCodeGrantRequest.getClientRegistration();
        HttpHeaders headers = getTokenRequestHeaders(clientRegistration);

        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        // 针对微信的定制  WECHAT_ID表示为微信公众号专用的registrationId
        if (WECHAT_ID.equals(clientRegistration.getRegistrationId())) {
            MultiValueMap<String, String> queryParameters = this.buildWechatQueryParameters(authorizationCodeGrantRequest);
            URI uri = UriComponentsBuilder.fromUriString(tokenUri).queryParams(queryParameters).build().toUri();
            return RequestEntity.get(uri).headers(headers).build();
        } else {
            defaultConverter.setParametersConverter(this::jwtClientAssertionConvert);
            return defaultConverter.convert(authorizationCodeGrantRequest);
        }
    }


    private   MultiValueMap<String, String> jwtClientAssertionConvert(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        ClientRegistration clientRegistration = authorizationCodeGrantRequest.getClientRegistration();
        OAuth2AuthorizationExchange authorizationExchange = authorizationCodeGrantRequest.getAuthorizationExchange();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add(OAuth2ParameterNames.GRANT_TYPE, authorizationCodeGrantRequest.getGrantType().getValue());
        parameters.add(OAuth2ParameterNames.CODE, authorizationExchange.getAuthorizationResponse().getCode());
        String redirectUri = authorizationExchange.getAuthorizationRequest().getRedirectUri();
        String codeVerifier = authorizationExchange.getAuthorizationRequest()
                .getAttribute(PkceParameterNames.CODE_VERIFIER);
        if (redirectUri != null) {
            parameters.add(OAuth2ParameterNames.REDIRECT_URI, redirectUri);
        }
        ClientAuthenticationMethod clientAuthenticationMethod = clientRegistration.getClientAuthenticationMethod();
        if (!ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(clientAuthenticationMethod)
                && !ClientAuthenticationMethod.BASIC.equals(clientAuthenticationMethod)) {
            parameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(clientAuthenticationMethod)
                || ClientAuthenticationMethod.POST.equals(clientAuthenticationMethod)) {
            parameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
        }
        if (codeVerifier != null) {
            parameters.add(PkceParameterNames.CODE_VERIFIER, codeVerifier);
        }
        if (JWT_CLIENT_ASSERTION_AUTHENTICATION_METHOD.equals(clientAuthenticationMethod)) {
            parameters.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE,clientAuthenticationMethod.getValue());
//            parameters.add(OAuth2ParameterNames.CLIENT_ASSERTION,"eyJ4NXQjUzI1NiI6IlN4cXFkV1l4VDdCWnJkSC11VnBnQUhmWDJxMzRxUHl4eDRvblg2bXYtcUkiLCJraWQiOiJqb3NlIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmZWxvcmQiLCJhdWQiOlsiaHR0cDpcL1wvbG9jYWxob3N0OjkwMDBcL29hdXRoMlwvdG9rZW4iLCJodHRwOlwvXC9sb2NhbGhvc3Q6OTAwMFwvb2F1dGgyXC9yZXZva2UiLCJodHRwOlwvXC9sb2NhbGhvc3Q6OTAwMFwvb2F1dGgyXC9pbnRyb3NwZWN0Il0sIm5iZiI6MTY0ODAyMzk5NCwic2NvcGUiOlsibWVzc2FnZS5yZWFkIiwibWVzc2FnZS53cml0ZSJdLCJpc3MiOiJmZWxvcmQiLCJleHAiOjE2NDgwMjQyOTQsImlhdCI6MTY0ODAyMzk5NH0.TObyX7Z2MD_k5eqaaJuJcJEa8_j_mQScP7c7MD4EOH0X102BPQadGUsGEuSNqrXEEA8hUks6rxflQKsEB3MpfBpUt7DifL_LA8QmcQucW1Fq3lJmX9a-2NiSmCs-YaLUpQxZSp0rJDIhcMf1YVQgRLA8oOIk2LOG9UOmbssWMmyQUkOQ1l_SYqElNBSOQXD7tVF80drFQ4pnqmsnQKgTJPPCWvq3OqZfqA4X8LuKj1rK-Idn6pbf5KgrdcodJB0nd77ihI2gcXMiDv00FWyEIROroQIlBIF0p-mZ_qScSPEGyvCtw7LvHeHyASmGTF1mDEepe1BRMfpFuQMLHkHPyg");

        }
        return parameters;

    }


    private MultiValueMap<String, String> buildWechatQueryParameters(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        // 获取微信的客户端配置
        ClientRegistration clientRegistration = authorizationCodeGrantRequest.getClientRegistration();
        OAuth2AuthorizationExchange authorizationExchange = authorizationCodeGrantRequest.getAuthorizationExchange();
        MultiValueMap<String, String> formParameters = new LinkedMultiValueMap<>();
        // grant_type
        formParameters.add(OAuth2ParameterNames.GRANT_TYPE, authorizationCodeGrantRequest.getGrantType().getValue());
        // code
        formParameters.add(OAuth2ParameterNames.CODE, authorizationExchange.getAuthorizationResponse().getCode());
        // 如果有redirect-uri
        String redirectUri = authorizationExchange.getAuthorizationRequest().getRedirectUri();
        if (redirectUri != null) {
            formParameters.add(OAuth2ParameterNames.REDIRECT_URI, redirectUri);
        }
        //appid
        formParameters.add("appid", clientRegistration.getClientId());
        //secret
        formParameters.add("secret", clientRegistration.getClientSecret());
        return formParameters;
    }


    static HttpHeaders getTokenRequestHeaders(ClientRegistration clientRegistration) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(DEFAULT_TOKEN_REQUEST_HEADERS);
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(clientRegistration.getClientAuthenticationMethod())
                || ClientAuthenticationMethod.BASIC.equals(clientRegistration.getClientAuthenticationMethod())) {
            String clientId = encodeClientCredential(clientRegistration.getClientId());
            String clientSecret = encodeClientCredential(clientRegistration.getClientSecret());
            headers.setBasicAuth(clientId, clientSecret);
        }
        return headers;
    }

    private static String encodeClientCredential(String clientCredential) {
        try {
            return URLEncoder.encode(clientCredential, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            // Will not happen since UTF-8 is a standard charset
            throw new IllegalArgumentException(ex);
        }
    }

    private static HttpHeaders getDefaultTokenRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
        final MediaType contentType = MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8");
        headers.setContentType(contentType);
        return headers;
    }
}
