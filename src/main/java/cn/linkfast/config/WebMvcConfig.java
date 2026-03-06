package cn.linkfast.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.linkfast.controller")
public class WebMvcConfig implements WebMvcConfigurer {
    //可以在此处配置拦截器、静态资源处理等
}