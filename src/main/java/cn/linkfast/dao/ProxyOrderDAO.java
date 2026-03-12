package cn.linkfast.dao;

import cn.linkfast.dto.OrderUpdateResultDTO;
import cn.linkfast.dto.ProxyOrderSearchCondition;
import cn.linkfast.entity.ProxyOrder;

import java.util.List;

public interface ProxyOrderDAO {
    /**
     * 保存或更新订单及其关联的实例数据
     *
     * @param order 包含实例列表的订单对象
     */
    OrderUpdateResultDTO saveOrder(ProxyOrder order);

    /**
     * 分页查询订单列表
     *
     * @param condition 查询条件
     * @return 订单实体列表
     */
    List<ProxyOrder> findProxyOrderList(ProxyOrderSearchCondition condition);

    /**
     * 查询订单总数（分页用）
     *
     * @param condition 查询条件
     * @return 总条数
     */
    int countProxyOrder(ProxyOrderSearchCondition condition);
}