package cn.linkfast.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyProduct {

    private String productNo;
    private String productName;
    private Integer proxyType;
    private String useType = "1";
    private String protocol = "1";
    private Integer useLimit = 3;
    private Integer sellLimit = 3;
    private String areaCode;
    private String countryCode;
    private String stateCode = "000";
    private String cityCode = "000";
    private String detail;

    @JsonProperty("price")
    private BigDecimal costPrice;
    private BigDecimal retailPrice;

    private Integer inventory;
    private Integer ipType = 1;
    private Integer ispType = 0;
    private Integer netType = 0;
    private Integer duration;
    private Integer unit;
    private Integer bandWidth;
    private BigDecimal bandWidthPrice;
    private Integer maxBandWidth;
    private Integer flow;
    private Integer cpu;
    private BigDecimal memory;
    private Integer enable = 1;
    private String supplierCode;
    private Integer ipCount;
    private Integer ipDuration;
    private Integer assignIp = -1;
    private String parentNo;
    private Integer cidrStatus = -1;
    private Integer oneDay;
    private String cidrBlocks;
    private String offlineCidrBlocks;
    private Integer proxyEverytimeChange = -1;
    private Integer proxyGlobalRandom = -1;
    private Integer apiDrawGlobalRandom = -1;
    private Integer ipWhiteList = -1;
    private Integer pwdDrawProxyUser = -1;
    private Integer proxyUserFlowLimit = -1;
    private Integer flowUseLog = -1;
    private String pwdDrawSessionRange;
    private Integer flowConversionBase = 0;
    private String projectList;
    private Integer productType = 1;
    private Date createTime;
    private Date updateTime;

    // --- 以下为完整 Getter 和 Setter，严禁省略 ---

}