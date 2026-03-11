package cn.linkfast.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author liaowenxiong
 * @version 1.0
 * @description TODO
 * @since 2026/3/10 20:34
 */
@Data
public class ProxyOrder {
    private String orderNo;       // 平台订单编号
    private String appOrderNo;    // 渠道商订单号
    private Long userId;          // 买家ID
    private Integer orderType;    // 订单类型: 1=新建, 2=续费, 3=释放
    private Integer status;       // 状态: 1=待处理, 2=处理中, 3=处理成功, 4=处理失败, 5=部分完成
    private Integer productCount; // 购买数量
    private BigDecimal amount;    // 总价
    private Integer hasRefund;    // 是否存在退费: 0=无, 1=存在
    private Long instanceTotal;   // 订单对应实例总数量
    private Date createTime;
    private Date updateTime;

    // 封装订单下的所有实例明细
    private List<ProxyOrderInstance> instances;
}