package cn.linkfast.vo;

import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;
import java.io.Serializable;

/**
 * 订单列表展示VO（前端需要的字段）
 */
@Data
public class ProxyOrderVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 订单类型 */
    private String orderType;

    /** 订单金额 */
    private BigDecimal amount;

    /** 创建时间 */
    private Date createTime;

    // 前端需要展示的实例总数
    private Integer instanceTotal;
}