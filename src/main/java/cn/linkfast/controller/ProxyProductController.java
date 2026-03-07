package cn.linkfast.controller;

import cn.linkfast.common.PageResult;
import cn.linkfast.common.Result;
import cn.linkfast.dto.ProxyProductQueryDTO;
import cn.linkfast.service.ProxyProductService;
import cn.linkfast.vo.ProxyProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代理产品控制器
 */
@Slf4j
@RestController // 使用 @RestController 相当于 @Controller + @ResponseBody
@RequiredArgsConstructor
@RequestMapping("/api/proxy-product")
public class ProxyProductController {

    private final ProxyProductService productService;

    /**
     * 分页查询代理产品列表
     * Spring 会自动根据参数名将 URL 中的查询参数映射到 ProxyProductQueryDTO 的字段中
     * 例如：/api/proxy/list?countryCode=US&page=1&pageSize=10
     */
    @GetMapping("/list")
    public Result<PageResult<ProxyProductVO>> getProxyProductList(ProxyProductQueryDTO queryDto) {
        try {
            // 直接调用 Service 层获取分页包装后的 VO 数据
            PageResult<ProxyProductVO> pageResult = productService.getProxyProducts(queryDto);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取代理产品列表异常, queryDto: {}", queryDto, e);
            return Result.error("获取产品列表失败: " + e.getMessage());
        }
    }
}