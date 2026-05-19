package cn.linkfast.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 代理续费订单明细表实体类
 * 对应表：proxy_renew_order_item
 *
 * @author liaowenxiong
 * @version 1.0
 * @since 2026/3/13
 */
@Data
public class ProxyRenewOrderItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 自增主键
    private Long id;

    // 代理续费订单的id
    private Long orderId;

    // 代理续费订单的平台订单号
    private String orderNo;

    // 代理续费订单的渠道商订单号
    private String appOrderNo;
    // 实例编号
    private String instanceNo;

    // 周期时长
    private Integer duration;
    // 周期单位
    private Integer unit;
    // 周期次数
    private Integer cycleTimes;
    // 续费金额
    private BigDecimal renewAmount;
    private Date createTime;
    private Date updateTime;
}