package cn.linkfast.controller;

import cn.linkfast.common.Result;
import cn.linkfast.config.AppConfig;
import cn.linkfast.config.WebMvcConfig;
import cn.linkfast.vo.ProxyPurchaseResultVO;
import cn.linkfast.vo.ProxyRenewResultVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 代理订单接口集成测试
 * 仅包含需要 Spring 容器 + 真实数据库 + 第三方 API 的测试用例
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class, WebMvcConfig.class})
@WebAppConfiguration
public class ProxyOrderControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 测试续费代理实例（真实调用第三方API）
     * 模拟数据：instanceNo=c_gjhfpkxyqjx2n76, unit=1, duration=30, cycleTimes=2
     * 测试目标：
     *   1. 代理续费订单（proxy_order）是否成功保存
     *   2. 代理续费订单明细（proxy_renew_order_item）是否成功保存
     *   3. 是否成功请求第三方续费接口并得到响应数据（orderNo、amount）
     *   4. 是否成功回写订单表和续费订单明细表（order_no、amount 字段）
     *   5. 控制器返回的结果数据是否正确（code=200、appOrderNo、orderNo、amount、status 均不为空）
     */
    @Test
    @DisplayName("续费代理实例-全链路集成测试")
    public void testRenewProxiesFullFlow() throws Exception {
        // 构造请求体：ProxyRenewDTO（含 payPassword 和 items 列表，与控制器 @RequestBody 参数类型一致）
        Map<String, Object> item = new HashMap<>();
        item.put("instanceNo", "c_gjhfpkxyqjx2n76");
        item.put("unit", 1);
        item.put("duration", 30);
        item.put("cycleTimes", 2);

        Map<String, Object> body = new HashMap<>();
        body.put("payPassword", "168888");
        body.put("items", Collections.singletonList(item));

        String requestBody = objectMapper.writeValueAsString(body);
        System.out.println("========== 续费代理请求体 ==========");
        System.out.println(requestBody);

        // 发起请求
        MvcResult mvcResult = mockMvc.perform(post("/api/order/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        System.out.println("========== 续费代理响应结果 ==========");
        System.out.println(responseBody);

        // 解析为强类型 Result<ProxyRenewResultVO>
        Result<ProxyRenewResultVO> result = objectMapper.readValue(
                responseBody, new TypeReference<Result<ProxyRenewResultVO>>() {});

        // ===== 断言 1：接口业务返回 code=200（续费全链路成功） =====
        assertEquals(200, result.getCode(),
                "续费全链路应成功，实际 code=" + result.getCode() + "，message=" + result.getMessage());

        ProxyRenewResultVO data = result.getData();

        // ===== 断言 2：data 不为空（订单已落库，说明 proxy_order 保存成功） =====
        assertNotNull(data, "code=200 时 data 不应为空，说明续费订单主表未正确保存");

        // ===== 断言 3：appOrderNo 不为空（本地渠道商订单号已生成并保存，说明 proxy_order 入库成功） =====
        assertNotNull(data.getAppOrderNo(), "appOrderNo 不应为空，说明续费订单主表未正确保存");
        assertFalse(data.getAppOrderNo().isBlank(), "appOrderNo 不应为空字符串");
        System.out.println("[OK] 续费订单主表已保存，appOrderNo: " + data.getAppOrderNo());

        // ===== 断言 4：orderNo 不为空（第三方API已响应并回写，说明 proxy_order 和 proxy_renew_order_item 均已回写 order_no） =====
        assertNotNull(data.getOrderNo(), "orderNo 不应为空，说明第三方续费接口未成功响应或回写失败");
        assertFalse(data.getOrderNo().isBlank(), "orderNo 不应为空字符串");
        System.out.println("[OK] 第三方续费接口已成功响应，orderNo: " + data.getOrderNo());

        // ===== 断言 5：amount 不为空且大于 0（第三方返回了扣费金额，说明回写成功） =====
        assertNotNull(data.getAmount(), "amount 不应为空，说明第三方未返回金额或回写失败");
        assertTrue(data.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0,
                "amount 应大于 0，实际值: " + data.getAmount());
        System.out.println("[OK] 订单金额已回写，amount: " + data.getAmount());

        // ===== 断言 6：status=1（控制器返回字段正确，与 Service 层设置一致） =====
        assertNotNull(data.getStatus(), "status 不应为空");
        assertEquals(1, data.getStatus(), "续费订单状态应为 1（待处理），实际: " + data.getStatus());
        System.out.println("[OK] 控制器返回数据正确，status: " + data.getStatus());

        System.out.println("========== 续费代理全链路测试通过 ==========");
        System.out.println("appOrderNo : " + data.getAppOrderNo());
        System.out.println("orderNo    : " + data.getOrderNo());
        System.out.println("amount     : " + data.getAmount());
        System.out.println("status     : " + data.getStatus());
    }

    /**
     * 测试使用完整合法参数创建代理订单
     * 说明：这是集成测试，依赖真实数据库和第三方API
     * 成功时返回 ProxyPurchaseResultVO（含 appOrderNo、orderNo、status、amount）
     * 失败时由全局异常处理器返回 Result（含 code、message）
     */
    @Test
    @DisplayName("完整合法参数创建代理订单-集成测试")
    public void testPurchaseProxiesWithValidData() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("payPassword", "168888");
        body.put("orderType", 1);
        body.put("totalQuantity", 1);

        Map<String, Object> item = new HashMap<>();
        item.put("productNo", "mb_gmhd5exsa");
        item.put("proxyType", 103);
        item.put("countryCode", "MYS");
        item.put("stateCode", "MYS000");
        item.put("cityCode", "MYS000000");
        item.put("unit", 1);
        item.put("duration", 30);
        item.put("count", 1);
        item.put("cycleTimes", 1);
        body.put("params", Collections.singletonList(item));

        MvcResult mvcResult = mockMvc.perform(post("/api/order/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        System.out.println("========== 创建代理订单响应结果，接口方法实际返回的数据 ==========");
        System.out.println(responseBody);

        // 解析响应 JSON
        Result<ProxyPurchaseResultVO> result = objectMapper.readValue(responseBody,
                new TypeReference<Result<ProxyPurchaseResultVO>>() {});

        if (result.getCode() == 200) {
            ProxyPurchaseResultVO data = result.getData();
            System.out.println("✅ 订单创建成功！");
            System.out.println("appOrderNo: " + data.getAppOrderNo());
            System.out.println("orderNo: " + data.getOrderNo());
            assertNotNull(data.getAppOrderNo());
            assertNotNull(data.getOrderNo());
        } else {
            System.out.println("❌ 订单创建失败！");
            System.out.println("code: " + result.getCode());
            System.out.println("message: " + result.getMessage());
            fail("订单创建失败: code=" + result.getCode() + ", message=" + result.getMessage());
        }
    }
}
