package cn.linkfast.dao.impl;

import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.entity.ProxyOrder;
import cn.linkfast.entity.ProxyOrderInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProxyOrderDaoImpl implements ProxyOrderDAO {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveOrder(ProxyOrder order) {
        // 1. 保存或更新主表数据
        String orderSql = "INSERT INTO proxy_order (order_no, app_order_no, user_id, order_type, status, product_count, amount, has_refund, instance_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status=VALUES(status), amount=VALUES(amount), has_refund=VALUES(has_refund), instance_total=VALUES(instance_total)";

        List<Object> params = new ArrayList<>();
        params.add(order.getOrderNo());
        params.add(order.getAppOrderNo());
        params.add(order.getUserId());
        params.add(order.getOrderType());
        params.add(order.getStatus());
        params.add(order.getProductCount());
        params.add(order.getAmount());
        params.add(order.getHasRefund());
        params.add(order.getInstanceTotal());

        jdbcTemplate.update(orderSql, params.toArray());

        // 2. 从 order 对象中获取实例列表进行批量处理
        List<ProxyOrderInstance> instances = order.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn(">>> 订单 {} 不包含任何实例数据，跳过子表更新", order.getOrderNo());
            return;
        }

        String instSql = "INSERT INTO proxy_order_instance (order_no, instance_no, proxy_type, protocol, ip, port, region_id, country_code, city_code, " +
                "use_type, username, pwd, user_expired, flow_total, flow_balance, status, renew, bridges, open_at, renew_at, release_at, " +
                "product_no, extend_ip, project_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status=VALUES(status), flow_balance=VALUES(flow_balance), renew_at=VALUES(renew_at), release_at=VALUES(release_at)";

        List<Object[]> batchArgs = instances.stream().map(i -> new Object[]{
                i.getOrderNo(), i.getInstanceNo(), i.getProxyType(), i.getProtocol(), i.getIp(), i.getPort(),
                i.getRegionId(), i.getCountryCode(), i.getCityCode(), i.getUseType(), i.getUsername(), i.getPwd(),
                i.getUserExpired(), i.getFlowTotal(), i.getFlowBalance(), i.getStatus(), i.getRenew(),
                i.getBridges(), i.getOpenAt(), i.getRenewAt(), i.getReleaseAt(), i.getProductNo(),
                i.getExtendIp(), i.getProjectId()
        }).collect(Collectors.toList());

        jdbcTemplate.batchUpdate(instSql, batchArgs);
        log.info(">>> 订单及 {} 个实例已成功持久化，单号: {}", instances.size(), order.getOrderNo());
    }
}