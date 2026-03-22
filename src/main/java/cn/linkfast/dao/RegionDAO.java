package cn.linkfast.dao;

import cn.linkfast.entity.Region;

import java.util.List;
import java.util.Map;

/**
 * 地域数据访问接口
 */
public interface RegionDAO {

    /**
     * 批量插入/更新地域信息（region_code 唯一时可实现 upsert）
     *
     * @param regions 地域集合
     * @return 处理数量（批次数组中 r>=0 认为成功）
     */
    int batchSaveOrUpdate(List<Region> regions);

    /**
     * 按 region_code 查询 region.id
     *
     * @param regionCodes region_code 列表
     * @return key=region_code, value=id
     */
    Map<String, Long> selectIdMapByRegionCodes(List<String> regionCodes);
}

