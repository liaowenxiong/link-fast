package cn.linkfast.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 地域树形 DTO
 */
@Data
public class AreaDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地域代码
     */
    private String code;

    /**
     * 地域英文名称
     */
    private String name;

    /**
     * 地域中文名称
     */
    private String cname;

    /**
     * 下级地域列表
     */
    private List<AreaDTO> children;
}

