package cn.linkfast.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// cn.linkfast.utils.ApiPacketUtil.java
public class ApiPacketUtil {
    private static final String APP_KEY = "你的appKey"; //
    private static final String AES_KEY = "qwertyuiop123456asdfghjk"; //
    private static final String AES_IV = AES_KEY.substring(0, 16); //
    private static final ObjectMapper mapper = new ObjectMapper();

    // 包装并加密请求
    public static Map<String, Object> pack(Object businessParams) throws Exception {
        String json = mapper.writeValueAsString(businessParams);
        byte[] encrypted = AESCBC.encryptCBC(json.getBytes(), AES_KEY.getBytes(), AES_IV.getBytes());
        String base64Params = Base64.getEncoder().encodeToString(encrypted); //

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("version", "v2.0"); //
        requestMap.put("encrypt", "aes"); //
        requestMap.put("appKey", APP_KEY); //
        requestMap.put("reqId", UUID.randomUUID().toString().replace("-", "")); //
        requestMap.put("params", base64Params); //
        return requestMap;
    }

    // 解密响应数据
    public static String unpack(String encryptedData) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        byte[] decrypted = AESCBC.decryptCBC(decoded, AES_KEY.getBytes(), AES_IV.getBytes());
        return new String(decrypted); //
    }
}