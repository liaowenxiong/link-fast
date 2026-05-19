package cn.linkfast.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 变更代理实例自动续费状态入参DTO
 */
@Data
public class ProxyRenewStatusDTO {

    /**
     * 自动续费状态（0=关闭，1=开启），必传
     */
    @NotNull(message = "自动续费状态renew不能为空")
    @Min(value = 0, message = "自动续费状态renew只允许0或1")
    @Max(value = 1, message = "自动续费状态renew只允许0或1")
    private Integer renew;
}
