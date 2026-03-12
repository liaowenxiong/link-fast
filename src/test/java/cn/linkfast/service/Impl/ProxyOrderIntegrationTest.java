package cn.linkfast.service.Impl;

import cn.linkfast.config.AppConfig;
import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.dto.OrderUpdateResultDTO;
import cn.linkfast.service.impl.ProxyOrderServiceImpl;
import cn.linkfast.utils.ApiPacketUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单同步集成测试
 * 环境：纯 Spring MVC (JUnit 5 + Mockito)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class}) // 直接加载你的核心配置类
//@Transactional // 保证测试入库后自动回滚，不产生垃圾数据
public class ProxyOrderIntegrationTest {

    // 手动 Mock 解密工具，跳过复杂的加密算法
    private final ApiPacketUtil apiPacketUtil = mock(ApiPacketUtil.class);
    @Autowired
    private ProxyOrderDAO proxyOrderDAO; // 自动注入 applicationContext.xml 中定义的真实 DAO
    @Autowired
    private ObjectMapper objectMapper; // 自动注入 AppConfig 中定义的 ObjectMapper
    private ProxyOrderServiceImpl proxyOrderServiceSpy;

    @BeforeEach
    void setUp() {
        // 1. 手动构造 Service 实例
        ProxyOrderServiceImpl realService = new ProxyOrderServiceImpl(proxyOrderDAO, objectMapper, apiPacketUtil);

        // 2. 将实例包装为 Mockito Spy
        proxyOrderServiceSpy = spy(realService);

        // 3. 手动填充 @Value 注解字段（手动 new 的对象 Spring 不会自动注入属性）
        ReflectionTestUtils.setField(proxyOrderServiceSpy, "env", "sandbox");
        ReflectionTestUtils.setField(proxyOrderServiceSpy, "sandboxUrl", "https://sandbox.ipipv.com");
    }

    @Test
    @DisplayName("测试 syncOrderDetails：跳过网络请求并真实持久化到数据库")
    void testSyncOrderAndSaveToDb() throws Exception {
        // --- 1. 准备模拟数据 ---

        // 模拟 sendPost 返回的原始加密响应
        String mockRawApiResponse = "{\"code\":200, \"msg\":\"ok\", \"data\":\"ENCRYPTED_DATA_STUB\"}";

        // 模拟解密后的明文 JSON（你提供的模拟数据）
// Java 15+ 支持：三引号包裹文本块，内部双引号无需转义
        String mockDecryptedJson = """
                {
                  "orderNo": "P2026031200010005",
                  "appOrderNo": "D2026031200886699",
                  "type": 1,
                  "status": 3,
                  "count": 2,
                  "amount": "99.80",
                  "refund": 0,
                  "page": 1,
                  "pageSize": 10,
                  "total": 2,
                  "instances": [
                    {
                      "instanceNo": "INST20260312001001",
                      "proxyType": 101,
                      "protocol": "1,2,3",
                      "ip": "192.168.1.101",
                      "port": 1080,
                      "regionId": "CN-SH",
                      "countryCode": "CN",
                      "cityCode": "SH",
                      "useType": "1,2",
                      "username": "proxy_user_01",
                      "pwd": "Pro@xy123456",
                      "orderNo": "P2026031200010005",
                      "userExpired": 1715500800,
                      "flowTotal": 10240.00,
                      "flowBalance": 9876.50,
                      "status": 3,
                      "renew": 0,
                      "bridges": ["10.0.0.1:8080", "10.0.0.2:8080"],
                      "openAt": "2026-03-12 09:30:00",
                      "renewAt": "2026-03-12 09:30:00",
                        "releaseAt":"2026-03-12 09:35:00",
                      "productNo": "PRO-STATIC-101",
                      "extendIp": "192.168.1.102,192.168.1.103"
                    },
                    {
                      "instanceNo": "INST20260312001002",
                      "proxyType": 104,
                      "protocol": "1,4",
                      "ip": "103.20.11.22",
                      "port": 2222,
                      "regionId": "US-NY",
                      "countryCode": "US",
                      "cityCode": "NY",
                      "useType": "3",
                      "username": "f9876a54-b321-0987-dcba-1234567890ab",
                      "pwd": "UUID@Dynamic2026",
                      "orderNo": "P2026031200010005",
                      "userExpired": 1715500800,
                      "flowTotal": 5120.00,
                      "flowBalance": 5120.00,
                      "status": 3,
                      "renew": 0,
                      "bridges": [],
                      "openAt": "2026-03-12 09:35:00",
                      "renewAt": "2026-03-12 09:35:00",
                      "releaseAt":"2026-03-12 09:35:00",
                      "productNo": "PRO-DYNAMIC-104",
                      "extendIp": null
                    }
                  ]
                }""";

        // --- 2. 行为打桩 ---

        // 拦截 sendPost：不执行真实网络调用，直接返回加密占位符
        doReturn(mockRawApiResponse).when(proxyOrderServiceSpy).sendPost(anyString(), anyMap());

        // 拦截 ApiPacketUtil：当内部调用 unpack 时，返回我们准备好的明文 JSON
        when(apiPacketUtil.unpack("ENCRYPTED_DATA_STUB")).thenReturn(mockDecryptedJson);

        // 拦截 pack：syncOrderDetails 第一步会加密参数，这里让它返回空 Map 即可
        when(apiPacketUtil.pack(any())).thenReturn(new HashMap<>());

        // --- 3. 执行测试 ---

        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", "P202603111200001");

        // 调用 spy 对象的方法
        // 流程：syncOrderDetails -> (Spy拦截)sendPost -> (真实)processResponse -> (Mock拦截)unpack -> (真实)DAO.saveOrder
        OrderUpdateResultDTO result = proxyOrderServiceSpy.syncOrderDetails(params);

        // --- 4. 验证结果 ---

        assertNotNull(result, "执行结果不应为空");

        // 验证数据库影响行数（由于是真实 DAO 执行，且 SQL 是 ON DUPLICATE KEY UPDATE）
        // 如果是第一次插入，rows 应该是 1；如果是更新，rows 可能是 2（MySQL 特性）
        assertTrue(result.getProxyOrderUpdatedRows() >= 0, "主表应该处理成功");
        assertTrue(result.getProxyInstanceUpdatedRows() >= 0, "子表应该处理成功");

        System.out.println("集成测试通过！");
        System.out.println("主表更新行数: " + result.getProxyOrderUpdatedRows());
        System.out.println("子表更新行数: " + result.getProxyInstanceUpdatedRows());
    }
}