package cn.linkfast.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Spring MVC 配置类 - 完全替代 spring-mvc.xml
 */
@Configuration
@EnableWebMvc // 对应 <mvc:annotation-driven />
@ComponentScan(basePackages = "cn.linkfast.controller") // 对应 <context:component-scan />
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 1. 配置消息转换器 (对应 <mvc:message-converters>)
     * 用于设置 Jackson 的日期格式化
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置日期格式化方案，对应 XML 中的 SimpleDateFormat 配置
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        converter.setObjectMapper(objectMapper);
        converters.add(converter);
    }

    /**
     * 2. 配置默认 Servlet 处理 (对应 <mvc:default-servlet-handler/>)
     * 允许将对静态资源的请求转发到 Web 容器的默认 Servlet
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}