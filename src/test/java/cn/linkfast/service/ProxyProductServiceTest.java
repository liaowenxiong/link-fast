package cn.linkfast.service;

import cn.linkfast.common.PageResult;
import cn.linkfast.config.AppConfig;
import cn.linkfast.dto.ProxyProductQueryDTO;
import cn.linkfast.vo.ProxyProductVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// 使用 JUnit 5 的扩展来启动 Spring 容器
@ExtendWith(SpringExtension.class)
// 指向你的 Spring 核心配置文件
//@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
@ContextConfiguration(classes = {AppConfig.class})
// 测试结束后自动回滚事务，防止污染数据库（如果 Service 有 @Transactional）
@Transactional
public class ProxyProductServiceTest {

    @Autowired
    private ProxyProductService productService;

    @Test
    public void testGetProxyProducts() throws Exception {
        // 1. 构建查询条件
        ProxyProductQueryDTO dto = new ProxyProductQueryDTO();
        dto.setCountryCode("US");
        dto.setPage(1);
        dto.setPageSize(10);

        // 2. 调用业务逻辑
        PageResult<ProxyProductVO> result = productService.getProxyProducts(dto);

        // 3. 断言验证
        assertNotNull(result, "返回结果不应为 null");
        System.out.println("查询结果条数: " + result.getList().size());
        System.out.println("查询结果总数: " + result.getTotal());
    }
}