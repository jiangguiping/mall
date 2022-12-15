package org.jiang.shpping.config;

import org.jiang.shpping.interceptor.LoginInterceptor;
import org.jiang.shpping.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //addInterceptor拦截器的注册器
        registry.addInterceptor(new LoginInterceptor()) // 添加拦截器
                .excludePathPatterns(
                        //排除不需要拦截的路径
                        "/user/**",
                        "/api/**",
                        ""
                );
        //默认拦截所有请求
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);

    }


}
