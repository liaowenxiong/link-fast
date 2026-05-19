package cn.linkfast.controller;

import cn.linkfast.config.AppConfig;
import cn.linkfast.config.WebMvcConfig;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支付密码校验接口集成测试
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class, WebMvcConfig.class})
@WebAppConfiguration
public class PayControllerIT {

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
     * 测试错误的支付密码
     * 预期：返回 200，code=200，data.passed=false，data.message="支付密码错误"
     */
    @Test
    @DisplayName("错误支付密码应返回校验不通过")
    public void testVerifyWithWrongPassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("payPassword", "000000");

        mockMvc.perform(post("/api/pay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.message").value("支付密码错误"));
    }

    /**
     * 测试正确的支付密码
     * 预期：返回 200，code=200，data.passed=true，data.message="支付密码正确"
     */
    @Test
    @DisplayName("正确支付密码应返回校验通过")
    public void testVerifyWithCorrectPassword() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("payPassword", "168888");

        mockMvc.perform(post("/api/pay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.message").value("支付密码正确"));
    }
}
