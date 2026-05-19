package cn.linkfast.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代理实例查询条件（DAO层SQL拼接使用）
 */
@Data
public class ProxyInstanceSearchCondition implements Serializable {
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
     * 代理类型
     */
    private Integer[] proxyType;

    /**
     * 实例状态
     */
    private Integer status;

    /**
     * 国家代码
     */
    private String countryCode;

    /**
     * 城市代码
     */
    private String cityCode;


    /**
     * IP地址（模糊查询）
     */
    private String ip;

    /**
     * 代理编号（精确查询）
     */
    private String instanceNo;
}

