package cn.linkfast.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Apache HttpClient 5 简单封装：POST JSON。
 */
@Slf4j
public final class HttpClientUtil {

    private HttpClientUtil() {
    }

    /**
     * 以 JSON 发送 POST；HTTP 状态非 2xx 时返回包含状态码的 JSON，便于上层按业务 code 解析。
     */
    public static String sendPost(String url, Map<String, Object> body, ObjectMapper objectMapper) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            String json = objectMapper.writeValueAsString(body);
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            return client.execute(post, response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    // 返回业务 Result 的 JSON 字符串。
                    return result != null ? result : "";
                } else {
                    log.error("HTTP 请求失败，状态码: {}", status);
                    return "{\"code\":" + status + ", \"msg\":\"HTTP Error\"}";
                }
            });
        }
    }
}
