package cn.linkfast.controller;

import cn.linkfast.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liaowenxiong
 * @version 1.0
 * @description TODO
 * @since 2026/3/9 11:24
 */
@RestController
public class RootController {
    @GetMapping("/")
    public Result<String> index() {
        return Result.success("Link Fast API Service v1.0.0 is Online.");
    }
}
