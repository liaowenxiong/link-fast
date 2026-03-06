package cn.linkfast.controller;

import cn.linkfast.common.Result;
import cn.linkfast.entity.ProxyProduct;
import cn.linkfast.service.ProxyProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// cn.linkfast.controller.ProxyProductController.java
@Controller
@RequiredArgsConstructor

@RequestMapping("/api/proxy")
public class ProxyProductController {

    private final ProxyProductService productService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public Result<List<ProxyProduct>> getProductList(
            @RequestParam(required = false) Integer proxyType,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String cityCode) {

        try {
            Map<String, Object> query = new HashMap<>();
            query.put("proxyType", proxyType);
            query.put("countryCode", countryCode);
            query.put("cityCode", cityCode);

            // 调用 Service 获取（内部已包含同步逻辑）
            List<ProxyProduct> list = productService.syncAndGetProducts(query);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取产品列表失败: " + e.getMessage());
        }
    }
}
