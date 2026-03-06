package cn.linkfast.dao.impl;

import cn.linkfast.dao.ProxyProductDAO;
import cn.linkfast.entity.ProxyProduct;
import cn.linkfast.entity.ProxyProductSearchCondition;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liaowenxiong
 * @version 1.0
 * @description 代理产品数据访问实现类
 * @since 2026/3/6 14:23
 */
@Repository
@RequiredArgsConstructor
public class ProxyProductDaoImpl implements ProxyProductDAO {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int batchSaveOrUpdate(List<ProxyProduct> products) {
        // 修正点 1：空集合返回 0，确保符合 int 返回类型
        if (products == null || products.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO proxy_product (" + "product_no, product_name, proxy_type, use_type, protocol, use_limit, sell_limit, " + "area_code, country_code, state_code, city_code, detail, cost_price, inventory, " + "ip_type, isp_type, net_type, duration, unit, band_width, band_width_price, " + "max_band_width, flow, cpu, memory, enable, supplier_code, ip_count, ip_duration, " + "assign_ip, parent_no, cidr_status, one_day, cidr_blocks, offline_cidr_blocks, " + "proxy_everytime_change, proxy_global_random, api_draw_global_random, ip_white_list, " + "pwd_draw_proxy_user, proxy_user_flow_limit, flow_use_log, pwd_draw_session_range, " + "flow_conversion_base, project_list, product_type" + ") VALUES (" + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + ") ON DUPLICATE KEY UPDATE " + "product_name=VALUES(product_name), " + "proxy_type=VALUES(proxy_type), " + "use_type=VALUES(use_type), " + "protocol=VALUES(protocol), " + "use_limit=VALUES(use_limit), " + "sell_limit=VALUES(sell_limit), " + "area_code=VALUES(area_code), " + "country_code=VALUES(country_code), " + "state_code=VALUES(state_code), " + "city_code=VALUES(city_code), " + "detail=VALUES(detail), " + "cost_price=VALUES(cost_price), " + "inventory=VALUES(inventory), " + "ip_type=VALUES(ip_type), " + "isp_type=VALUES(isp_type), " + "net_type=VALUES(net_type), " + "duration=VALUES(duration), " + "unit=VALUES(unit), " + "band_width=VALUES(band_width), " + "band_width_price=VALUES(band_width_price), " + "max_band_width=VALUES(max_band_width), " + "flow=VALUES(flow), " + "cpu=VALUES(cpu), " + "memory=VALUES(memory), " + "enable=VALUES(enable), " + "supplier_code=VALUES(supplier_code), " + "ip_count=VALUES(ip_count), " + "ip_duration=VALUES(ip_duration), " + "assign_ip=VALUES(assign_ip), " + "parent_no=VALUES(parent_no), " + "cidr_status=VALUES(cidr_status), " + "one_day=VALUES(one_day), " + "cidr_blocks=VALUES(cidr_blocks), " + "offline_cidr_blocks=VALUES(offline_cidr_blocks), " + "proxy_everytime_change=VALUES(proxy_everytime_change), " + "proxy_global_random=VALUES(proxy_global_random), " + "api_draw_global_random=VALUES(api_draw_global_random), " + "ip_white_list=VALUES(ip_white_list), " + "pwd_draw_proxy_user=VALUES(pwd_draw_proxy_user), " + "proxy_user_flow_limit=VALUES(proxy_user_flow_limit), " + "flow_use_log=VALUES(flow_use_log), " + "pwd_draw_session_range=VALUES(pwd_draw_session_range), " + "flow_conversion_base=VALUES(flow_conversion_base), " + "project_list=VALUES(project_list), " + "product_type=VALUES(product_type), " + "update_time=CURRENT_TIMESTAMP";

        // 修正点 2：接收 batchUpdate 的执行结果
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                ProxyProduct p = products.get(i);
                int idx = 1;
                ps.setString(idx++, p.getProductNo());
                ps.setString(idx++, p.getProductName());
                ps.setObject(idx++, p.getProxyType());
                ps.setString(idx++, p.getUseType());
                ps.setString(idx++, p.getProtocol());
                ps.setObject(idx++, p.getUseLimit());
                ps.setObject(idx++, p.getSellLimit());
                ps.setString(idx++, p.getAreaCode());
                ps.setString(idx++, p.getCountryCode());
                ps.setString(idx++, p.getStateCode());
                ps.setString(idx++, p.getCityCode());
                ps.setString(idx++, p.getDetail());
                ps.setBigDecimal(idx++, p.getCostPrice());
                ps.setObject(idx++, p.getInventory());
                ps.setObject(idx++, p.getIpType());
                ps.setObject(idx++, p.getIspType());
                ps.setObject(idx++, p.getNetType());
                ps.setObject(idx++, p.getDuration());
                ps.setObject(idx++, p.getUnit());
                ps.setObject(idx++, p.getBandWidth());
                ps.setBigDecimal(idx++, p.getBandWidthPrice());
                ps.setObject(idx++, p.getMaxBandWidth());
                ps.setObject(idx++, p.getFlow());
                ps.setObject(idx++, p.getCpu());
                ps.setBigDecimal(idx++, p.getMemory());
                ps.setObject(idx++, p.getEnable());
                ps.setString(idx++, p.getSupplierCode());
                ps.setObject(idx++, p.getIpCount());
                ps.setObject(idx++, p.getIpDuration());
                ps.setObject(idx++, p.getAssignIp());
                ps.setString(idx++, p.getParentNo());
                ps.setObject(idx++, p.getCidrStatus());
                ps.setObject(idx++, p.getOneDay());
                ps.setString(idx++, p.getCidrBlocks());
                ps.setString(idx++, p.getOfflineCidrBlocks());
                ps.setObject(idx++, p.getProxyEverytimeChange());
                ps.setObject(idx++, p.getProxyGlobalRandom());
                ps.setObject(idx++, p.getApiDrawGlobalRandom());
                ps.setObject(idx++, p.getIpWhiteList());
                ps.setObject(idx++, p.getPwdDrawProxyUser());
                ps.setObject(idx++, p.getProxyUserFlowLimit());
                ps.setObject(idx++, p.getFlowUseLog());
                ps.setString(idx++, p.getPwdDrawSessionRange());
                ps.setObject(idx++, p.getFlowConversionBase());
                ps.setString(idx++, p.getProjectList());
                ps.setObject(idx++, p.getProductType());
            }

