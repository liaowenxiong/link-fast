package cn.linkfast.dao.impl;

import cn.linkfast.dao.ProxyRegionDAO;
import cn.linkfast.entity.ProxyRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 地域数据访问实现类
 * 对应表：proxy_region
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProxyRegionDaoImpl implements ProxyRegionDAO {

    /**
     * 单次 JDBC batch 的最大条数。数据量大时若一次 batch 过久，易触发 MySQL / 中间网络
     * {@code net_write_timeout}、连接空闲超时等导致 {@code Communications link failure}，故分块提交。
     */
    private static final int BATCH_CHUNK_SIZE = 200;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int batchSaveOrUpdate(List<ProxyRegion> proxyRegions) {
        log.info("批量插入/更新地域数据，数量：{}", proxyRegions == null ? 0 : proxyRegions.size());
        if (proxyRegions == null || proxyRegions.isEmpty()) {
            return 0;
        }

        int totalSuccess = 0;
        for (int from = 0; from < proxyRegions.size(); from += BATCH_CHUNK_SIZE) {
            int to = Math.min(from + BATCH_CHUNK_SIZE, proxyRegions.size());
            List<ProxyRegion> chunk = new ArrayList<>(proxyRegions.subList(from, to));
            totalSuccess += batchSaveOrUpdateChunk(chunk);
        }
        log.info("批量插入/更新地域数据完成（分块 size={}），累计成功数量：{}", BATCH_CHUNK_SIZE, totalSuccess);
        return totalSuccess;
    }

    private int batchSaveOrUpdateChunk(List<ProxyRegion> proxyRegions) {
        // 注意：ON DUPLICATE KEY 依赖 region_code 的唯一索引（uk_region_code）
        String sql = "INSERT INTO proxy_region (" +
                "parent_id, level, region_code, region_name, region_en_name, sort, full_code, full_name, status" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "parent_id=VALUES(parent_id), " +
                "level=VALUES(level), " +
                "region_name=VALUES(region_name), " +
                "region_en_name=VALUES(region_en_name), " +
                "sort=VALUES(sort), " +
                "full_code=VALUES(full_code), " +
                "full_name=VALUES(full_name), " +
                "status=VALUES(status), " +
                "update_time=CURRENT_TIMESTAMP";

        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                ProxyRegion r = proxyRegions.get(i);
                int idx = 1;
                ps.setObject(idx++, r.getParentId());
                ps.setObject(idx++, r.getLevel());
                ps.setString(idx++, r.getRegionCode());
                ps.setString(idx++, r.getRegionName());
                ps.setString(idx++, r.getRegionEnName());
                ps.setObject(idx++, r.getSort());
                ps.setString(idx++, r.getFullCode());
                ps.setString(idx++, r.getFullName());
                ps.setObject(idx++, r.getStatus());
            }

            @Override
            public int getBatchSize() {
                return proxyRegions.size();
            }
        });
        int successCount = 0;
        for (int x : results) {
            // ON DUPLICATE KEY UPDATE 返回值：1=插入，2=更新，0=无变化，-2=成功但无法确认行数
            // 只要不是异常（batchUpdate 不会为负数以外的错误），均视为处理成功
            if (x >= 0 || x == Statement.SUCCESS_NO_INFO) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Long> selectIdMapByRegionCodes(List<String> regionCodes) {
        if (regionCodes == null || regionCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> idMap = new java.util.HashMap<>();
        for (int from = 0; from < regionCodes.size(); from += BATCH_CHUNK_SIZE) {
            int to = Math.min(from + BATCH_CHUNK_SIZE, regionCodes.size());
            List<String> chunkCodes = regionCodes.subList(from, to);

            String placeholders = String.join(",", Collections.nCopies(chunkCodes.size(), "?"));
            String sql = "SELECT id, region_code FROM proxy_region WHERE region_code IN (" + placeholders + ")";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, chunkCodes.toArray());
            rows.forEach(m -> idMap.put(
                    (String) m.get("region_code"),
                    ((Number) m.get("id")).longValue()
            ));
        }

        return idMap;
    }

    @Override
    public ProxyRegion selectByRegionCode(String regionCode) {
        if (regionCode == null || regionCode.isEmpty()) {
            return null;
        }
        String sql = "SELECT id, parent_id, level, region_code, region_name, region_en_name, " +
                "sort, full_code, full_name, status, create_time, update_time " +
                "FROM proxy_region WHERE region_code = ?";
        List<ProxyRegion> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProxyRegion r = new ProxyRegion();
            r.setId(rs.getLong("id"));
            r.setParentId(rs.getLong("parent_id"));
            r.setLevel(rs.getInt("level"));
            r.setRegionCode(rs.getString("region_code"));
            r.setRegionName(rs.getString("region_name"));
            r.setRegionEnName(rs.getString("region_en_name"));
            r.setSort(rs.getInt("sort"));
            r.setFullCode(rs.getString("full_code"));
            r.setFullName(rs.getString("full_name"));
            r.setStatus(rs.getInt("status"));
            r.setCreateTime(rs.getTimestamp("create_time"));
            r.setUpdateTime(rs.getTimestamp("update_time"));
            return r;
        }, regionCode);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Map<String, ProxyRegion> selectByRegionCodes(List<String> regionCodes) {
        if (regionCodes == null || regionCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        // 去重
        List<String> uniqueCodes = regionCodes.stream().distinct().toList();

        String placeholders = String.join(",", Collections.nCopies(uniqueCodes.size(), "?"));
        String sql = "SELECT id, parent_id, level, region_code, region_name, region_en_name, " +
                "sort, full_code, full_name, status, create_time, update_time " +
                "FROM proxy_region WHERE region_code IN (" + placeholders + ")";

        List<ProxyRegion> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProxyRegion r = new ProxyRegion();
            r.setId(rs.getLong("id"));
            r.setParentId(rs.getLong("parent_id"));
            r.setLevel(rs.getInt("level"));
            r.setRegionCode(rs.getString("region_code"));
            r.setRegionName(rs.getString("region_name"));
            r.setRegionEnName(rs.getString("region_en_name"));
            r.setSort(rs.getInt("sort"));
            r.setFullCode(rs.getString("full_code"));
            r.setFullName(rs.getString("full_name"));
            r.setStatus(rs.getInt("status"));
            r.setCreateTime(rs.getTimestamp("create_time"));
            r.setUpdateTime(rs.getTimestamp("update_time"));
            return r;
        }, uniqueCodes.toArray());

        Map<String, ProxyRegion> resultMap = new java.util.HashMap<>();
        for (ProxyRegion r : list) {
            resultMap.put(r.getRegionCode(), r);
        }
        return resultMap;
    }
}

