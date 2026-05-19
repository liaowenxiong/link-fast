package cn.linkfast.service;

import cn.linkfast.common.PageResult;
import cn.linkfast.dto.ProxyInstanceQueryDTO;
import cn.linkfast.entity.ProxyInstance;
import cn.linkfast.vo.ProxyInstanceSyncResultVO;
import cn.linkfast.vo.ProxyInstanceVO;

import java.util.List;

/**
 * 代理实例服务接口
 */
public interface ProxyInstanceService {

    /**
     * 从第三方同步代理实例信息到我方数据库
     *
     * @param instanceNos 供应商实例编号
     * @return 同步结果（预期更新数 + 实际更新数）
     */
    ProxyInstanceSyncResultVO syncProxyInstance(List<String> instanceNos) throws Exception;

    /**
     * 分页查询本地代理实例列表
     *
     * @param queryDto 查询入参
     * @return 分页VO结果
     */
    PageResult<ProxyInstanceVO> queryProxyInstances(ProxyInstanceQueryDTO queryDto);

    /**
     * 更新代理实例备注
     *
     * @param instanceNo 平台实例编号
     * @param remark     备注内容
     */
    void updateRemark(String instanceNo, String remark);

    /**
     * 变更代理实例自动续费状态
     *
     * @param instanceNo 平台实例编号
     * @param renew      自动续费状态（0=关闭，1=开启）
     */
    void updateRenewStatus(String instanceNo, Integer renew);

    /**
     * 获取已开启自动续费且即将到期的代理实例列表
     *
     * @param days 距离到期天数
     * @return 即将到期的代理实例列表
     */
    List<ProxyInstance> getAutoRenewExpiringInstances(int days);
}
