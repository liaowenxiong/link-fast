package cn.linkfast.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单查询条件（DAO层SQL拼接使用）
 */
@Data
public class ProxyOrderSearchCondition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分页偏移量 (pageNum-1)*pageSize
     */
    private Integer offset;

    /**
     * 每页条数，对应 pageSize
     */
    private Integer limit;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 订单号
     */
    private String orderNo;

}
