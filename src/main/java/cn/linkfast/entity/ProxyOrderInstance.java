package cn.linkfast.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实例子表
 */
@Data
public class ProxyOrderInstance {
    private Long id;
    private String orderNo;       // 平台订单号
    private String instanceNo;    // 平台实例编号
    private Integer proxyType;    // 代理类型
    private String protocol;      // 协议类型
    private String ip;            // 代理地址
    private Integer port;         // 代理端口
    private String regionId;      // 区域地址
    private String countryCode;   // 国家代码
    private String cityCode;      // 城市代码
    private String useType;       // 使用方式
    private String username;      // 账户名或uuid
    private String pwd;           // 密码
    private Long userExpired;     // 到期时间（时间戳，秒）
    private BigDecimal flowTotal; // 总流量(MB)/IP数
    private BigDecimal flowBalance;// 剩余流量(MB)
    private Integer status;       // 实例状态
    private Integer renew;        // 是否自动续费（已废弃）
    private String bridges;       // 桥地址列表（JSON存储）
    private Date openAt;          // 实例开通时间
    private Date renewAt;         // 最后成功续费时间
    private Date releaseAt;       // 实例释放成功时间
    private String productNo;     // 产品编号
    private String extendIp;      // 扩展地址
    private String projectId;     // 项目code
    private Date createTime;
    private Date updateTime;
}