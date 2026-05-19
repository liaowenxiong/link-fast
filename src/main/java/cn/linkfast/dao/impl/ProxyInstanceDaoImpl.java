package cn.linkfast.dao.impl;

import cn.linkfast.dao.ProxyInstanceDAO;
import cn.linkfast.dto.ProxyInstanceSearchCondition;
import cn.linkfast.entity.ProxyInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代理实例数据访问实现类
 *
 * @author liaowenxiong
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProxyInstanceDaoImpl implements ProxyInstanceDAO {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public int batchUpdate(List<ProxyInstance> instances) {
        log.info("批量更新代理实例数据，数量：{}", instances.size());
        if (instances.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE proxy_instance SET " +
                "order_no=?, proxy_type=?, protocol=?, ip=?, port=?, region_id=?, country_code=?, city_code=?, " +
                "use_type=?, username=?, pwd=?, user_expired=?, flow_total=?, flow_balance=?, status=?, bridges=?, "
                +
                "open_at=?, renew_at=?, release_at=?, product_no=?, extend_ip=?, project_id=?, " +
                "update_time=CURRENT_TIMESTAMP " +
                "WHERE instance_no=?";

        List<Object[]> batchArgs = instances.stream().map(i -> new Object[] {
                i.getOrderNo(),
                i.getProxyType(),
                i.getProtocol(),
                i.getIp(),
                i.getPort(),
                i.getRegionId(),
                i.getCountryCode(),
                i.getCityCode(),
                i.getUseType(),
                i.getUsername(),
                i.getPwd(),
                i.getUserExpired(),
                i.getFlowTotal(),
                i.getFlowBalance(),
                i.getStatus(),
                toJson(i.getBridges()),
                i.getOpenAt(),
                i.getRenewAt(),
                i.getReleaseAt(),
                i.getProductNo(),
                i.getExtendIp(),
                i.getProjectId(),
                i.getInstanceNo()
        }).collect(Collectors.toList());
        int[] results;
        try {
            results = jdbcTemplate.batchUpdate(sql, batchArgs);
        } catch (Exception e) {
            log.error("批量更新代理实例失败，待处理数量：{}", instances.size(), e);
            throw new RuntimeException("批量更新代理实例异常", e);
        }

        int actualChangeCount = 0;
        for (int r : results) {
            if (r > 0) {
                actualChangeCount++;
            }
        }
        log.info("批量更新代理实例数据，成功数量：{}", actualChangeCount);
        return actualChangeCount;
    }

    @Override
    public List<ProxyInstance> selectListByCondition(ProxyInstanceSearchCondition condition) {
        StringBuilder sql = new StringBuilder("SELECT * FROM proxy_instance WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        appendProxyTypeCondition(sql, params, condition.getProxyType());
        appendOptionalConditions(sql, params, condition);

        sql.append("ORDER BY create_time DESC LIMIT ? OFFSET ?");
        params.add(condition.getLimit());
        params.add(condition.getOffset());

        long startTime = System.currentTimeMillis();
        List<ProxyInstance> result = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(ProxyInstance.class), params.toArray());
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("selectListByCondition 查询耗时：{}ms", elapsed);
        return result;
    }

    @Override
    public int countByCondition(ProxyInstanceSearchCondition condition) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM proxy_instance WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        appendProxyTypeCondition(sql, params, condition.getProxyType());
        appendOptionalConditions(sql, params, condition);

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 拼接 proxyType IN (...) 条件（为空时不限制）
     */
    private void appendProxyTypeCondition(StringBuilder sql, List<Object> params, Integer[] proxyType) {
        if (proxyType != null && proxyType.length > 0) {
            String placeholders = String.join(",", java.util.Collections.nCopies(proxyType.length, "?"));
            sql.append("AND proxy_type IN (").append(placeholders).append(") ");
            params.addAll(Arrays.asList(proxyType));
        }
    }

    /**
     * 拼接可选查询条件（列表查询和统计查询共用）
     */
    private void appendOptionalConditions(StringBuilder sql, List<Object> params,
            ProxyInstanceSearchCondition condition) {
        if (condition.getStatus() != null) {
            sql.append("AND status = ? ");
            params.add(condition.getStatus());
        }
        if (condition.getCountryCode() != null && !condition.getCountryCode().isEmpty()) {
            sql.append("AND country_code = ? ");
            params.add(condition.getCountryCode());
        }
        if (condition.getCityCode() != null && !condition.getCityCode().isEmpty()) {
            sql.append("AND city_code = ? ");
            params.add(condition.getCityCode());
        }
        if (condition.getIp() != null && !condition.getIp().isEmpty()) {
            sql.append("AND ip LIKE ? ");
            params.add("%" + condition.getIp() + "%");
        }
        if (condition.getInstanceNo() != null && !condition.getInstanceNo().isEmpty()) {
            sql.append("AND instance_no = ? ");
            params.add(condition.getInstanceNo());
        }
    }

    /**
     * 根据实例编号更新备注
     */
    @Override
    public int updateRemarkByInstanceNo(String instanceNo, String remark) {
        String sql = "UPDATE proxy_instance SET remark = ?, update_time = CURRENT_TIMESTAMP WHERE instance_no = ?";
        int rows = jdbcTemplate.update(sql, remark, instanceNo);
        log.info(">>> 更新实例备注，instanceNo: {}，影响行数: {}", instanceNo, rows);
        return rows;
    }

    /**
     * 根据实例编号更新自动续费状态
     */
    @Override
    public int updateRenewByInstanceNo(String instanceNo, Integer renew) {
        String sql = "UPDATE proxy_instance SET renew = ?, update_time = CURRENT_TIMESTAMP WHERE instance_no = ?";
        int rows = jdbcTemplate.update(sql, renew, instanceNo);
        log.info(">>> 更新实例自动续费状态，instanceNo: {}，renew: {}，影响行数: {}", instanceNo, renew, rows);
        return rows;
    }

    /**
     * 查询已开启自动续费且即将到期的代理实例
     * 条件：renew = 1 且 user_expired 在当前时间到 N 天后的时间范围内
     */
    @Override
    public List<ProxyInstance> selectAutoRenewExpiringInstances(int days) {
        String sql = "SELECT * FROM proxy_instance WHERE renew = 1 " +
                "AND user_expired IS NOT NULL " +
                "AND user_expired > UNIX_TIMESTAMP(NOW()) " +
                "AND user_expired <= UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL ? DAY)) " +
                "ORDER BY user_expired";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ProxyInstance.class), days);
    }

    /**
     * 将对象（List）转换为 JSON 字符串存入数据库
     */
    private String toJson(Object obj) {
        if (obj == null)
            return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON转换失败: ", e);
            return "[]";
        }
    }
}
