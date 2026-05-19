package cn.linkfast.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 代理实例列表展示VO（前端需要的字段）
 */
@Data
public class ProxyInstanceVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 实例编号（唯一标识）
    private String instanceNo;
    // ip地址
    private String ip;
    // 端口号
    private Integer port;
    // 洲的编号
    private String regionId;
    // 国家编码
    private String countryCode;
    // 城市编码
    private String cityCode;
    // 地域全称
    private String fullRegionName;
    private Integer status;
    private String username;
    private String pwd;
    private Integer renew;
    private String orderNo;
    private String productNo;
    // 购买时的周期单位
    private Integer unit;
    // 购买时的周期时长
    private Integer duration;
    private Long userExpired;
    private String remark;
    private Date createTime;
}
