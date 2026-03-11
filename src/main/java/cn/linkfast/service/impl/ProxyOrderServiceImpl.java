package cn.linkfast.service.impl;

import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.entity.ProxyOrder;
import cn.linkfast.service.ProxyOrderService;
import cn.linkfast.utils.ApiPacketUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyOrderServiceImpl implements ProxyOrderService {

    private final ProxyOrderDAO proxyOrderDAO;
    private final ObjectMapper objectMapper;
    private final ApiPacketUtil apiPacketUtil;

    @Value("${api.ipv.env}")
    private String env;

    @Value("${api.ipv.sandbox_url}")
    private String sandboxUrl;

    @Value("${api.ipv.prod_url}")
    private String prodUrl;

    @Value("${api.ipv.path.product_query}")
    private String productQueryPath;

    private String baseUrl; // 动态确定的基础地址

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
    }

    @Override
    public int syncOrderDetails(Map<String, Object> params) throws Exception {


        // 拼接完整的请求 URL
        String fullUrl = baseUrl + productQueryPath;

        // 业务参数转成最终的请求参数
        Map<String, Object> finalRequest = apiPacketUtil.pack(params);

        // 发送 HTTP 请求
        String responseStr = sendPost(fullUrl, finalRequest);

        return processResponse(responseStr);

    }

    private String sendPost(String url, Map<String, Object> body) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            return client.execute(post, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        }
    }

    private int processResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        if (root.path("code").asInt() == 200) {
            String encryptedData = root.path("data").asText();
            if (encryptedData == null || encryptedData.isEmpty()) return 0;

            // 解密响应数据
            String decryptedJson = apiPacketUtil.unpack(encryptedData);
            log.info("接口返回数据解密成功: {}", decryptedJson);
            // 将解密后的 JSON 转换为 ProxyProduct 列表
            ProxyOrder order = objectMapper.readValue(decryptedJson, new TypeReference<>() {
            });
            return proxyOrderDAO.saveOrder(order);
        } else {
            throw new RuntimeException("API错误: " + root.path("msg").asText());
        }
    }
}