package cn.felord.oauth2.wechat;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.MapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 兼容微信token接口返回转换的工具，扩展了{@link DefaultMapOAuth2AccessTokenResponseConverter}
 *
 * @author felord.cn
 * @since 2021/8/13 16:43
 */
public class WechatMapOAuth2AccessTokenResponseConverter implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    private final Converter<Map<String, Object>, OAuth2AccessTokenResponse> delegate = new DefaultMapOAuth2AccessTokenResponseConverter();

    @Override
    public OAuth2AccessTokenResponse convert(Map<String, Object> tokenResponseParameters) {
        HashMap<String, Object> response = new HashMap<>(tokenResponseParameters);
        // 避免 token_type 空校验异常
        response.put(OAuth2ParameterNames.TOKEN_TYPE, OAuth2AccessToken.TokenType.BEARER.getValue());
        return this.delegate.convert(response);
    }

}
