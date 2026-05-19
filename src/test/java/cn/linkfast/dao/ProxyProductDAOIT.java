package cn.linkfast.dao;

import cn.linkfast.config.AppConfig;
import cn.linkfast.entity.ProxyProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代理产品DAO集成测试
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class})
public class ProxyProductDAOIT {

    @Autowired
    private ProxyProductDAO proxyProductDAO;

    @Test
    @DisplayName("根据productNo查询产品，验证retail_price是否为null")
    public void testSelectByProductNo() {
        String productNo = "ipv_gnmr4u5rx";

        // 1. 查询产品
        ProxyProduct product = proxyProductDAO.selectByProductNo(productNo);

        // 2. 断言产品存在
        assertNotNull(product, "产品应存在，productNo=" + productNo);

        // 3. 打印所有属性值
        System.out.println("========== 产品属性值 ==========");
        System.out.println("productNo:              " + product.getProductNo());
        System.out.println("productName:            " + product.getProductName());
        System.out.println("proxyType:              " + product.getProxyType());
        System.out.println("useType:                " + product.getUseType());
        System.out.println("protocol:               " + product.getProtocol());
        System.out.println("useLimit:               " + product.getUseLimit());
        System.out.println("sellLimit:              " + product.getSellLimit());
        System.out.println("areaCode:               " + product.getAreaCode());
        System.out.println("countryCode:            " + product.getCountryCode());
        System.out.println("stateCode:              " + product.getStateCode());
        System.out.println("cityCode:               " + product.getCityCode());
        System.out.println("detail:                 " + product.getDetail());
        System.out.println("costPrice:              " + product.getCostPrice());
        System.out.println("retailPrice:            " + product.getRetailPrice());
        System.out.println("inventory:              " + product.getInventory());
        System.out.println("ipType:                 " + product.getIpType());
        System.out.println("ispType:                " + product.getIspType());
        System.out.println("netType:                " + product.getNetType());
        System.out.println("duration:               " + product.getDuration());
        System.out.println("unit:                   " + product.getUnit());
        System.out.println("bandWidth:              " + product.getBandWidth());
        System.out.println("bandWidthPrice:         " + product.getBandWidthPrice());
        System.out.println("maxBandWidth:           " + product.getMaxBandWidth());
        System.out.println("flow:                   " + product.getFlow());
        System.out.println("cpu:                    " + product.getCpu());
        System.out.println("memory:                 " + product.getMemory());
        System.out.println("enable:                 " + product.getEnable());
        System.out.println("supplierCode:           " + product.getSupplierCode());
        System.out.println("ipCount:                " + product.getIpCount());
        System.out.println("ipDuration:             " + product.getIpDuration());
        System.out.println("assignIp:               " + product.getAssignIp());
        System.out.println("parentNo:               " + product.getParentNo());
        System.out.println("cidrStatus:             " + product.getCidrStatus());
        System.out.println("oneDay:                 " + product.getOneDay());
        System.out.println("cidrBlocks:             " + product.getCidrBlocks());
        System.out.println("offlineCidrBlocks:      " + product.getOfflineCidrBlocks());
        System.out.println("projectList:            " + product.getProjectList());
        System.out.println("proxyEverytimeChange:   " + product.getProxyEverytimeChange());
        System.out.println("proxyGlobalRandom:      " + product.getProxyGlobalRandom());
        System.out.println("apiDrawGlobalRandom:    " + product.getApiDrawGlobalRandom());
        System.out.println("ipWhiteList:            " + product.getIpWhiteList());
        System.out.println("pwdDrawProxyUser:       " + product.getPwdDrawProxyUser());
        System.out.println("proxyUserFlowLimit:     " + product.getProxyUserFlowLimit());
        System.out.println("flowUseLog:             " + product.getFlowUseLog());
        System.out.println("pwdDrawSessionRange:    " + product.getPwdDrawSessionRange());
        System.out.println("flowConversionBase:     " + product.getFlowConversionBase());
        System.out.println("productType:            " + product.getProductType());
        System.out.println("createTime:             " + product.getCreateTime());
        System.out.println("updateTime:             " + product.getUpdateTime());
        System.out.println("================================");

        // 4. 验证 retailPrice 不为 null，数据库实际值为 0.00
        assertNotNull(product.getRetailPrice(),
                "retailPrice 不应为 null");
        assertEquals(0, product.getRetailPrice().compareTo(java.math.BigDecimal.ZERO),
                "retailPrice 应为 0.00，实际值=" + product.getRetailPrice());

        System.out.println("✅ 验证通过：retailPrice = " + product.getRetailPrice());
    }
}
