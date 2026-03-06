package cn.linkfast.service;

import cn.linkfast.common.PageResult;
import cn.linkfast.dto.ProxyProductQueryDTO; // 对应修改为 ProxyProductQueryDTO
import cn.linkfast.vo.ProxyProductVO;      // 对应修改为 ProxyProductVO

import java.util.Map;

/**
 * 代理产品服务接口
 * 采用 DTO 入参和 VO 出参，确保接口协议与数据库模型解耦
 */
public interface ProxyProductService {

    /**
     * 从第三方同步代理产品数据到我方数据库
     */
    int syncProxyProducts(Map<String, Object> params) throws Exception;

    /**
     * 分页查询本地代理产品列表
     */
    PageResult<ProxyProductVO> getProxyProducts(ProxyProductQueryDTO queryDto) throws Exception;
}