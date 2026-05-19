package cn.linkfast.dao;

import cn.linkfast.dto.ProxyInstanceSearchCondition;
import cn.linkfast.entity.ProxyInstance;

import java.util.List;

/**
 * 代理实例数据访问接口
 */
public interface ProxyInstanceDAO {

    /**
     * 批量保存或更新实例信息（存在则更新，不存在则插入）
     *
     * @param instances 实例列表
     * @return 成功处理的条数
     */
    int batchUpdate(List<ProxyInstance> instances);

    /**
     * 根据查询条件分页查询代理实例列表
     *
     * @param condition 查询条件
     * @return 实例列表
     */
    List<ProxyInstance> selectListByCondition(ProxyInstanceSearchCondition condition);

    /**
     * 根据查询条件统计代理实例总数
     *
     * @param condition 查询条件
     * @return 总条数
     */
    int countByCondition(ProxyInstanceSearchCondition condition);

    /**
     * 根据实例编号更新备注
     *
     * @param instanceNo 平台实例编号
     * @param remark     备注内容
     * @return 影响行数
     */
    int updateRemarkByInstanceNo(String instanceNo, String remark);

    /**
     * 根据实例编号更新自动续费状态
     *
     * @param instanceNo 平台实例编号
     * @param renew      自动续费状态（0=关闭，1=开启）
     * @return 影响行数
     */
    int updateRenewByInstanceNo(String instanceNo, Integer renew);

    /**
     * 查询已开启自动续费且即将到期的代理实例
     *
     * @param days 距离到期天数
     * @return 即将到期的代理实例列表
     */
    List<ProxyInstance> selectAutoRenewExpiringInstances(int days);
}
