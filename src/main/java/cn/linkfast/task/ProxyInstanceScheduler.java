package cn.linkfast.task;

import cn.linkfast.dto.ProxyRenewDTO;
import cn.linkfast.dto.ProxyRenewItemDTO;
import cn.linkfast.entity.ProxyInstance;
import cn.linkfast.service.ProxyInstanceService;
import cn.linkfast.service.ProxyOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 代理实例定时任务
 * 负责自动续费即将到期的代理实例
 *
 * @author liaowenxiong
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProxyInstanceScheduler {

    private final ProxyInstanceService proxyInstanceService;
    private final ProxyOrderService proxyOrderService;

    /** 自动续费的支付密码（固定值） */
    private static final String AUTO_RENEW_PAY_PASSWORD = "168888";
    /** 距离到期天数阈值 */
    private static final int EXPIRING_SOON_DAYS = 3;

    /**
     * 定时续费任务：每天凌晨 02:00:00 执行
     * 查询已开启自动续费且还剩3天到期的代理实例，自动发起续费
     * Cron 表达式（Spring 6 域）：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void renewProxyInstances() {
        log.info("============== [定时续费任务] 开始执行 ==============");

        // 1. 查询已开启自动续费（renew=1）且还剩3天到期的代理实例
        List<ProxyInstance> expiringInstances;
        try {
            expiringInstances = proxyInstanceService.getAutoRenewExpiringInstances(EXPIRING_SOON_DAYS);
        } catch (Exception e) {
            log.error("!!! [定时续费任务] 查询即将到期实例失败: {}", e.getMessage(), e);
            return;
        }

        if (expiringInstances == null || expiringInstances.isEmpty()) {
            log.info("============== [定时续费任务] 暂无需要续费的实例，任务结束 ==============");
            return;
        }

        log.info("[定时续费任务] 查询到 {} 个需要续费的代理实例", expiringInstances.size());

        // 2. 将实例列表封装为  List<ProxyRenewItemDTO> ，调用续费服务

        List<ProxyRenewItemDTO> items = expiringInstances.stream().map(instance -> {
            ProxyRenewItemDTO item = new ProxyRenewItemDTO();
            item.setInstanceNo(instance.getInstanceNo());
            // 沿用实例购买时的周期单位（unit）和周期时长（duration）
            item.setUnit(instance.getUnit());
            item.setDuration(instance.getDuration());
            item.setCycleTimes(1);
            return item;
        }).toList();

        // 3. 调用续费服务
        try {
            proxyOrderService.renewProxies(items);
            log.info("============== [定时续费任务] 续费成功，共续费 {} 个实例 ==============", items.size());
        } catch (Exception e) {
            log.error("!!! [定时续费任务] 续费失败: {}", e.getMessage(), e);
        }
    }
}
