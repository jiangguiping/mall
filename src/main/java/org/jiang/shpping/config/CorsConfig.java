package org.jiang.shpping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SpringBoot解决跨域问题
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //解决Vue与SpringBoot通信跨域问题
        registry.addMapping("/**")  //设置允许跨域的路径
                .allowedOriginPatterns("*")          //设置允许跨域请求的域名
                .allowedMethods("GET","HEAD","POST","PUT","DELETE","OPTIONS")   //设置允许的方法
                .allowCredentials(true)       //这里：是否允许证书 不再默认开启
                .maxAge(3600)                 //跨域允许时间
                .allowedHeaders("*");
    }
}

