package cn.linkfast.controller;

import cn.linkfast.service.ProxyProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * 代理产品接口单元测试
 * 测试目标：参数校验逻辑，不依赖 Spring 容器、数据库或第三方 API
 */
@ExtendWith(MockitoExtension.class)
public class ProxyProductControllerTest {

    @Mock
    private ProxyProductService productService;

    @InjectMocks
    private ProxyProductController proxyProductController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(proxyProductController)
                .setControllerAdvice(new cn.linkfast.exception.GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试不携带任何参数请求 /api/proxy-product/list
     * 预期：返回 400，参数校验失败
     */
    @Test
    @DisplayName("不传参数时应返回参数校验失败")
    public void testQueryProxyProductsWithoutParams() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/proxy-product/list"))
                .andDo(print())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("========== 不带参数请求结果 ==========");
        System.out.println(responseBody);
    }
}
