package cn.linkfast.dao;

import cn.linkfast.entity.ProxyOrder;

import java.util.Map;

public interface ProxyOrderDAO {
    /**
     * 保存或更新订单及其关联的实例数据
     * @param order 包含实例列表的订单对象
     */
    Map<String, Object> saveOrder(ProxyOrder order);
}