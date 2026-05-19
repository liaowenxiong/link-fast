package cn.linkfast.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代理实例查询入参DTO
 */
@Data
public class ProxyInstanceQueryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 代理类型（可选，支持多个值）
     */
    private Integer[] proxyType;

    /**
     * 实例状态（可选）
     */
    private Integer status;

    /**
     * 页码（必传，大于0）
     */
    @NotNull(message = "页码pageNum不能为空")
    @Min(value = 1, message = "页码pageNum必须大于0")
    private Integer pageNum;

    /**
     * 每页条数（必传，大于0且不超过100）
     */
    @NotNull(message = "每页条数pageSize不能为空")
    @Min(value = 1, message = "每页条数pageSize必须大于0")
    @Max(value = 100, message = "每页条数pageSize不能超过100")
    private Integer pageSize;

    /**
     * 国家代码（可选）
     */
    private String countryCode;

    /**
     * 城市代码（可选）
     */
    private String cityCode;

    /**
     * IP地址（可选，模糊查询）
     */
    private String ip;

    /**
     * 代理编号（可选，精确查询）
     */
    private String instanceNo;
}

