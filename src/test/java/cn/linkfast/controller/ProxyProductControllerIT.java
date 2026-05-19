package cn.linkfast.controller;

import cn.linkfast.common.PageResult;
import cn.linkfast.common.Result;
import cn.linkfast.config.AppConfig;
import cn.linkfast.config.WebMvcConfig;
import cn.linkfast.vo.ProxyProductVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 代理产品接口集成测试
 * 仅包含需要 Spring 容器 + 真实数据库的测试用例
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class, WebMvcConfig.class})
@WebAppConfiguration
public class ProxyProductControllerIT {

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
     * 测试获取代理产品列表（真实请求）
     * 入参：pageNum=1, pageSize=50
     * 测试目标：是否可以成功获取到产品列表数据（code=200，list 不为空，total > 0）
     */
    @Test
    @DisplayName("获取代理产品列表-分页集成测试")
    public void testQueryProxyProductsWithValidParams() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/proxy-product/list")
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        System.out.println("========== 代理产品列表响应结果 ==========");
        System.out.println(responseBody);

        // 解析为强类型
        Result<PageResult<ProxyProductVO>> result = objectMapper.readValue(
                responseBody, new TypeReference<Result<PageResult<ProxyProductVO>>>() {});

        assertEquals(200, result.getCode(),
                "接口应返回 code=200，实际 code=" + result.getCode() + "，message=" + result.getMessage());

        PageResult<ProxyProductVO> data = result.getData();
        assertNotNull(data, "data 不应为空");
        assertNotNull(data.getList(), "产品列表不应为空");
        assertFalse(data.getList().isEmpty(), "产品列表不应为空集合，请确认数据库中存在产品数据");
        assertTrue(data.getTotal() > 0, "total 应大于 0，实际值: " + data.getTotal());

        System.out.println("[OK] 成功获取产品列表，total=" + data.getTotal() + "，本页条数=" + data.getList().size());
        data.getList().forEach(vo -> System.out.println("  产品: " + vo));
    }
}
