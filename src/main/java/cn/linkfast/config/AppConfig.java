package cn.linkfast.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring核心配置类
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"cn.linkfast.service", "cn.linkfast.dao", "cn.linkfast.exception", "cn.linkfast.utils", "cn.linkfast.controller", "cn.linkfast.vo", "cn.linkfast.dto", "cn.linkfast.entity", "cn.linkfast.config", "cn.linkfast.common", "cn.linkfast.task"})
@ImportResource("classpath:applicationContext.xml")
public class AppConfig {
    //可以在此处添加额外的Java配置
}