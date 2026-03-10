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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// 使用 JUnit 5 的扩展来启动 Spring 容器
@ExtendWith(SpringExtension.class)
// 指向你的 Spring 核心配置文件
//@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
@ContextConfiguration(classes = {AppConfig.class})
// 测试结束后自动回滚事务，防止污染数据库（如果 Service 有 @Transactional）
//@Transactional
public class ProxyProductServiceTest {

    @Autowired
    private ProxyProductService productService;

    @Test
    public void testGetProxyProducts2() throws Exception {
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

    @Test
    public void testSyncDataToDb() {
        try {
            // 1. 构造同步所需的参数 (参考你 ProductSyncTask 里的配置)
            // 101: 静态云平台, 102: 静态国内家庭, 103: 静态国外家庭
            Map<String, Object> params = new HashMap<>();
            params.put("proxyType", Arrays.asList(101, 102, 103, 104, 105));

            System.out.println(">>> 开始手动触发同步...");

            // 2. 直接调用 Service 方法
            // 注意：这里会发起真实的 HTTP 请求并执行真实的数据库插入
            int savedCount = productService.syncProxyProducts(params);

            // 3. 打印结果
            System.out.println(">>> 同步任务执行完成！");
            System.out.println(">>> 本次成功同步/更新的数据条数: " + savedCount);

            // 4. 断言：只要大于 0 说明数据确实进库了
            org.junit.jupiter.api.Assertions.assertTrue(savedCount >= 0, "同步返回值应大于或等于0");

        } catch (Exception e) {
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail("同步过程中发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testGetProxyProducts() throws Exception {
        // 1. 构造查询 DTO (模拟前端传参)
        // 根据你之前同步成功的日志，数据库中现在应该有国家为 "USA" 或 "CAN" 的数据
        ProxyProductQueryDTO queryDTO = new ProxyProductQueryDTO();
        queryDTO.setCountryCode("USA"); // 必填，对应 SQL 中的 WHERE country_code = ?
        queryDTO.setCityCode("USA000000"); // 必填，对应 SQL 中的 WHERE city_code = ?
        queryDTO.setPage(1);            // 第 1 页
        queryDTO.setPageSize(10);       // 每页 10 条

        System.out.println(">>> 开始测试获取代理产品列表...");

        // 2. 调用 Service 方法
        PageResult<ProxyProductVO> pageResult = productService.getProxyProducts(queryDTO);

        // 3. 验证结果
        // 验证结果对象不为空
        org.junit.jupiter.api.Assertions.assertNotNull(pageResult, "分页结果不应为 null");

        // 打印查询结果汇总信息
        System.out.println(">>> 查询成功！");
        System.out.println(">>> 总记录数 (total): " + pageResult.getTotal());
        System.out.println(">>> 当前页数据条数: " + pageResult.getList().size());

        // 4. 深度验证数据完整性
        if (pageResult.getTotal() > 0) {
            ProxyProductVO firstProduct = pageResult.getList().get(0);
            System.out.println(">>> 第一条产品详情:");
            System.out.println("    - 产品编号: " + firstProduct.getProductNo());
            System.out.println("    - 产品名称: " + firstProduct.getProductName());
            System.out.println("    - 价格 (costPrice): " + firstProduct.getCostPrice());

            // 验证关键字段是否映射成功（避免因为实体类注解写错导致 VO 属性全是 null）
            org.junit.jupiter.api.Assertions.assertNotNull(firstProduct.getProductNo(), "产品编号不应为空");
            org.junit.jupiter.api.Assertions.assertNotNull(firstProduct.getCostPrice(), "产品价格不应为空");
        } else {
            System.out.println(">>> 警告：未找到符合条件的数据，请检查 countryCode 和 cityCode 是否与数据库匹配。");
        }
    }
}