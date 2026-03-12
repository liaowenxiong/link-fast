package cn.linkfast.dao.impl;

import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.dto.OrderUpdateResultDTO;
import cn.linkfast.dto.ProxyOrderSearchCondition;
import cn.linkfast.entity.ProxyOrder;
import cn.linkfast.entity.ProxyOrderInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderUpdateResultDTO saveOrder(ProxyOrder order) {
        String orderSql = "INSERT INTO proxy_order (order_no, app_order_no, user_id, order_type, status, product_count, amount, has_refund, instance_total) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE status=VALUES(status), amount=VALUES(amount), has_refund=VALUES(has_refund), instance_total=VALUES(instance_total)";

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

        // 1. 保存或更新主表数据
        int proxyOrderUpdatedRows = jdbcTemplate.update(orderSql, params.toArray());
        log.info(">>> 订单已成功持久化，单号: {}", order.getOrderNo());

        // 2. 从 order 对象中获取实例列表进行批量处理
        List<ProxyOrderInstance> instances = order.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn(">>> 订单 {} 不包含任何实例数据，跳过子表更新", order.getOrderNo());
            return new OrderUpdateResultDTO(proxyOrderUpdatedRows, 0);
        }

        String instSql = "INSERT INTO proxy_order_instance (order_no, instance_no, proxy_type, protocol, ip, port, region_id, country_code, city_code, " + "use_type, username, pwd, user_expired, flow_total, flow_balance, status, renew, bridges, open_at, renew_at, release_at, " + "product_no, extend_ip, project_id) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE status=VALUES(status), flow_balance=VALUES(flow_balance), renew_at=VALUES(renew_at), release_at=VALUES(release_at)";

        List<Object[]> batchArgs = instances.stream().map(i -> new Object[]{i.getOrderNo(), i.getInstanceNo(), i.getProxyType(), i.getProtocol(), i.getIp(), i.getPort(), i.getRegionId(), i.getCountryCode(), i.getCityCode(), i.getUseType(), i.getUsername(), i.getPwd(), i.getUserExpired(), i.getFlowTotal(), i.getFlowBalance(), i.getStatus(), i.getRenew(), toJson(i.getBridges()), i.getOpenAt(), i.getRenewAt(), i.getReleaseAt(), i.getProductNo(), i.getExtendIp(), i.getProjectId()}).collect(Collectors.toList());

        int[] results = jdbcTemplate.batchUpdate(instSql, batchArgs);
        log.info(">>> 订单 {} 的实例已成功持久化，数量: {}", order.getOrderNo(), instances.size());

        // 3. 统计实例表更新的行数
        int proxyInstanceUpdatedRows = 0;
        for (int r : results) {
            // r = 1 (新增), r = 2 (更新), r = 0 (无变化)
            // 只要 r >= 0，都说明这条记录在数据库里处理成功了
            if (r >= 0) {
                proxyInstanceUpdatedRows++;
            }
        }
        log.info(">>> 订单及 {} 个实例已成功持久化，单号: {}", instances.size(), order.getOrderNo());
        return new OrderUpdateResultDTO(proxyOrderUpdatedRows, proxyInstanceUpdatedRows);
    }


    /**
     * 根据条件查询代理订单列表
     *
     * @param condition 查询条件
     * @return 订单列表
     */
    @Override
    public List<ProxyOrder> findProxyOrderList(ProxyOrderSearchCondition condition) {
        // 1. 动态拼接SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM proxy_order WHERE 1=1 ");

        // 存储动态参数（避免SQL注入）
        List<Object> params = new ArrayList<>();

        // 2. 拼接查询条件
        // 必传条件：status
        sql.append("AND status = ? ");
        params.add(condition.getStatus());

        // 非必传条件：orderType
        if (condition.getOrderType() != null && !condition.getOrderType().isEmpty()) {
            sql.append("AND order_type = ? ");
            params.add(condition.getOrderType());
        }

        // 非必传条件：orderNo
        if (condition.getOrderNo() != null && !condition.getOrderNo().isEmpty()) {
            sql.append("AND order_no = ? ");
            params.add(condition.getOrderNo());
        }

        // 3. 拼接分页（MySQL LIMIT）
        sql.append("ORDER BY create_time DESC LIMIT ?, ?");
        params.add(condition.getOffset());
        params.add(condition.getLimit());

        // 4. 执行查询并映射为实体（BeanPropertyRowMapper自动映射驼峰字段）
        return jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(ProxyOrder.class), params.toArray());
    }

    /**
     * 查询订单总数（仅统计符合条件的条数）
     */
    @Override
    public int countProxyOrder(ProxyOrderSearchCondition condition) {
        // 1. 动态拼接统计SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(1) FROM proxy_order WHERE 1=1 ");

        // 存储动态参数
        List<Object> params = new ArrayList<>();

        // 2. 拼接查询条件（与列表查询保持一致）
        sql.append("AND status = ? ");
        params.add(condition.getStatus());

        if (condition.getOrderType() != null && !condition.getOrderType().isEmpty()) {
            sql.append("AND order_type = ? ");
            params.add(condition.getOrderType());
        }

        if (condition.getOrderNo() != null && !condition.getOrderNo().isEmpty()) {
            sql.append("AND order_no = ? ");
            params.add(condition.getOrderNo());
        }

        // 3. 执行统计查询
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }


    /**
     * 将对象（List）转换为 JSON 字符串存入数据库
     */
    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON转换失败: ", e);
            return "[]";
        }
    }
}