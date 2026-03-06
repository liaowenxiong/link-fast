package cn.linkfast.task;

import cn.linkfast.service.ProxyProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理产品自动化同步任务
 * 采用 Lombok @RequiredArgsConstructor 实现构造器注入
 * 采用 Lombok @Slf4j 实现日志记录
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductSyncTask {

    // final 修饰的字段会被 Lombok 自动纳入构造函数中，实现完美注入
    private final ProxyProductService proxyProductService;

    /**
     * 定时同步任务：每小时执行一次
     * Cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 * * * *")
    public void executeSyncJob() {
        log.info("============== [定时任务] 开始同步第三方代理产品数据 ==============");
        // 1. 根据文档要求构造参数：proxyType 是必填的 []int 类型
        Map<String, Object> params = new HashMap<>();

        // 2. 构造所有代理类型的数组 (参考图片字典)
        // 101: 静态云平台, 102: 静态国内家庭, 103: 静态国外家庭
        // 104: 动态国外, 105: 动态国内, 201: whatsapp
        List<Integer> allProxyTypes = Arrays.asList(101, 102, 103, 104, 105, 201);

        // 3. 将数组存入 Map
        params.put("proxyType", allProxyTypes);

        // (可选) 如果你想拉取特定的供应商或国家，可以在这里继续 put
        // params.put("countryCode", "US");
        try {
            // 调用同步逻辑
            int result = proxyProductService.syncProxyProducts(params);
            log.info("[定时任务] 数据同步执行成功，同步成功 {} 条数据", result);
        } catch (Exception e) {
            log.error("!!! [定时任务] 数据同步过程中发生致命错误：{}", e.getMessage(), e);
        }
    }

    /**
     * 系统启动自检查任务：
     * 项目启动 5 秒后立即执行一次，确保数据库中有初始数据
     * fixedDelay 设置为 Long.MAX_VALUE，确保该方法在启动后仅运行一次
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    public void runInitialSync() {
        log.info(">>> [启动检查] 正在执行系统初始化产品同步...");
        // 1. 根据文档要求构造参数：proxyType 是必填的 []int 类型
        Map<String, Object> params = new HashMap<>();

        // 2. 构造所有代理类型的数组 (参考图片字典)
        // 101: 静态云平台, 102: 静态国内家庭, 103: 静态国外家庭
        // 104: 动态国外, 105: 动态国内, 201: whatsapp
        List<Integer> allProxyTypes = Arrays.asList(101, 102, 103, 104, 105, 201);

        // 3. 将数组存入 Map
        params.put("proxyType", allProxyTypes);

        // (可选) 如果你想拉取特定的供应商或国家，可以在这里继续 put
        // params.put("countryCode", "US");
        try {
            int result = proxyProductService.syncProxyProducts(params);
            log.info("<<< [启动检查] 初始化产品同步已完成，同步成功 {} 条数据", result);
        } catch (Exception e) {
            log.error("!!! [启动检查] 初始化同步失败：{}", e.getMessage(), e);
        }
    }
}