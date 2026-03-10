package cn.linkfast.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.text.SimpleDateFormat;

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
        /**
         * 将 ObjectMapper 提升到根容器，确保 Service 层和测试环境都能直接注入
         */
        @Bean
        public ObjectMapper objectMapper() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return objectMapper;
        }
}