package cn.felord.oauth2.config;

import cn.felord.oauth2.wechat.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author felord.cn
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /***
     * 自定义
     *
     * @param http http
     * @return SecurityFilterChain
     * @throws Exception exception
     */
    @Bean
    SecurityFilterChain customSecurityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        Map<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>> map = new HashMap<>();
        WechatOAuth2UserService value = new WechatOAuth2UserService();
        map.put(ClientProviders.WECHAT_WEB_CLIENT.registrationId(), value);
        map.put(ClientProviders.WECHAT_WEB_LOGIN_CLIENT.registrationId(), value);
        map.put(ClientProviders.WORK_WECHAT_SCAN_CLIENT.registrationId(), new WorkWechatOAuth2UserService());

        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DelegatingOAuth2UserService<>(map);

        OAuth2AuthorizationRequestResolver authorizationRequestResolver = oAuth2AuthorizationRequestResolver(clientRegistrationRepository);
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient = accessTokenResponseClient();

        http.authorizeRequests((requests) ->
                        requests.mvcMatchers(HttpMethod.GET, "/wwechat/callback").anonymous()
                                .anyRequest().authenticated())
                .oauth2Login().authorizationEndpoint()
                // 授权端点配置
                .authorizationRequestResolver(authorizationRequestResolver)
                .and()
                // 获取token端点配置  比如根据code 获取 token
                .tokenEndpoint().accessTokenResponseClient(accessTokenResponseClient)
                .and()
                // 获取用户信息端点配置  根据accessToken获取用户基本信息
                .userInfoEndpoint().userService(oAuth2UserService);
        http.oauth2Client()
                .authorizationCodeGrant().authorizationRequestResolver(authorizationRequestResolver)
                .accessTokenResponseClient(accessTokenResponseClient);
        return http.build();
    }

    /**
     * 用来从{@link javax.servlet.http.HttpServletRequest}中检索Oauth2需要的参数并封装成OAuth2请求对象{@link OAuth2AuthorizationRequest}
     *
     * @param clientRegistrationRepository the client registration repository
     * @return DefaultOAuth2AuthorizationRequestResolver
     */
    private OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(WechatOAuth2AuthorizationRequestCustomizer::customize);
        return resolver;
    }

    /**
     * 调用token-uri去请求授权服务器获取token的OAuth2 Http 客户端
     *
     * @return OAuth2AccessTokenResponseClient
     */
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        tokenResponseClient.setRequestEntityConverter(new WechatOAuth2AuthorizationCodeGrantRequestEntityConverter());

        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
        // 微信返回的content-type 是 text-plain
        tokenResponseHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json")));
        // 兼容微信解析
        tokenResponseHttpMessageConverter.setAccessTokenResponseConverter(new WechatMapOAuth2AccessTokenResponseConverter());

        RestTemplate restTemplate = new RestTemplate(
                Arrays.asList(new FormHttpMessageConverter(),
                        tokenResponseHttpMessageConverter
                ));

        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        tokenResponseClient.setRestOperations(restTemplate);
        return new DelegateOAuth2AccessTokenResponseClient(tokenResponseClient);
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .antMatchers("/error")
                .antMatchers("/favicon.ico");
    }
}

