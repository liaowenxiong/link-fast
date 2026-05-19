package cn.linkfast.dao;

import cn.linkfast.entity.ProxyRegion;

import java.util.List;
import java.util.Map;

/**
 * 地域数据访问接口
 */
public interface ProxyRegionDAO {

    /**
     * 批量插入/更新地域信息（region_code 唯一时可实现 upsert）
     *
     * @param proxyRegions 地域集合
     * @return 处理数量（批次数组中 r>=0 认为成功）
     */
    int batchSaveOrUpdate(List<ProxyRegion> proxyRegions);

    /**
     * 按 region_code 查询 region.id
     *
     * @param regionCodes region_code 列表
     * @return key=region_code, value=id
     */
    Map<String, Long> selectIdMapByRegionCodes(List<String> regionCodes);

    /**
     * 按 region_code 查询单个地域信息
     *
     * @param regionCode 地域编码
     * @return ProxyRegion 对象，不存在时返回 null
     */
    ProxyRegion selectByRegionCode(String regionCode);

    /**
     * 按 region_code 批量查询地域信息（用于消除 N+1 查询）
     *
     * @param regionCodes 地域编码集合
     * @return key=region_code, value=ProxyRegion
     */
    Map<String, ProxyRegion> selectByRegionCodes(List<String> regionCodes);
}


