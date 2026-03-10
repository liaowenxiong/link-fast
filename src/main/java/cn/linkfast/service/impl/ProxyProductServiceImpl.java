package cn.linkfast.service.impl;

import cn.linkfast.common.PageResult;
import cn.linkfast.dao.ProxyProductDAO;
import cn.linkfast.dto.ProxyProductQueryDTO;
import cn.linkfast.entity.ProxyProduct;
import cn.linkfast.entity.ProxyProductSearchCondition;
import cn.linkfast.service.ProxyProductService;
import cn.linkfast.utils.ApiPacketUtil;
import cn.linkfast.vo.ProxyProductVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProxyProductServiceImpl implements ProxyProductService {
    /*static {
        // 关键防御代码：在代码层强制限制 JSON 嵌套深度
        // 这样即便黑客利用漏洞发送恶意 JSON，Jackson 也会在 1000 层时直接报错，不会导致服务器崩溃
        StreamReadConstraints constraints = StreamReadConstraints.builder().maxNestingDepth(1000).build();
        objectMapper.getFactory().setStreamReadConstraints(constraints);
    }*/

    private final ObjectMapper objectMapper;
    private final ProxyProductDAO proxyProductDAO;
    private final ApiPacketUtil apiPacketUtil;
    // --- 注入配置 ---
    @Value("${api.ipv.env}")
    private String env;

    @Value("${api.ipv.sandbox_url}")
    private String sandboxUrl;

    @Value("${api.ipv.prod_url}")
    private String prodUrl;

    @Value("${api.ipv.path.product_query}")
    private String productQueryPath;

    private String baseUrl; // 动态确定的基础地址

    private static @NonNull ProxyProductSearchCondition buildSearchCondition(@NonNull ProxyProductQueryDTO queryDto) {
        ProxyProductSearchCondition condition = new ProxyProductSearchCondition();
        condition.setCountryCode(queryDto.getCountryCode());
        condition.setCityCode(queryDto.getCityCode());

        // 处理分页逻辑：只有当前端传了分页参数时才计算
        if (queryDto.getPage() != null && queryDto.getPageSize() != null) {
            condition.setLimit(queryDto.getPageSize());
            // 公式：offset = (当前页码 - 1) * 每页条数
            int offset = (queryDto.getPage() - 1) * queryDto.getPageSize();
            condition.setOffset(Math.max(offset, 0)); // 防止负数
        }
        return condition;
    }

    /**
     * 初始化：根据环境开关选择 BaseUrl，并准备 AES 的 IV
     */
    @PostConstruct
    public void init() {
        // 1. 确定环境地址
        if ("prod".equalsIgnoreCase(env)) {
            this.baseUrl = prodUrl;
        } else {
            this.baseUrl = sandboxUrl;
        }

        // 2. 准备 AES IV (Key的前16位)
//        if (appSecret != null && appSecret.length() >= 16) {
//            this.aesIv = appSecret.substring(0, 16);
//        }
    }

    @Override
    public int syncProxyProducts(Map<String, Object> params) throws Exception {

        // 拼接完整的请求 URL
        String fullUrl = baseUrl + productQueryPath;

        // 业务参数转成最终的请求参数
        Map<String, Object> finalRequest = apiPacketUtil.pack(params);

        // 发送 HTTP 请求
        String responseStr = sendPost(fullUrl, finalRequest);

        return processResponse(responseStr);
    }

    @Override
    public PageResult<ProxyProductVO> getProxyProducts(ProxyProductQueryDTO queryDto) {
        // 1. DTO 转 SearchCondition (计算 offset)
        ProxyProductSearchCondition condition = buildSearchCondition(queryDto);

        // 2. 查询总条数 (用于分页组件显示总页数)
        int total = proxyProductDAO.countProxyProduct(condition);
        if (total == 0) {
            return new PageResult<>(0, List.of(), queryDto.getPage(), queryDto.getPageSize());
        }

        // 3. 执行数据查询 (Entity 列表)
        List<ProxyProduct> entityList = proxyProductDAO.findProxyProductList(condition);

        // 4. 将 Entity 转换为 VO (数据脱敏/格式转换)
        List<ProxyProductVO> voList = entityList.stream().map(this::convertToVO).collect(Collectors.toList());

        // 5. 封装返回
        return new PageResult<>(total, voList, queryDto.getPage(), queryDto.getPageSize());
    }

    /**
     * 将数据库实体转换为展示对象
     */
    private ProxyProductVO convertToVO(ProxyProduct entity) {
        ProxyProductVO vo = new ProxyProductVO();
        // 使用 Spring 的 BeanUtils 快速拷贝同名属性
        BeanUtils.copyProperties(entity, vo);
        // 这里可以做特殊逻辑，比如：
        // if (entity.getProxyType() == 1) vo.setProxyTypeName("静态代理");
        return vo;
    }

    // 解析响应数据
    private int processResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        if (root.path("code").asInt() == 200) {
            String encryptedData = root.path("data").asText();
            if (encryptedData == null || encryptedData.isEmpty()) return 0;

            // 解密响应数据
            String decryptedJson = apiPacketUtil.unpack(encryptedData);
            log.info("接口返回数据解密成功: {}", decryptedJson);
            // 将解密后的 JSON 转换为 ProxyProduct 列表
            List<ProxyProduct> productList = objectMapper.readValue(decryptedJson, new TypeReference<>() {
            });
            return proxyProductDAO.batchSaveOrUpdate(productList);
        } else {
            throw new RuntimeException("API错误: " + root.path("msg").asText());
        }
    }


    /**
     * 简单的 HttpClient 发送方法
     */
    private String sendPost(String url, Map<String, Object> body) throws Exception {
        // 1. 创建客户端（建议生产环境下将 client 提取为全局变量或使用连接池）
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            // 2. 构造请求对象
            HttpPost post = new HttpPost(url);
            String json = objectMapper.writeValueAsString(body);

            // 3. 设置请求体，HC5 推荐直接在 StringEntity 中指定 ContentType
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            // 4. 使用 ResponseHandler 模式执行请求
            // 这种方式会自动管理 HttpResponse 的生命周期，确保流被关闭
            return client.execute(post, response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } else {
                    log.error("HTTP 请求失败，状态码: {}", status);
                    return "{\"code\":" + status + ", \"msg\":\"HTTP Error\"}";
                }
            });
        }
    }

}