package cn.linkfast.dto;

import lombok.Data;

/**
 * @author liaowenxiong
 * @version 1.0
 * @description TODO
 * @since 2026/3/6 14:17
 */
@Data
public class ProxyProductSearchCondition {
    private String countryCode;   // 必传
    private String cityCode;      // 必传

    // 以下为分页参数，如果不传则为 null
    private Integer limit;        // 对应 pageSize
    private Integer offset;       // 对应 (page-1) * pageSize
}
