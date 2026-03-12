package cn.linkfast.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代理产品查询参数传输对象 (Data Transfer Object)
 * 接收前端传来的所有筛选和分页参数
 */
@Data
public class ProxyProductQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String countryCode;
    private String cityCode;

    private Integer pageNum;
    private Integer pageSize;

}
