package cn.linkfast.service.Impl;

import cn.linkfast.config.AppConfig;
import cn.linkfast.service.ProxyRegionService;
import cn.linkfast.dto.AreaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代理地域服务集成测试
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class})
public class ProxyRegionServiceImplIT {

    @Autowired
    private ProxyRegionService proxyRegionService;

    /**
     * 测试获取全部地域信息（不传 codes）
     */
    @Test
    public void testQueryRegionTreeAll() throws Exception {
        System.out.println("========== 测试获取全部地域树 ==========");

        List<AreaDTO> regionTree = proxyRegionService.queryRegionTree(null);

        assertNotNull(regionTree, "返回结果不应为 null");
        assertFalse(regionTree.isEmpty(), "返回结果不应为空");

        System.out.println("顶级地域数量: " + regionTree.size());

        // 格式化输出 JSON，方便查看树形结构
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(regionTree);
        System.out.println("地域树形数据：");
        System.out.println(json);
    }

    /**
     * 测试获取指定地域信息（传入 codes）
     */
    @Test
    public void testQueryRegionTreeByCodes() throws Exception {
        System.out.println("========== 测试获取指定地域树（US, CN） ==========");

        List<AreaDTO> regionTree = proxyRegionService.queryRegionTree(Arrays.asList("US", "CN"));

        assertNotNull(regionTree, "返回结果不应为 null");

        System.out.println("返回地域数量: " + regionTree.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(regionTree);
        System.out.println("地域树形数据：");
        System.out.println(json);
    }
}
