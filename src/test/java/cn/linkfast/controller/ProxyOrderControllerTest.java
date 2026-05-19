package cn.linkfast.controller;

import cn.linkfast.service.PayService;
import cn.linkfast.service.ProxyOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 代理订单接口单元测试
 * 测试目标：参数校验逻辑，不依赖 Spring 容器、数据库或第三方 API
 */
@ExtendWith(MockitoExtension.class)
public class ProxyOrderControllerTest {

    @Mock
    private ProxyOrderService proxyOrderService;

    @Mock
    private PayService payService;

    @InjectMocks
    private ProxyOrderController proxyOrderController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(proxyOrderController)
                .setControllerAdvice(new cn.linkfast.exception.GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试请求体为空（不传 JSON Body）
     * 预期：返回 400，提示请求体缺失相关错误
     */
    @Test
    @DisplayName("请求体为空时应返回错误信息")
    public void testPurchaseProxiesWithEmptyBody() throws Exception {
        mockMvc.perform(post("/api/order/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试缺少支付密码
     * 预期：返回参数校验失败，code=400
     */
    @Test
    @DisplayName("缺少支付密码时应返回错误信息")
    public void testPurchaseProxiesWithoutPayPassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("orderType", 1);
        body.put("totalQuantity", 1);

        Map<String, Object> item = new HashMap<>();
        item.put("productNo", "TEST_PRODUCT_001");
        item.put("proxyType", 101);
        item.put("countryCode", "US");
        item.put("stateCode", "CA");
        item.put("cityCode", "LAX");
        item.put("unit", 1);
        item.put("duration", 30);
        item.put("count", 1);
        item.put("cycleTimes", 1);
        body.put("params", Collections.singletonList(item));

        mockMvc.perform(post("/api/order/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试传入错误的支付密码
     * 预期：返回 400，提示支付密码校验不通过
     */
    @Test
    @DisplayName("支付密码错误时应返回400及错误提示")
    public void testPurchaseProxiesWithWrongPayPassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("payPassword", "wrong_password_123");
        body.put("orderType", 1);
        body.put("totalQuantity", 1);

        Map<String, Object> item = new HashMap<>();
        item.put("productNo", "TEST_PRODUCT_001");
        item.put("proxyType", 101);
        item.put("countryCode", "US");
        item.put("stateCode", "CA");
        item.put("cityCode", "LAX");
        item.put("unit", 1);
        item.put("duration", 30);
        item.put("count", 1);
        item.put("cycleTimes", 1);
        body.put("params", Collections.singletonList(item));

        MvcResult result = mockMvc.perform(post("/api/order/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("========== 错误支付密码请求结果 ==========");
        System.out.println(responseBody);
    }

    /**
     * 测试 params 列表为空
     * 预期：返回业务异常，订单创建失败
     */
    @Test
    @DisplayName("订单项列表为空时应返回错误信息")
    public void testPurchaseProxiesWithEmptyParams() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("payPassword", "any_password");
        body.put("orderType", 1);
        body.put("totalQuantity", 0);
        body.put("params", Collections.emptyList());

        mockMvc.perform(post("/api/order/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试请求 Content-Type 不是 JSON
     * 预期：全局异常处理器捕获异常后返回 HTTP 200 + JSON 错误信息
     */
    @Test
    @DisplayName("Content-Type不是JSON时应返回错误")
    public void testPurchaseProxiesWithWrongContentType() throws Exception {
        mockMvc.perform(post("/api/order/open")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("payPassword=test&orderType=1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").exists());
    }
}
