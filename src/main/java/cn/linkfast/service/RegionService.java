package cn.linkfast.service;

import cn.linkfast.vo.AreaDTO;

import java.util.List;

/**
 * 地域同步服务
 */
public interface RegionService {

    List<AreaDTO> queryRegionTree(List<String> codes);

    /**
     * 从第三方“获取地域信息接口”同步地域树到本地数据库 `region` 表。
     *
     * @param codes 地域代码列表，为 null/空 则同步全部
     * @return 扁平化后的地域节点数量（即写入/更新的行数）
     */
    int syncRegionTreeToDb(List<String> codes) throws Exception;
}

