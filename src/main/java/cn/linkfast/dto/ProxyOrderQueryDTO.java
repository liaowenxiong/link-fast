package cn.linkfast.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * TODO
 *
 * @author liaowenxiong
 * @version 1.0
 * @since 2026/3/12 14:18
 */
@Data
public class ProxyOrderQueryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码（必传）
     */
    @NotNull(message = "页码pageNum不能为空")
    private Integer pageNum;

    /**
     * 每页条数（必传）
     */
    @NotNull(message = "每页条数pageSize不能为空")
    private Integer pageSize;

    /**
     * 订单状态（必传）
     */
    @NotNull(message = "订单状态status不能为空")
    private Integer status;

    /**
     * 订单类型（非必传）
     */
    private String orderType;

    /**
     * 订单号（非必传）
     */
    private String orderNo;
}