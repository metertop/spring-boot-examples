package com.hyx.Thread.demo;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 13:36
 */
public class WebConfig extends WebMvcConfigurationSupport {
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        // 将 ApiInterceptor 拦截器类添加进去
        registry.addInterceptor(new ApiInterceptor());
    }
}
