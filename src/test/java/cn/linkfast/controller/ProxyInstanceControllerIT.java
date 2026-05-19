package cn.linkfast.controller;

import cn.linkfast.config.AppConfig;
import cn.linkfast.config.WebMvcConfig;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 代理实例同步接口集成测试
 * 测试目标：真实调用第三方API获取实例数据并同步到本地数据库
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class, WebMvcConfig.class})
@WebAppConfiguration
public class ProxyInstanceControllerIT {

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
     * 测试同步代理实例：使用真实实例编号调用第三方API
     * 测试目标：
     * 1. 接口返回 HTTP 200
     * 2. 响应体 code=200 时，校验同步成功：Result.message 包含描述，data 包含 expectedCount 和 actualCount
     * 3. 响应体 code≠200 时，校验同步失败：Result.message 包含“同步失败”
     */
    @Test
    @DisplayName("同步代理实例-真实调用第三方API同步实例数据")
    public void testSyncProxyInstance() throws Exception {
        String requestBody = "[\"c_gjhppprvsvc9j52\"]";

        MvcResult result = mockMvc.perform(post("/api/instance/sync").contentType(MediaType.APPLICATION_JSON).content(requestBody)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.code").isNumber()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        int code = responseJson.path("code").asInt(-1);
        String resultMessage = responseJson.path("message").asText("");

        if (code == 200) {
            // 同步成功：第三方返回了实例数据
            System.out.println("========== ✅ 同步成功 ==========");

            // 校验 Result.message 包含同步描述
            assertFalse(resultMessage.isEmpty(), "同步成功时 Result.message 不应为空");
            System.out.println("message: " + resultMessage);

            // 校验 data 不为 null
            JsonNode dataNode = responseJson.path("data");
            assertFalse(dataNode.isMissingNode() || dataNode.isNull(), "data 不应为 null");

            // 校验 ProxyInstanceSyncResultVO 字段
            int expectedCount = dataNode.path("expectedCount").asInt(-1);
            int actualCount = dataNode.path("actualCount").asInt(-1);
            assertNotEquals(-1, expectedCount, "expectedCount 字段不应缺失");
            assertNotEquals(-1, actualCount, "actualCount 字段不应缺失");
            assertTrue(expectedCount > 0, "同步成功时 expectedCount 应 > 0");
            assertTrue(actualCount >= 0, "actualCount 应 >= 0");

            System.out.println("expectedCount（预期同步数）: " + expectedCount);
            System.out.println("actualCount（数据变更数）:  " + actualCount);

            if (actualCount == expectedCount) {
                System.out.println("✅ 全部实例数据均有变化");
            } else {
                System.out.println("✅ 其中 " + (expectedCount - actualCount) + " 条数据无变化（与数据库已有数据相同）");
            }
        } else {
            // 同步失败：第三方没有返回实例数据或接口异常
            System.out.println("========== ❌ 同步失败 ==========");
            System.out.println("code: " + code);
            System.out.println("message: " + resultMessage);

            assertTrue(resultMessage.contains("同步失败"), "同步失败时 message 应包含'同步失败'，实际为: " + resultMessage);
        }

        // 无论业务成功失败，接口必须返回正常的业务 Result（不能因未捕获异常导致 HTTP 500）
        assertNotEquals(-1, code, "响应 code 字段不应缺失");
    }
}
