package cn.linkfast.service.impl;

import cn.linkfast.common.PageResult;
import cn.linkfast.dao.ProxyInstanceDAO;
import cn.linkfast.dao.ProxyRegionDAO;
import cn.linkfast.dto.ProxyInstanceQueryDTO;
import cn.linkfast.dto.ProxyInstanceSearchCondition;
import cn.linkfast.entity.ProxyInstance;
import cn.linkfast.entity.ProxyRegion;
import cn.linkfast.service.ProxyInstanceService;
import cn.linkfast.utils.ApiPacketUtil;
import cn.linkfast.utils.HttpClientUtil;
import cn.linkfast.vo.ProxyInstanceSyncResultVO;
import cn.linkfast.vo.ProxyInstanceVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代理实例服务实现类
 *
 * @author liaowenxiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyInstanceServiceImpl implements ProxyInstanceService {

    private final ObjectMapper objectMapper;
    private final ProxyInstanceDAO proxyInstanceDAO;
    private final ProxyRegionDAO proxyRegionDAO;
    private final ApiPacketUtil apiPacketUtil;

    @Value("${api.ipv.env}")
    private String env;

    @Value("${api.ipv.sandbox_url}")
    private String sandboxUrl;

    @Value("${api.ipv.prod_url}")
    private String prodUrl;

    @Value("${api.ipv.path.instance_query}")
    private String instanceQueryPath;

    private String baseUrl;

    private static ProxyInstanceSearchCondition buildSearchCondition(ProxyInstanceQueryDTO queryDto) {
        ProxyInstanceSearchCondition condition = new ProxyInstanceSearchCondition();
        condition.setProxyType(queryDto.getProxyType());
        condition.setStatus(queryDto.getStatus());
        condition.setCountryCode(queryDto.getCountryCode());
        condition.setCityCode(queryDto.getCityCode());
        condition.setIp(queryDto.getIp());
        condition.setInstanceNo(queryDto.getInstanceNo());

        if (queryDto.getPageNum() != null && queryDto.getPageSize() != null) {
            condition.setLimit(queryDto.getPageSize());
            int offset = (queryDto.getPageNum() - 1) * queryDto.getPageSize();
            condition.setOffset(Math.max(offset, 0));
        }
        return condition;
    }

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
    @Transactional(rollbackFor = Exception.class)
    public ProxyInstanceSyncResultVO syncProxyInstance(List<String> instanceNos) throws Exception {
        // 1. 构造请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("instances", instanceNos);

        // 2. 拼接完整的请求 URL
        String fullUrl = baseUrl + instanceQueryPath;

        // 3. 业务参数加密封装
        Map<String, Object> finalRequest = apiPacketUtil.pack(params);

        // 4. 发送 HTTP 请求，获得业务 Result 的 JSON 字符串
        String responseStr = HttpClientUtil.sendPost(fullUrl, finalRequest, objectMapper);

        // 5. 解析响应，获得业务数据
        List<ProxyInstance> instanceList = processResponse(responseStr);

        // 6. 保存或更新到数据库
        ProxyInstanceSyncResultVO result = new ProxyInstanceSyncResultVO();
        if (instanceList.isEmpty()) {
            result.setExpectedCount(0);
            result.setActualCount(0);
            return result;
        }

        int expectedCount = instanceList.size();
        int actualCount = proxyInstanceDAO.batchUpdate(instanceList);
        result.setExpectedCount(expectedCount);
        result.setActualCount(actualCount);
        return result;
    }

    @Override
    public PageResult<ProxyInstanceVO> queryProxyInstances(ProxyInstanceQueryDTO queryDto) {
        log.info("Service层开始查询代理实例列表，查询条件：{}", queryDto);
        // 1. DTO 转 SearchCondition（计算 offset）
        ProxyInstanceSearchCondition condition = buildSearchCondition(queryDto);

        // 2. 查询总条数
        int total = proxyInstanceDAO.countByCondition(condition);
        if (total == 0) {
            return new PageResult<>(0, List.of(), queryDto.getPageNum(), queryDto.getPageSize());
        }

        // 3. 执行数据查询
        List<ProxyInstance> entityList = proxyInstanceDAO.selectListByCondition(condition);

        // 4. 批量查询地域信息（消除 N+1）
        Map<String, ProxyRegion> regionMap = batchLoadRegions(entityList);

        // 5. Entity 转 VO
        List<ProxyInstanceVO> voList = entityList.stream()
                .map(entity -> convertToVO(entity, regionMap))
                .collect(Collectors.toList());

        // 6. 封装返回
        return new PageResult<>(total, voList, queryDto.getPageNum(), queryDto.getPageSize());
    }

    /**
     * 批量加载地域信息，一次 IN 查询替代 N+1 逐条查询
     */
    private Map<String, ProxyRegion> batchLoadRegions(List<ProxyInstance> entityList) {
        List<String> regionCodes = new ArrayList<>();
        for (ProxyInstance entity : entityList) {
            if (entity.getCountryCode() != null && !entity.getCountryCode().isEmpty()) {
                regionCodes.add(entity.getCountryCode());
            }
            if (entity.getCityCode() != null && !entity.getCityCode().isEmpty()) {
                regionCodes.add(entity.getCityCode());
            }
        }
        if (regionCodes.isEmpty()) {
            return Map.of();
        }
        return proxyRegionDAO.selectByRegionCodes(regionCodes);
    }

    private ProxyInstanceVO convertToVO(ProxyInstance entity, Map<String, ProxyRegion> regionMap) {
        ProxyInstanceVO vo = new ProxyInstanceVO();
        BeanUtils.copyProperties(entity, vo);

        // 拼接地域中文名：国家-城市（有值则拼，无则跳过）
        StringBuilder fullRegionName = new StringBuilder();
        if (entity.getCountryCode() != null && !entity.getCountryCode().isEmpty()) {
            ProxyRegion country = regionMap.get(entity.getCountryCode());
            fullRegionName.append(country != null ? country.getRegionName() : entity.getCountryCode());
        }
        if (entity.getCityCode() != null && !entity.getCityCode().isEmpty()) {
            ProxyRegion city = regionMap.get(entity.getCityCode());
            String cityName = city != null ? city.getRegionName() : entity.getCityCode();
            if (!fullRegionName.isEmpty()) {
                fullRegionName.append("-");
            }
            fullRegionName.append(cityName);
        }
        if (!fullRegionName.isEmpty()) {
            vo.setFullRegionName(fullRegionName.toString());
        }
        return vo;
    }

    @Override
    public void updateRemark(String instanceNo, String remark) {
        int rows = proxyInstanceDAO.updateRemarkByInstanceNo(instanceNo, remark);
        if (rows == 0) {
            throw new cn.linkfast.exception.BusinessException(400, "实例不存在: " + instanceNo);
        }
    }

    @Override
    public void updateRenewStatus(String instanceNo, Integer renew) {
        int rows = proxyInstanceDAO.updateRenewByInstanceNo(instanceNo, renew);
        if (rows == 0) {
            throw new cn.linkfast.exception.BusinessException(400, "实例不存在: " + instanceNo);
        }
    }

    @Override
    public List<ProxyInstance> getAutoRenewExpiringInstances(int days) {
        return proxyInstanceDAO.selectAutoRenewExpiringInstances(days);
    }

    /**
     * 解析第三方API响应的业务数据
     */
    private List<ProxyInstance> processResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        if (root.path("code").asInt() == 200) {
            String encryptedData = root.path("data").asText();
            log.info("实例接口返回的加密数据: {}", encryptedData);
            if (encryptedData == null || encryptedData.isEmpty()) {
                log.warn("实例接口返回 data 为空");
                return Collections.emptyList();
            }

            // 解密响应数据
            String decryptedJson = apiPacketUtil.unpack(encryptedData);
            log.info("实例接口返回数据解密成功: {}", decryptedJson);

            if (decryptedJson == null || decryptedJson.isEmpty() || "[]".equals(decryptedJson.trim())) {
                log.warn("实例接口返回数据解密后为空");
                return Collections.emptyList();
            }

            // 将解密后的 JSON 转换为 ProxyInstance 列表
            List<ProxyInstance> instanceList = objectMapper.readValue(
                    decryptedJson, new TypeReference<List<ProxyInstance>>() {
                    });

            return instanceList;

        } else {
            throw new RuntimeException("获取实例信息API错误: " + root.path("msg").asText());
        }
    }

}
