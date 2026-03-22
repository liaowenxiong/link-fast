package cn.linkfast.dao.impl;

import cn.linkfast.dao.RegionDAO;
import cn.linkfast.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地域数据访问实现类
 * 对应表：region
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RegionDaoImpl implements RegionDAO {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int batchSaveOrUpdate(List<Region> regions) {
        log.info("批量插入/更新地域数据，数量：{}", regions == null ? 0 : regions.size());
        if (regions == null || regions.isEmpty()) {
            return 0;
        }

        // 注意：ON DUPLICATE KEY 依赖 region_code 的唯一索引（uk_region_code）
        String sql = "INSERT INTO region (" +
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
                Region r = regions.get(i);
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
                return regions.size();
            }
        });
        int successCount = 0;
        for (int x : results) {
            if (x >= 0) {
                successCount++;
            }
        }
        log.info("批量插入/更新地域数据完成，成功数量：{}", successCount);
        return successCount;
    }

    @Override
    public Map<String, Long> selectIdMapByRegionCodes(List<String> regionCodes) {
        if (regionCodes == null || regionCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(regionCodes.size(), "?"));
        String sql = "SELECT id, region_code FROM region WHERE region_code IN (" + placeholders + ")";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, regionCodes.toArray());
        return rows.stream().collect(Collectors.toMap(
                m -> (String) m.get("region_code"),
                m -> ((Number) m.get("id")).longValue(),
                (a, b) -> a
        ));
    }
}

