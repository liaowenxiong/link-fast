package cn.linkfast.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // 关键：开启 Spring 的定时任务调度功能
public class SchedulingConfig {
    // 这个类不需要写代码，仅作为开启开关使用
}