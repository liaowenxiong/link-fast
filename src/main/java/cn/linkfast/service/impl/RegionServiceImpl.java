package cn.linkfast.service.impl;

import cn.linkfast.dao.RegionDAO;
import cn.linkfast.entity.Region;
import cn.linkfast.exception.BusinessException;
import cn.linkfast.service.RegionService;
import cn.linkfast.utils.ApiPacketUtil;
import cn.linkfast.vo.AreaDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 地域同步实现类：解析树形结构并落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final ObjectMapper objectMapper;
    private final ApiPacketUtil apiPacketUtil;
    private final RegionDAO regionDAO;

    @Value("${api.ipv.env}")
    private String env;

    @Value("${api.ipv.sandbox_url}")
    private String sandboxUrl;

    @Value("${api.ipv.prod_url}")
    private String prodUrl;

    @Value("${api.ipv.path.area_list}")
    private String areaListPath;

    private String baseUrl;

    /**
     * 初始化：根据环境开关选择 BaseUrl
     */
    @PostConstruct
    public void init() {
        if ("prod".equalsIgnoreCase(env)) {
            this.baseUrl = prodUrl;
        } else {
            this.baseUrl = sandboxUrl;
        }
    }

    @Override
    public List<AreaDTO> queryRegionTree(List<String> codes) {
        try {
            // 1. 构造请求参数
            Map<String, Object> params = new HashMap<>();
            if (codes != null && !codes.isEmpty()) {
                params.put("codes", codes);
            }

            // 2. 加密封装
            Map<String, Object> finalRequest = apiPacketUtil.pack(params);

            // 3. 发送请求
            String fullUrl = baseUrl + areaListPath;
            String responseStr = sendPost(fullUrl, finalRequest);

            // 4. 解析响应并解密
            return processResponse(responseStr);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取地域列表失败，请稍后重试", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncRegionTreeToDb(List<String> codes) throws Exception {
        // 1. 请求第三方“获取地域信息接口”
        Map<String, Object> params = new HashMap<>();
        if (codes != null && !codes.isEmpty()) {
            params.put("codes", codes);
        }
        Map<String, Object> finalRequest = apiPacketUtil.pack(params);
        String fullUrl = baseUrl + areaListPath;
        String responseStr = sendPost(fullUrl, finalRequest);

        // 2. 处理响应：解密 data -> 反序列化为树形 AreaDTO
        List<AreaDTO> tree = processResponse(responseStr);
        if (tree.isEmpty()) {
            log.warn("地域同步：第三方返回为空，codes={}", codes);
            return 0;
        }

        // 2. 扁平化为 Region 集合，并记录 parentCode 以便后续回填 parent_id
        List<Region> regionList = new ArrayList<>();
        Map<String, String> regionCodeToParentCode = new HashMap<>();
        flattenTree(tree, null, 1, new ArrayList<>(), new ArrayList<>(), regionList, regionCodeToParentCode);

        if (regionList.isEmpty()) {
            return 0;
        }

        // 3. 第一次 upsert：region_code 唯一，先把节点写入（parent_id 暂时可能为 0）
        regionDAO.batchSaveOrUpdate(regionList);

        // 4. 查询写入后的 id，回填 parent_id
        List<String> regionCodes = regionList.stream().map(Region::getRegionCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<String, Long> idMap = regionDAO.selectIdMapByRegionCodes(regionCodes);
        for (Region r : regionList) {
            String parentCode = regionCodeToParentCode.get(r.getRegionCode());
            if (parentCode == null) {
                r.setParentId(0L);
                continue;
            }
            Long parentId = idMap.get(parentCode);
            if (parentId == null) {
                // 理论上不应该发生：因为我们上面已经同步了完整树
                log.warn("地域同步：找不到 parentId，regionCode={}, parentCode={}", r.getRegionCode(), parentCode);
                r.setParentId(0L);
            } else {
                r.setParentId(parentId);
            }
        }

        // 5. 第二次 upsert：更新 parent_id
        regionDAO.batchSaveOrUpdate(regionList);
        return regionList.size();
    }

    private String sendPost(String url, Map<String, Object> body) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
            return client.execute(post, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        }
    }

    /**
     * 解析第三方API响应，解密后转为 AreaDTO 列表
     */
    private List<AreaDTO> processResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        if (root.path("code").asInt() == 200) {
            String encryptedData = root.path("data").asText();
            if (encryptedData == null || encryptedData.isEmpty()) {
                log.warn("地域接口返回 data 为空");
                return Collections.emptyList();
            }

            String decryptedJson = apiPacketUtil.unpack(encryptedData);
            log.info("地域接口返回数据解密成功：{}", decryptedJson);

            List<AreaDTO> areaList = objectMapper.readValue(decryptedJson, new TypeReference<>() {
            });
            return areaList != null ? areaList : Collections.emptyList();
        } else {
            throw new BusinessException("地域API错误: " + root.path("msg").asText());
        }
    }

    private void flattenTree(List<AreaDTO> nodes, String parentCode, int level, List<String> codePath, List<String> cnamePath, List<Region> out, Map<String, String> regionCodeToParentCode) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            AreaDTO node = nodes.get(i);
            if (node == null || node.getCode() == null) {
                continue;
            }

            String regionCode = node.getCode();
            String cname = node.getCname();
            String nameEn = node.getName();

            // 构建全路径（full_code/full_name）
            List<String> nextCodePath = new ArrayList<>(codePath);
            List<String> nextCnamePath = new ArrayList<>(cnamePath);
            nextCodePath.add(regionCode);
            nextCnamePath.add(cname);

            String fullCode = String.join("-", nextCodePath);
            String fullName = nextCnamePath.stream().filter(Objects::nonNull).collect(Collectors.joining("-"));

            Region region = new Region();
            region.setParentId(0L); // 后续在 upsert 后回填
            region.setLevel(level);
            region.setRegionCode(regionCode);
            region.setRegionName(cname);
            region.setRegionEnName(nameEn == null ? "" : nameEn);
            region.setSort(i);
            region.setFullCode(fullCode);
            region.setFullName(fullName);
            region.setStatus(1);
            out.add(region);

            regionCodeToParentCode.put(regionCode, parentCode);

            // 递归子节点
            flattenTree(node.getChildren(), regionCode, level + 1, nextCodePath, nextCnamePath, out, regionCodeToParentCode);
        }
    }
}

