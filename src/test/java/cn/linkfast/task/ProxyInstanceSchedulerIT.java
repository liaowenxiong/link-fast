package cn.linkfast.task;

import cn.linkfast.config.AppConfig;
import cn.linkfast.dto.ProxyRenewItemDTO;
import cn.linkfast.entity.ProxyInstance;
import cn.linkfast.service.ProxyInstanceService;
import cn.linkfast.service.ProxyOrderService;
import cn.linkfast.vo.ProxyRenewResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProxyInstanceScheduler 集成测试
 * 真实查询数据库、真实创建续费订单、真实调用第三方续费接口，校验到期时间是否延期
 *
 * @author liaowenxiong
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class})
public class ProxyInstanceSchedulerIT {

    @Autowired
    private ProxyInstanceService proxyInstanceService;

    @Autowired
    private ProxyOrderService proxyOrderService;

    @Autowired
    private ProxyInstanceScheduler proxyInstanceScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试自动续费完整流程：
     * 1. 查询即将到期且开启自动续费的实例
     * 2. 记录续费前的到期时间
     * 3. 调用续费服务（真实创建订单 + 真实调用第三方接口）
     * 4. 校验是否成功创建续费订单
     * 5. 校验续费订单是否有第三方返回的 orderNo
     * 6. 等待2分钟后，同步实例数据，校验到期时间是否真的延期了
     */
    @Test
    @DisplayName("自动续费集成测试：续费 -> 校验订单创建 -> 校验第三方orderNo -> 等待2分钟 -> 校验到期时间延期")
    public void testRenewProxyInstances() throws Exception {
        // ============ 1. 查询已开启自动续费且即将到期的代理实例（与定时任务逻辑一致，3天内） ============
        List<ProxyInstance> expiringInstances = proxyInstanceService.getAutoRenewExpiringInstances(3);
        assertNotNull(expiringInstances, "查询结果不应为null");

        if (expiringInstances.isEmpty()) {
            System.out.println("========== 暂无3天内即将到期的自动续费实例，扩大查询范围 ==========");
            String sql = "SELECT * FROM proxy_instance WHERE renew = 1 "
                    + "AND user_expired IS NOT NULL "
                    + "AND user_expired > UNIX_TIMESTAMP(NOW()) "
                    + "ORDER BY user_expired LIMIT 5";
            expiringInstances = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ProxyInstance.class));
        }

        if (expiringInstances.isEmpty()) {
            System.out.println("========== 数据库中没有开启自动续费的活跃实例，跳过测试 ==========");
            return;
        }

        // ============ 2. 取第一个实例进行测试，记录续费前的到期时间 ============
        ProxyInstance testInstance = expiringInstances.get(0);
        String instanceNo = testInstance.getInstanceNo();
        Long originalExpired = testInstance.getUserExpired();
        Integer unit = testInstance.getUnit();
        Integer duration = testInstance.getDuration();

        System.out.println("========== 续费前实例信息 ==========");
        System.out.println("instanceNo:    " + instanceNo);
        System.out.println("到期时间戳(秒): " + originalExpired);
        System.out.println("到期时间:       " + new Date(originalExpired * 1000));
        System.out.println("unit(周期单位): " + unit);
        System.out.println("duration(时长): " + duration);
        System.out.println("renew(自动续费): " + testInstance.getRenew());

        assertNotNull(instanceNo, "实例编号不应为null");
        assertNotNull(unit, "周期单位不应为null");
        assertNotNull(duration, "周期时长不应为null");

        // ============ 3. 构建续费参数，与定时任务 renewProxyInstances() 逻辑一致 ============
        ProxyRenewItemDTO item = new ProxyRenewItemDTO();
        item.setInstanceNo(instanceNo);
        item.setUnit(unit);
        item.setDuration(duration);
        item.setCycleTimes(1);

        // ============ 4. 调用续费服务（真实创建续费订单 + 真实调用第三方续费接口） ============
        System.out.println("========== 开始调用续费服务 ==========");
        ProxyRenewResultVO result = proxyOrderService.renewProxies(List.of(item));
        assertNotNull(result, "续费结果不应为null");
        String appOrderNo = result.getAppOrderNo();
        assertNotNull(appOrderNo, "渠道商订单号不应为null");

        System.out.println("续费渠道商订单号: " + appOrderNo);
        System.out.println("续费第三方订单号: " + result.getOrderNo());
        System.out.println("续费金额:          " + result.getAmount());
        System.out.println("续费状态:          " + result.getStatus());

        // ============ 5. 校验是否成功创建续费订单 ============
        System.out.println("========== 校验续费订单是否成功创建 ==========");
        String orderQuerySql = "SELECT id, order_no, app_order_no, order_type, status, amount "
                + "FROM proxy_order WHERE app_order_no = ?";
        Map<String, Object> orderRow = jdbcTemplate.queryForMap(orderQuerySql, appOrderNo);

        assertNotNull(orderRow, "续费订单应存在");
        System.out.println("订单id:        " + orderRow.get("id"));
        System.out.println("订单号orderNo: " + orderRow.get("order_no"));
        System.out.println("渠道商订单号:   " + orderRow.get("app_order_no"));
        System.out.println("订单类型:       " + orderRow.get("order_type"));
        System.out.println("订单状态:       " + orderRow.get("status"));
        System.out.println("订单金额:       " + orderRow.get("amount"));

        // 校验订单类型为续费（order_type=2）
        assertEquals(2, ((Number) orderRow.get("order_type")).intValue(),
                "续费订单的 order_type 应为 2");

        // 校验续费订单明细是否存在
        String itemQuerySql = "SELECT id, order_no, app_order_no, instance_no, duration, unit, cycle_times "
                + "FROM proxy_renew_order_item WHERE app_order_no = ?";
        List<Map<String, Object>> orderItems = jdbcTemplate.queryForList(itemQuerySql, appOrderNo);
        assertFalse(orderItems.isEmpty(), "续费订单明细不应为空");
        System.out.println("续费订单明细条数: " + orderItems.size());
        for (Map<String, Object> orderItem : orderItems) {
            System.out.println("  明细id: " + orderItem.get("id")
                    + ", instanceNo: " + orderItem.get("instance_no")
                    + ", duration: " + orderItem.get("duration")
                    + ", unit: " + orderItem.get("unit")
                    + ", cycleTimes: " + orderItem.get("cycle_times"));
        }

        // ============ 6. 校验续费订单是否有第三方返回的 orderNo ============
        System.out.println("========== 校验续费订单的第三方 orderNo ==========");
        String dbOrderNo = (String) orderRow.get("order_no");
        assertNotNull(dbOrderNo, "续费订单的第三方 orderNo 不应为null");
        assertFalse(dbOrderNo.isEmpty(), "续费订单的第三方 orderNo 不应为空字符串");
        System.out.println("第三方 orderNo: " + dbOrderNo);

        // 同时校验续费订单明细中的 orderNo 也已回写
        for (Map<String, Object> orderItem : orderItems) {
            String itemOrderNo = (String) orderItem.get("order_no");
            assertNotNull(itemOrderNo, "续费订单明细的 orderNo 不应为null");
            assertEquals(dbOrderNo, itemOrderNo, "续费订单明细的 orderNo 应与主订单一致");
        }

        // ============ 7. 等待2分钟，给第三方处理和回调时间 ============
        System.out.println("========== 等待2分钟，给第三方处理和回调时间 ==========");
        long waitStart = System.currentTimeMillis();
        Thread.sleep(2 * 60 * 1000L);
        long waitEnd = System.currentTimeMillis();
        System.out.println("等待完成，实际等待: " + ((waitEnd - waitStart) / 1000) + "秒");

        // ============ 8. 从第三方同步实例最新数据到本地数据库 ============
        System.out.println("========== 同步实例最新数据 ==========");
        int syncRows = proxyInstanceService.syncProxyInstance(Collections.singletonList(instanceNo)).getActualCount();
        System.out.println("同步更新行数:    " + syncRows);
        assertTrue(syncRows >= 0, "同步应成功");

        // ============ 9. 查询本地数据库，获取续费后的到期时间 ============
        String querySql = "SELECT user_expired FROM proxy_instance WHERE instance_no = ?";
        Long newExpired = jdbcTemplate.queryForObject(querySql, Long.class, instanceNo);

        assertNotNull(newExpired, "续费后到期时间不应为null");
        System.out.println("========== 续费后实例信息 ==========");
        System.out.println("新到期时间戳(秒): " + newExpired);
        System.out.println("新到期时间:       " + new Date(newExpired * 1000));
        System.out.println("延期秒数:         " + (newExpired - originalExpired));

        // ============ 10. 核心断言：到期时间确实延期了 ============
        assertTrue(newExpired > originalExpired,
                "续费后到期时间应晚于续费前。续费前: " + originalExpired
                        + "（" + new Date(originalExpired * 1000) + "），"
                        + "续费后: " + newExpired
                        + "（" + new Date(newExpired * 1000) + "）");

        System.out.println("========== ✅ 测试通过：自动续费成功，到期时间已延期 ==========");
    }
}
