package cn.linkfast.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理产品展示对象 (View Object)
 * 仅包含前端页面渲染所必需的字段，实现数据脱敏和格式化
 */
@Data
public class ProxyProductVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String productNo;      // 产品编号
    private String productName;    // 产品名称
    private Integer proxyType;     // 代理类型 (建议后期转为 String 的“中文描述”)
    private String protocol;       // 协议类型 (HTTP/SOCKS5)
    private String detail;         // 详情描述
    private BigDecimal costPrice;  // 成本价 (建议谨慎对外展示)
    private Integer inventory;     // 库存数量
}