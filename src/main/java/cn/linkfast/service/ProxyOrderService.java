package cn.linkfast.service;

import java.util.Map;

public interface ProxyOrderService {
    /**
     * 同步订单完整信息并保存
     */
    int syncOrderDetails(Map<String, Object> params) throws Exception;

}