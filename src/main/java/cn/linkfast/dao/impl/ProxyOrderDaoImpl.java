package cn.linkfast.dao.impl;

import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.dto.ProxyOrderSearchCondition;
import cn.linkfast.dto.ProxyOrderUpdateResultDTO;
import cn.linkfast.entity.ProxyInstance;
import cn.linkfast.entity.ProxyOrder;
import cn.linkfast.entity.ProxyPurchaseOrderItem;
import cn.linkfast.entity.ProxyRenewOrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
    public ProxyOrderUpdateResultDTO updateProxyPurchaseOrderByAppOrderNo(ProxyOrder order) {
        // 仅更新订单主表（proxy_order 在此场景下不存在插入的情况）
        String orderSql = "UPDATE proxy_order SET order_no=?, order_type=?, status=?, total_quantity=?, amount=?, has_refund=?, instance_total=? WHERE app_order_no=?";
        List<Object> params = new ArrayList<>();
        params.add(order.getOrderNo());
        params.add(order.getOrderType());
        params.add(order.getStatus());
        params.add(order.getTotalQuantity());
        params.add(order.getAmount());
        params.add(order.getHasRefund());
        params.add(order.getInstanceTotal());
        params.add(order.getAppOrderNo());

        // 1. 更新主表数据
        int proxyOrderUpdatedRows = jdbcTemplate.update(orderSql, params.toArray());
        log.info(">>> 订单已更新，单号: {}，影响行数: {}", order.getOrderNo(), proxyOrderUpdatedRows);

        // 2. 从 order 对象中获取实例列表进行批量处理
        List<ProxyInstance> instances = order.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn(">>> 订单 {} 不包含任何实例数据，跳过子表更新", order.getOrderNo());
            return new ProxyOrderUpdateResultDTO(proxyOrderUpdatedRows, 0, 0);
        }

        String instSql = "INSERT INTO proxy_instance (order_id, order_no, app_order_no, user_id, instance_no, proxy_type, protocol, ip, port, region_id, country_code, city_code, " + "use_type,unit,duration,username, pwd, user_expired, flow_total, flow_balance, status, renew, bridges, open_at, renew_at, release_at, " + "product_no, extend_ip, project_id) " + "VALUES (?,?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE status=VALUES(status), flow_balance=VALUES(flow_balance), user_expired=VALUES(user_expired), renew_at=VALUES(renew_at), release_at=VALUES(release_at)";

        List<Object[]> batchArgs = instances.stream().map(i -> new Object[]{i.getOrderId(), i.getOrderNo(), i.getAppOrderNo(), i.getUserId(), i.getInstanceNo(), i.getProxyType(), i.getProtocol(), i.getIp(), i.getPort(), i.getRegionId(), i.getCountryCode(), i.getCityCode(), i.getUseType(), i.getUnit(), i.getDuration(), i.getUsername(), i.getPwd(), i.getUserExpired(), i.getFlowTotal(), i.getFlowBalance(), i.getStatus(), i.getRenew(), toJson(i.getBridges()), i.getOpenAt(), i.getRenewAt(), i.getReleaseAt(), i.getProductNo(), i.getExtendIp(), i.getProjectId()}).collect(Collectors.toList());

        int[] results = jdbcTemplate.batchUpdate(instSql, batchArgs);
        log.info(">>> 订单 {} 的实例已成功持久化，数量: {}", order.getOrderNo(), instances.size());

        // 3. 统计实例表更新的行数
        int proxyInstanceUpdatedRows = 0;
        for (int r : results) {
            // r = 1 (新增), r = 2 (更新), r = 0 (无变化)
            // 只要 r >= 0，都说明这条记录在数据库里处理成功了
            if (r > 0) {
                proxyInstanceUpdatedRows++;
            }
        }
        log.info(">>> 订单及 {} 个实例已成功持久化，单号: {}", instances.size(), order.getOrderNo());
        return new ProxyOrderUpdateResultDTO(proxyOrderUpdatedRows, proxyInstanceUpdatedRows, 0);
    }


    /**
     * 根据条件查询代理订单列表
     *
     * @param condition 查询条件
     * @return 订单列表
     */
    @Override
    public List<ProxyOrder> selectListByCondition(ProxyOrderSearchCondition condition) {
        // 1. 动态拼接SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM proxy_order WHERE 1=1 ");

        // 存储动态参数（避免SQL注入）
        List<Object> params = new ArrayList<>();

        // 2. 拼接查询条件
        // 非必传条件：status
        if (condition.getStatus() != null) {
            sql.append("AND status = ? ");
            params.add(condition.getStatus());
        }

        // 非必传条件：orderType
        if (condition.getOrderType() != null) {
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
        log.info("订单查询语句：" + sql);
        return jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(ProxyOrder.class), params.toArray());
    }

    /**
     * 查询订单总数（仅统计符合条件的条数）
     */
    @Override
    public int countByCondition(ProxyOrderSearchCondition condition) {
        // 1. 动态拼接统计SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(1) FROM proxy_order WHERE 1=1 ");

        // 存储动态参数
        List<Object> params = new ArrayList<>();

        // 2. 拼接查询条件（与列表查询保持一致）
        if (condition.getStatus() != null) {
            sql.append("AND status = ? ");
            params.add(condition.getStatus());
        }

        if (condition.getOrderType() != null) {
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

    /**
     * 代理购买业务，回写第三方返回的 orderNo 和 amount（同时更新 proxy_order 和 proxy_purchase_order_item）
     */
    @Override
    public ProxyOrderUpdateResultDTO updateProxyPurchaseOrderByAppOrderNo(String appOrderNo, String orderNo, BigDecimal amount) {
        // 1. 更新主订单表
        String orderSql = "UPDATE proxy_order SET order_no=?, amount=? WHERE app_order_no=?";
        int orderUpdatedRows = jdbcTemplate.update(orderSql, orderNo, amount, appOrderNo);
        log.info(">>> 回写代理订单表，appOrderNo: {}，orderNo: {}，影响行数: {}", appOrderNo, orderNo, orderUpdatedRows);

        // 2. 更新订单明细表
        String itemSql = "UPDATE proxy_purchase_order_item SET order_no=? WHERE app_order_no=?";
        int orderItemUpdatedRows = jdbcTemplate.update(itemSql, orderNo, appOrderNo);
        log.info(">>> 回写代理购买订单明细表，appOrderNo: {}，orderNo: {}，影响行数: {}", appOrderNo, orderNo, orderItemUpdatedRows);

        return new ProxyOrderUpdateResultDTO(orderUpdatedRows, 0, orderItemUpdatedRows);
    }

    /**
     * 代理续费业务，回写第三方返回的 orderNo 和 amount（同时更新 proxy_order 和 proxy_renew_order_item）
     */
    @Override
    public ProxyOrderUpdateResultDTO updateProxyRenewOrderByAppOrderNo(String appOrderNo, String orderNo, BigDecimal amount) {
        // 1. 更新主订单表
        String orderSql = "UPDATE proxy_order SET order_no=?, amount=? WHERE app_order_no=?";
        int orderUpdatedRows = jdbcTemplate.update(orderSql, orderNo, amount, appOrderNo);
        log.info(">>> 回写代理订单表，appOrderNo: {}，orderNo: {}，影响行数: {}", appOrderNo, orderNo, orderUpdatedRows);

        // 2. 更新订单明细表
        String itemSql = "UPDATE proxy_renew_order_item SET order_no=? WHERE app_order_no=?";
        int orderItemUpdatedRows = jdbcTemplate.update(itemSql, orderNo, appOrderNo);
        log.info(">>> 回写代理续费订单明细表，appOrderNo: {}，orderNo: {}，影响行数: {}", appOrderNo, orderNo, orderItemUpdatedRows);

        return new ProxyOrderUpdateResultDTO(orderUpdatedRows, 0, orderItemUpdatedRows);
    }

    /**
     * 保存订单主数据和项目数据到数据库
     * 用于开通代理等新建订单场景
     *
     * @param order 包含主表信息和 purchaseItems 列表的订单对象
     * @return 保存的订单的 appOrderNo（便于后续业务使用）
     */
    public String insertOrderWithItems(ProxyOrder order) {

        // 1. 插入主表数据（纯插入，不做更新）
        String orderSql = "INSERT INTO proxy_order (order_no, app_order_no, user_id, order_type, status, total_quantity, amount, has_refund, instance_total, create_time, update_time) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        List<Object> params = new ArrayList<>();
        params.add(order.getOrderNo());
        params.add(order.getAppOrderNo());
        params.add(order.getUserId());
        params.add(order.getOrderType());
        params.add(order.getStatus());
        params.add(order.getTotalQuantity());
        params.add(order.getAmount());
        params.add(order.getHasRefund());
        params.add(order.getInstanceTotal());

        int proxyOrderInsertedRows = jdbcTemplate.update(orderSql, params.toArray());
        log.info(">>> 订单已新建，渠道商订单号: {}，影响行数: {}", order.getAppOrderNo(), proxyOrderInsertedRows);

        // 2. 获取并保存订单项目数据
        List<ProxyPurchaseOrderItem> items = order.getPurchaseItems();
        if (items != null && !items.isEmpty()) {
            String itemSql = "INSERT INTO proxy_purchase_order_item (app_order_no, product_no, proxy_type, use_type, protocol, use_limit, " + "area_code, country_code, state_code, city_code, detail, count,cycle_times,cost_price, retail_price, " + "ip_type, isp_type, net_type, duration, unit, band_width, band_width_price, max_band_width, flow, " + "cpu, memory, supplier_code, ip_count, ip_duration, parent_no, proxy_everytime_change, " + "proxy_global_random,projectId) " + "VALUES (?, ?, ?,?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?)";

            List<Object[]> batchArgs = items.stream().map(item -> new Object[]{item.getAppOrderNo(), item.getProductNo(), item.getProxyType(), item.getUseType(), item.getProtocol(), item.getUseLimit(), item.getAreaCode(), item.getCountryCode(), item.getStateCode(), item.getCityCode(), item.getDetail(), item.getCount(), item.getCycleTimes(), item.getCostPrice(), item.getRetailPrice(), item.getIpType(), item.getIspType(), item.getNetType(), item.getDuration(), item.getUnit(), item.getBandWidth(), item.getBandWidthPrice(), item.getMaxBandWidth(), item.getFlow(), item.getCpu(), item.getMemory(), item.getSupplierCode(), item.getIpCount(), item.getIpDuration(), item.getParentNo(), item.getProxyEverytimeChange(), item.getProxyGlobalRandom(), item.getProjectId()}).collect(Collectors.toList());

            jdbcTemplate.batchUpdate(itemSql, batchArgs);
            log.info(">>> 订单 {} 的项目已成功新建，数量: {}", order.getAppOrderNo(), items.size());
        }

        log.info(">>> 订单保存完成，appOrderNo: {}", order.getAppOrderNo());
        return order.getAppOrderNo();
    }

    /**
     * 根据渠道商订单号查询单个订单
     *
     * @param appOrderNo 渠道商订单号
     * @return 订单实体，不存在则返回 null
     */
    @Override
    public ProxyOrder selectByAppOrderNo(String appOrderNo) {
        String sql = "SELECT * FROM proxy_order WHERE app_order_no = ?";
        List<ProxyOrder> results = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ProxyOrder.class), appOrderNo);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 仅将主订单数据插入 proxy_order 表，返回数据库自增生成的主键 id
     *
     * @param order 主订单对象
     * @return 自增主键 id
     */
    @Override
    public Long insertOrder(ProxyOrder order) {
        String sql = "INSERT INTO proxy_order (order_no, app_order_no, user_id, order_type, status, total_quantity, amount, has_refund, instance_total, create_time, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, order.getOrderNo());
            ps.setObject(2, order.getAppOrderNo());
            ps.setObject(3, order.getUserId());
            ps.setObject(4, order.getOrderType());
            ps.setObject(5, order.getStatus());
            ps.setObject(6, order.getTotalQuantity());
            ps.setObject(7, order.getAmount());
            ps.setObject(8, order.getHasRefund());
            ps.setObject(9, order.getInstanceTotal());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        Long id = key != null ? key.longValue() : null;
        log.info(">>> 主订单已插入，appOrderNo: {}，生成主键 id: {}", order.getAppOrderNo(), id);
        return id;
    }

    /**
     * 将 ProxyOrder 中的 purchaseItems 批量插入 proxy_purchase_order_item 表
     *
     * @param order 包含 purchaseItems 的订单对象
     * @return 实际插入的行数
     */
    @Override
    public int insertProxyPurchaseOrderItems(ProxyOrder order) {
        List<ProxyPurchaseOrderItem> items = order.getPurchaseItems();
        if (items == null || items.isEmpty()) {
            log.warn(">>> purchaseItems 为空，跳过插入，appOrderNo: {}", order.getAppOrderNo());
            return 0;
        }
        String sql = "INSERT INTO proxy_purchase_order_item "
                + "(order_id, order_no, app_order_no, product_no, proxy_type, use_type, protocol, use_limit, "
                + "area_code, country_code, state_code, city_code, detail, cost_price, retail_price, "
                + "ip_type, isp_type, net_type, duration, unit, count, cycle_times, "
                + "band_width, band_width_price, max_band_width, flow, use_bridge, "
                + "cpu, memory, supplier_code, ip_count, ip_duration, parent_no, "
                + "proxy_everytime_change, proxy_global_random, project_id, create_time, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        List<Object[]> batchArgs = items.stream().map(item -> new Object[]{
                item.getOrderId(),
                item.getOrderNo(),
                item.getAppOrderNo(),
                item.getProductNo(),
                item.getProxyType(),
                item.getUseType(),
                item.getProtocol(),
                item.getUseLimit(),
                item.getAreaCode(),
                item.getCountryCode(),
                item.getStateCode(),
                item.getCityCode(),
                item.getDetail(),
                item.getCostPrice(),
                item.getRetailPrice(),
                item.getIpType(),
                item.getIspType(),
                item.getNetType(),
                item.getDuration(),
                item.getUnit(),
                item.getCount(),
                item.getCycleTimes(),
                item.getBandWidth(),
                item.getBandWidthPrice(),
                item.getMaxBandWidth(),
                item.getFlow(),
                item.getUseBridge(),
                item.getCpu(),
                item.getMemory(),
                item.getSupplierCode(),
                item.getIpCount(),
                item.getIpDuration(),
                item.getParentNo(),
                item.getProxyEverytimeChange(),
                item.getProxyGlobalRandom(),
                item.getProjectId()
        }).collect(Collectors.toList());
        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        int affectedRows = 0;
        for (int r : results) {
            if (r > 0) affectedRows++;
        }
        log.info(">>> 订单明细已插入，appOrderNo: {}，插入行数: {}", order.getAppOrderNo(), affectedRows);
        return affectedRows;
    }

    /**
     * 根据渠道商订单号查询代理购买订单明细列表
     */
    @Override
    public List<cn.linkfast.entity.ProxyPurchaseOrderItem> selectPurchaseItemsByAppOrderNo(String appOrderNo) {
        String sql = "SELECT * FROM proxy_purchase_order_item WHERE app_order_no = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(cn.linkfast.entity.ProxyPurchaseOrderItem.class), appOrderNo);
    }

    /**
     * 将 ProxyOrder 中的 releaseOrderItems 批量插入 proxy_release_order_item 表
     */
    @Override
    public int insertProxyReleaseOrderItems(ProxyOrder order) {
        List<cn.linkfast.entity.ProxyReleaseOrderItem> items = order.getReleaseOrderItems();
        if (items == null || items.isEmpty()) {
            log.warn(">>> releaseOrderItems 为空，跳过插入，appOrderNo: {}", order.getAppOrderNo());
            return 0;
        }
        String sql = "INSERT INTO proxy_release_order_item "
                + "(order_id, order_no, app_order_no, instance_no, total_amount, create_time, update_time) "
                + "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
        List<Object[]> batchArgs = items.stream().map(item -> new Object[]{
                item.getOrderId(),
                item.getOrderNo(),
                item.getAppOrderNo(),
                item.getInstanceNo(),
                item.getTotalAmount()
        }).collect(Collectors.toList());
        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        int affectedRows = 0;
        for (int r : results) {
            if (r > 0) affectedRows++;
        }
        log.info(">>> proxyReleaseOrderItem 插入完成，appOrderNo: {}，插入行数: {}", order.getAppOrderNo(), affectedRows);
        return affectedRows;
    }

    /**
     * 代理释放业务，回写第三方返回的 orderNo 和 amount（同时更新 proxy_order 和 proxy_release_order_item）
     */
    @Override
    public ProxyOrderUpdateResultDTO updateProxyReleaseOrderByAppOrderNo(String appOrderNo, String orderNo, java.math.BigDecimal amount) {
        String orderSql = "UPDATE proxy_order SET order_no=?, amount=? WHERE app_order_no=?";
        int orderUpdatedRows = jdbcTemplate.update(orderSql, orderNo, amount, appOrderNo);
        log.info(">>> 回写代理释放订单表，appOrderNo: {}，orderNo: {}，影响行数: {}", appOrderNo, orderNo, orderUpdatedRows);

        String itemSql = "UPDATE proxy_release_order_item SET order_no=? WHERE app_order_no=?";
        int orderItemUpdatedRows = jdbcTemplate.update(itemSql, orderNo, appOrderNo);
        log.info(">>> 回写代理释放订单明细表，appOrderNo: {}，orderNo: {}，影响行数: {}", appOrderNo, orderNo, orderItemUpdatedRows);

        return new ProxyOrderUpdateResultDTO(orderUpdatedRows, 0, orderItemUpdatedRows);
    }

    /**
     * 将 ProxyOrder 中的 renewItems 批量插入 proxy_renew_order_item 表
     *
     * @param order 包含 renewItems 的订单对象
     * @return 实际插入的行数
     */
    @Override
    public int insertProxyRenewOrderItems(ProxyOrder order) {
        List<ProxyRenewOrderItem> items = order.getRenewItems();
        if (items == null || items.isEmpty()) {
            log.warn(">>> renewItems 为空，跳过插入，appOrderNo: {}", order.getAppOrderNo());
            return 0;
        }
        String sql = "INSERT INTO proxy_renew_order_item "
                + "(order_id, order_no, app_order_no, instance_no, duration, unit, cycle_times, renew_amount, create_time, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        List<Object[]> batchArgs = items.stream().map(item -> new Object[]{
                item.getOrderId(),
                item.getOrderNo(),
                item.getAppOrderNo(),
                item.getInstanceNo(),
                item.getDuration(),
                item.getUnit(),
                item.getCycleTimes(),
                item.getRenewAmount()
        }).collect(Collectors.toList());
        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        int affectedRows = 0;
        for (int r : results) {
            if (r > 0) affectedRows++;
        }
        log.info(">>> proxyRenewOrderItem 插入完成，appOrderNo: {}，插入行数: {}", order.getAppOrderNo(), affectedRows);
        return affectedRows;
    }

}