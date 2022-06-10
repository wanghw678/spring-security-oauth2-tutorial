package cn.felord.oauth2.wechat;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Data
public class WorkWechatOAuth2User implements OAuth2User {
    private Set<GrantedAuthority> authorities;
    private Integer errcode;
    private String errmsg;
    @JsonAlias("OpenId")
    private String openId;
    @JsonAlias("UserId")
    private String userId;



    @Override
    public Map<String, Object> getAttributes() {
        //todo 这里放一些有用的额外参数
        return Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //todo 微信用户你想赋权的可以在这里或者set方法中实现。
        return this.authorities;
    }

    @Override
    public String getName() {
        return Optional.ofNullable(userId).orElse(openId);
    }
}
