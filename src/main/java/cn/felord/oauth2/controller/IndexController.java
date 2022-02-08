package cn.felord.oauth2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * @author  felord.cn
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public Map<String,String> index(){
        return Collections.singletonMap("msg","oauth2Login success!");
    }
}