            @Override
            public int getBatchSize() {
                return products.size();
            }
        });

        int successCount = 0;
        for (int r : results) {
            // r = 1 (新增), r = 2 (更新), r = 0 (无变化)
            // 只要 r >= 0，都说明这条记录在数据库里处理成功了
            if (r >= 0) {
                successCount++;
            }
        }

        return successCount;
    }

    @Override
    public List<ProxyProduct> findProxyProductList(ProxyProductSearchCondition condition) {
        // 1. 构造基础 SQL
        // 使用 WHERE 1=1 是为了方便后续动态拼接 AND 条件
        StringBuilder sql = new StringBuilder("SELECT * FROM proxy_product WHERE country_code = ? AND city_code = ?");
        List<Object> params = new ArrayList<>();

        // 2. 添加必传参数
        params.add(condition.getCountryCode());
        params.add(condition.getCityCode());

        // 3. 动态拼接分页逻辑
        // 只有当 limit 和 offset 都不为 null 时，才执行数据库分页
        // 如果前端没传 pageSize/page，这里就不会拼 LIMIT，从而实现“查询全部数据”
        if (condition.getLimit() != null && condition.getOffset() != null) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(condition.getLimit());
            params.add(condition.getOffset());
        }

        // 4. 执行查询并返回结果
        // BeanPropertyRowMapper 会自动处理数据库下划线字段到 Java 驼峰命名的映射
        return jdbcTemplate.query(
                sql.toString(),
                new org.springframework.jdbc.core.BeanPropertyRowMapper<>(ProxyProduct.class),
                params.toArray()
        );
    }

    @Override
    public int countProxyProduct(ProxyProductSearchCondition condition) {
        String sql = "SELECT COUNT(*) FROM proxy_product WHERE country_code = ? AND city_code = ?";
        // 注意：Count 语句不需要拼 LIMIT 和 OFFSET
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                condition.getCountryCode(), condition.getCityCode());
        return count != null ? count : 0;
    }
}