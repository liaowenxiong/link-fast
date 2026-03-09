package cn.linkfast.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring 核心配置类 - 负责业务层与持久层
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(
        basePackages = {"cn.linkfast"},
        excludeFilters = {
                // 纠正点：排除掉 Controller，交给 WebMvcConfig 去扫描
                @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebMvcConfig.class)
        }
)
@ImportResource("classpath:applicationContext.xml")
public class AppConfig {
}