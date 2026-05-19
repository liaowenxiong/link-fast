package cn.linkfast.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API 数据包加密与解密工具类
 * 已改为 Spring 组件，支持从 api.properties 读取配置
 */
@Slf4j
@Component
public class ApiPacketUtil {

    // 使用注入的方式或静态初始化均可，这里建议由 Spring 管理单例
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${api.ipv.env}")
    private String env;
    @Value("${api.ipv.sandbox.appKey}")
    private String sandboxAppKey;
    @Value("${api.ipv.prod.appKey}")
    private String prodAppKey;
    @Value("${api.ipv.sandbox.appSecret}")
    private String sandboxAppSecret;
    @Value("${api.ipv.prod.appSecret}")
    private String prodAppSecret;
    private String aesIv;
    private String appKey;
    private String appSecret;

    /**
     * 初始化方法：在属性注入完成后计算 IV
     */
    @PostConstruct
    public void init() {
        if ("prod".equalsIgnoreCase(env)) {
            this.appKey = prodAppKey;
            this.appSecret = prodAppSecret;
        } else {
            this.appKey = sandboxAppKey;
            this.appSecret = sandboxAppSecret;
        }
        if (appSecret != null && appSecret.length() >= 16) {
            this.aesIv = appSecret.substring(0, 16);
        }
    }

    /**
     * 将业务参数加密、编码，并封装成最终的请求 Map
     */
    public Map<String, Object> pack(Map<String, Object> businessParams) throws Exception {
        // 1. 业务对象转 JSON，支持无业务参数
//        Object paramsObj = businessParams == null ? new HashMap<>() : businessParams;
        Map<String, Object> requestMap = new HashMap<>();
        if (businessParams == null) {
            // 4. 组装接口文档要求的公共参数
            requestMap.put("version", "2.0"); // 对应文档 v2 版本
            requestMap.put("encrypt", "aes");
            requestMap.put("appKey", appKey);
            requestMap.put("reqId", UUID.randomUUID().toString().replace("-", ""));

            return requestMap;
        }

        String json = mapper.writeValueAsString(businessParams);

        // 2. AES CBC 加密
        byte[] encrypted = AESCBC.encryptCBC(
                json.getBytes(),
                appSecret.getBytes(),
                aesIv.getBytes()
        );

        // 3. 转 Base64
        String base64Params = Base64.getEncoder().encodeToString(encrypted);

        // 4. 组装接口文档要求的公共参数
        requestMap.put("version", "2.0"); // 对应文档 v2 版本
        requestMap.put("encrypt", "aes");
        requestMap.put("appKey", appKey);
        requestMap.put("reqId", UUID.randomUUID().toString().replace("-", ""));
        requestMap.put("params", base64Params);

        return requestMap;
    }

    /**
     * 解密响应数据中的 data 字段
     */
    public String unpack(String encryptedData) throws Exception {
        log.info("待解密数据: {}", encryptedData);
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        byte[] decrypted = AESCBC.decryptCBC(decoded, appSecret.getBytes(), aesIv.getBytes());
        return new String(decrypted);
    }
}