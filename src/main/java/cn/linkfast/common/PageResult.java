package cn.linkfast.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果通用容器
 * @param <T> 具体的数据类型，通常是 VO 对象
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer total;      // 总记录数
    private List<T> list;       // 当前页的数据列表
    private Integer pageNum;    // 当前页码
    private Integer pageSize;   // 每页显示条数

    /**
     * 快速构建分页结果的构造方法
     */
    public PageResult(Integer total, List<T> list, Integer pageNum, Integer pageSize) {
        this.total = total;
        this.list = list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}