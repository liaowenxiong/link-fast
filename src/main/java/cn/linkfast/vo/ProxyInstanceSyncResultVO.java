package cn.linkfast.vo;

import lombok.Data;

/**
 * 代理实例同步结果VO
 *
 * @author liaowenxiong
 * @version 1.0
 * @since 2026/5/17 09:29
 */
@Data
public class ProxyInstanceSyncResultVO {
    /** 预期同步数（第三方返回的实例数量） */
    private Integer expectedCount;
    /** 数据变更数（数据库中实际数据发生变化的实例数，不含数据无变化的） */
    private Integer actualCount;
}
