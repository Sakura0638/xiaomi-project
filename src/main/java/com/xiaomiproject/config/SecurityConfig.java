package com.xiaomiproject.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    // 定义密码编码器，用于对密码进行加密和验证
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 暴露AuthenticationManager, 用于在Controller中手动进行登录认证
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // 配置认证提供者，告诉Spring Security如何获取用户信息和使用哪个密码编码器
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    // 配置HTTP安全策略
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 1. 关闭CSRF保护，因为我们是为API服务，不使用基于表单的保护
                .csrf().disable()
                // 2. 配置URL的访问权限
                .authorizeRequests()
                // 允许对 /api/auth/** 下的所有请求进行匿名访问 (注册和登录)
                .antMatchers("/api/auth/**").permitAll()
                // 其他所有请求都需要身份认证
                .anyRequest().authenticated();
    }
}
