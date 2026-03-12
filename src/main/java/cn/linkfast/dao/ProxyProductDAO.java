package cn.linkfast.dao;

import cn.linkfast.entity.ProxyProduct;
import cn.linkfast.dto.ProxyProductSearchCondition;

import java.util.List;

public interface ProxyProductDAO {
    /**
     * 批量保存或更新产品信息
     */
    int batchSaveOrUpdate(List<ProxyProduct> products);

    List<ProxyProduct> findProxyProductList(ProxyProductSearchCondition condition);

    int countProxyProduct(ProxyProductSearchCondition condition);
}