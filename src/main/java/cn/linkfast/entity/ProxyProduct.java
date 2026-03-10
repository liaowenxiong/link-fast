package cn.linkfast.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
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
    private Integer proxyEverytimeChange = -1;
    private Integer proxyGlobalRandom = -1;
    private Integer apiDrawGlobalRandom = -1;
    private Integer ipWhiteList = -1;
    private Integer pwdDrawProxyUser = -1;
    private Integer proxyUserFlowLimit = -1;
    private Integer flowUseLog = -1;
    private String pwdDrawSessionRange;
    private Integer flowConversionBase = 0;
    private Integer productType = 1;
    private Date createTime;
    private Date updateTime;

    // 默认初始化为空集合，配合 DAO 层逻辑
    private List<BlockItem> cidrBlocks = new ArrayList<>();
    private List<OfflineBlockItem> offlineCidrBlocks = new ArrayList<>();
    private List<ProjectItem> projectList = new ArrayList<>();

    // 内嵌类，代表 projectList 数组的每个元素
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectItem {
        private String code;
        private Integer inventory;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockItem {
        private String cidr;
        private Integer count;
        private String asn;
        private String isp;
        // 这里的 projectList 也必须是 List<ProjectItem>，不能是 List<String>
        private List<ProjectItem> projectList = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfflineBlockItem {
        private String cidr;
        private String offlineTime;
    }
}