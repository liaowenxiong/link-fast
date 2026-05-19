package cn.linkfast.controller;

import cn.linkfast.common.PageResult;
import cn.linkfast.common.Result;
import cn.linkfast.dto.ProxyInstanceQueryDTO;
import cn.linkfast.dto.ProxyInstanceRemarkDTO;
import cn.linkfast.dto.ProxyRenewStatusDTO;
import cn.linkfast.service.ProxyInstanceService;
import cn.linkfast.vo.ProxyInstanceSyncResultVO;
import cn.linkfast.vo.ProxyInstanceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理实例接口控制器
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/instance")
public class ProxyInstanceController {

    private final ProxyInstanceService proxyInstanceService;

    /**
     * 获取代理实例列表（分页）
     *
     * @param queryDto 查询入参（自动校验必传参数）
     * @return 分页实例VO列表
     */
    @GetMapping("/list")
    public Result<PageResult<ProxyInstanceVO>> queryProxyInstances(@Validated ProxyInstanceQueryDTO queryDto) {
        log.info("Controller层开始查询代理实例列表，查询条件：{}", queryDto);
        PageResult<ProxyInstanceVO> pageResult = proxyInstanceService.queryProxyInstances(queryDto);
        return Result.success(pageResult);
    }

    /**
     * 更新代理实例备注
     *
     * @param dto instanceNo（必传）+ remark
     * @return 操作结果
     */
    @PatchMapping("/remark")
    public Result<Void> updateRemark(@RequestBody @Validated ProxyInstanceRemarkDTO dto) {
        proxyInstanceService.updateRemark(dto.getInstanceNo(), dto.getRemark());
        return Result.success(null);
    }

    /**
     * 同步代理实例信息
     * 从第三方拉取最新实例数据并更新到本地数据库
     *
     * @param instanceNos 代理实例编号数组
     * @return 同步结果（预期更新数 + 实际更新数）
     */
    @PostMapping("/sync")
    public Result<ProxyInstanceSyncResultVO> syncProxyInstance(@RequestBody List<String> instanceNos) {
        log.info("Controller层开始同步代理实例，instanceNos：{}", instanceNos);
        try {
            ProxyInstanceSyncResultVO syncResult = proxyInstanceService.syncProxyInstance(instanceNos);
            if (syncResult.getExpectedCount() == 0) {
                return Result.error("第三方没有返回实例数据，同步失败~");
            }
            String message = String.format("第三方返回%d个实例数据，预期更新%d个实例，实际更新%d个实例~",
                    syncResult.getExpectedCount(), syncResult.getExpectedCount(), syncResult.getActualCount());
            return Result.success(message, syncResult);
        } catch (Exception e) {
            log.error("同步代理实例失败，instanceNos：{}，原因：{}", instanceNos, e.getMessage(), e);
            return Result.error("同步失败：" + e.getMessage());
        }
    }

    /**
     * 变更代理实例自动续费状态
     *
     * @param instanceNo  平台实例编号（路径参数）
     * @param dto 自动续费状态（0=关闭，1=开启）
     * @return 操作结果
     */
    @PatchMapping("/{instanceNo}")
    public Result<Void> updateRenewStatus(@PathVariable("instanceNo") String instanceNo,
                                         @RequestBody @Validated ProxyRenewStatusDTO dto) {
        proxyInstanceService.updateRenewStatus(instanceNo, dto.getRenew());
        return Result.success("设置成功", null);
    }

}

