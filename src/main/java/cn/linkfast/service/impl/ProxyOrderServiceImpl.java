package cn.linkfast.service.impl;

import cn.linkfast.common.PageResult;
import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.dto.OrderUpdateResultDTO;
import cn.linkfast.dto.ProxyOrderQueryDTO;
import cn.linkfast.dto.ProxyOrderSearchCondition;
import cn.linkfast.entity.ProxyOrder;
import cn.linkfast.service.ProxyOrderService;
import cn.linkfast.utils.ApiPacketUtil;
import cn.linkfast.vo.ProxyOrderVO;
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

    @Value("${api.ipv.path.order_info}")
    private String orderQueryPath;

    private String baseUrl; // 动态确定的基础地址

    private static @NonNull ProxyOrderSearchCondition buildSearchCondition(@NonNull ProxyOrderQueryDTO queryDto) {
        ProxyOrderSearchCondition condition = new ProxyOrderSearchCondition();
        BeanUtils.copyProperties(queryDto, condition);

        // 处理分页逻辑：只有当前端传了分页参数时才计算
        if (queryDto.getPageNum() != null && queryDto.getPageSize() != null) {
            condition.setLimit(queryDto.getPageSize());
            // 公式：offset = (当前页码 - 1) * 每页条数
            int offset = (queryDto.getPageNum() - 1) * queryDto.getPageSize();
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
    }

    @Override
    public OrderUpdateResultDTO syncOrderDetails(Map<String, Object> params) throws Exception {


        // 拼接完整的请求 URL
        String fullUrl = baseUrl + orderQueryPath;

        // 业务参数转成最终的请求参数
        Map<String, Object> finalRequest = apiPacketUtil.pack(params);

        // 发送 HTTP 请求，返回的是 HTTP 响应体（Response Body）的全文内容
        String responseStr = sendPost(fullUrl, finalRequest);

        ProxyOrder order = processResponse(responseStr);
        if (order == null) return new OrderUpdateResultDTO();
        return proxyOrderDAO.saveOrder(order);
    }

    @Override
    public PageResult<ProxyOrderVO> getProxyOrders(ProxyOrderQueryDTO dto) {
        // 1. DTO转换为DAO查询条件
        ProxyOrderSearchCondition condition = buildSearchCondition(dto);

        // 2. 调用DAO查询数据（列表+总数）
        List<ProxyOrder> orderList = proxyOrderDAO.findProxyOrderList(condition);
        int total = proxyOrderDAO.countProxyOrder(condition);

        // 3. 实体转换为VO（包含instanceTotal）
        // 4. 将 Entity 转换为 VO (数据脱敏/格式转换)
        List<ProxyOrderVO> voList = orderList.stream().map(this::convertToVO).collect(Collectors.toList());

        // 4. 封装分页结果返回
        return new PageResult<>(total, voList, dto.getPageNum(), dto.getPageSize());

    }

    /**
     * 将数据库实体转换为展示对象
     */
    private ProxyOrderVO convertToVO(ProxyOrder entity) {
        ProxyOrderVO vo = new ProxyOrderVO();
        // 使用 Spring 的 BeanUtils 快速拷贝同名属性
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public String sendPost(String url, Map<String, Object> body) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            return client.execute(post, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        }
    }

    private ProxyOrder processResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        if (root.path("code").asInt() == 200) {
            String encryptedData = root.path("data").asText();
            if (encryptedData == null || encryptedData.isEmpty()) return null;

            // 解密响应数据
            String decryptedJson = apiPacketUtil.unpack(encryptedData);
            log.info("接口返回数据解密成功: {}", decryptedJson);
            // 将解密后的 JSON 转换为 ProxyOrder
            return objectMapper.readValue(decryptedJson, new TypeReference<>() {
            });
        } else {
            throw new RuntimeException("API错误: " + root.path("msg").asText());
        }
    }
}