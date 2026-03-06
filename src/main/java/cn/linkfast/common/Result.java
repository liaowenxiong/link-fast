package cn.linkfast.common;

import lombok.Getter;
import lombok.Setter;

/**
 *统一结果响应类
 * @param <T> 数据类型
 */
@Setter
@Getter
public class Result<T> {
    // Getter和Setter方法
    private Integer code;    //状态码
    private String message;  //消息
    private T data;         // 数据
    
    //私有构造函数
    private Result() {}
    
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // 成功响应
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }


    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    //错误响应
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    //判断是否成功
    public boolean isSuccess() {
        return code != null && code == 200;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}