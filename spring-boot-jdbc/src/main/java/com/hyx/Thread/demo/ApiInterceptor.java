package com.hyx.Thread.demo;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 13:34
 */
public class ApiInterceptor implements HandlerInterceptor {
    /**
     * 请求之前访问
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("请求之前拦截...");
        return true;
    }

    /**
     * 请求时访问
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        System.out.println("请求中拦截...");
    }

    /**
     * 请求完成后访问
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        System.out.println("请求完成后拦截...");
    }
}
