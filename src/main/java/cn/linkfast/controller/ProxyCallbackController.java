package cn.linkfast.controller;

import cn.linkfast.common.Result;
import cn.linkfast.dto.ProxyOrderUpdateResultDTO;
import cn.linkfast.service.ProxyInstanceService;
import cn.linkfast.service.ProxyOrderService;
import cn.linkfast.service.ProxyProductService;
import cn.linkfast.vo.ProxyInstanceSyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 第三方回调统一入口
 */
@Slf4j
@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class ProxyCallbackController {

    private final ProxyProductService proxyProductService;
    private final ProxyOrderService proxyOrderService;
    private final ProxyInstanceService proxyInstanceService;

    /**
     * 第三方通知回调地址
     *
     * @param type 变更类型 (例如: "product", "order", "instance")
     * @param no   变更编号 (产品编号/订单编号/实例编号)
     * @param op   操作类型 (例如: "update", "add")，实例回调不携带此参数
     * @return 成功响应给第三方
     */
    @GetMapping("/notify")
    public Result<Void> handleNotify(@RequestParam("type") String type, @RequestParam("no") String no,
                                     @RequestParam(value = "op", required = false) String op) {

        log.info(">>> 收到第三方回调通知：type={}, no={}, op={}", type, no, op);

        // 逻辑判断：仅处理产品相关的变动
        if ("product".equalsIgnoreCase(type)) {
            // 构造针对单个产品的同步参数
            Map<String, Object> params = new HashMap<>();
            params.put("productNo", no);
            //  proxyType 是必填项。
            // 既然是更新现有产品，我们传入全量类型确保能查询到
            params.put("proxyType", Arrays.asList(101, 102, 103, 104, 105, 201));

            // 执行同步任务
            try {

                proxyProductService.syncProxyProducts(params);
                log.info("<<< 产品 {} 同步成功", no);
            } catch (Exception e) {
                log.error("<<< 产品 {} 同步失败：{}", no, e.getMessage());
                return Result.error("产品 " + no + " 同步失败：" + e.getMessage());
            }
        } else if ("order".equalsIgnoreCase(type)) {
            log.info("<<< 订单 {} 同步开始", no);
            Map<String, Object> params = new HashMap<>();
            params.put("orderNo", no);
            params.put("pageSize", 100);
            try {
                ProxyOrderUpdateResultDTO result = proxyOrderService.syncOrderDetails(params);
                log.info("<<< 订单 {} 同步成功，更新订单 {} 行，更新实例 {} 行",
                        no, result.getOrderUpdatedRows(), result.getInstanceUpdatedRows());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if ("instance".equalsIgnoreCase(type)) {
            // 实例信息变动回调处理
            try {
                ProxyInstanceSyncResultVO syncResult = proxyInstanceService.syncProxyInstance(Collections.singletonList(no));
                log.info("<<< 实例 {} 同步成功，预期更新 {} 条，实际更新 {} 条",
                        no, syncResult.getExpectedCount(), syncResult.getActualCount());
            } catch (Exception e) {
                log.error("<<< 实例 {} 同步失败：{}", no, e.getMessage());
                return Result.error("实例 " + no + " 同步失败：" + e.getMessage());
            }
        }

        // 返回给第三方，告知已收到通知
        // 只要逻辑处理没崩，一律返回 code 200 的 JSON 对象
        return Result.success(null);
    }
}