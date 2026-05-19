package cn.linkfast.service.impl;

import cn.linkfast.common.PageResult;
import cn.linkfast.dao.ProxyOrderDAO;
import cn.linkfast.dao.ProxyProductDAO;
import cn.linkfast.dto.*;
import cn.linkfast.entity.*;
import cn.linkfast.exception.BusinessException;
import cn.linkfast.exception.NoRollbackBusinessException;
import cn.linkfast.service.PayService;
import cn.linkfast.service.ProxyOrderService;
import cn.linkfast.service.ProxyProductService;
import cn.linkfast.utils.ApiPacketUtil;
import cn.linkfast.utils.AppOrderNoGenerator;
import cn.linkfast.utils.HttpClientUtil;
import cn.linkfast.vo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ProxyProductDAO proxyProductDAO;
    private final PayService payService;
    private final AppOrderNoGenerator appOrderNoGenerator;
    private final ProxyProductService proxyProductService;
    @Value("${api.ipv.env}")
    private String env;
    @Value("${api.ipv.sandbox_url}")
    private String sandboxUrl;
    @Value("${api.ipv.prod_url}")
    private String prodUrl;
    @Value("${api.ipv.path.order_info}")
    private String orderQueryPath;
    @Value("${api.ipv.path.order_create}")
    private String orderOpenPath;
    @Value("${api.ipv.path.instance_renew}")
    private String instanceRenewPath;
    @Value("${api.ipv.path.instance_release}")
    private String instanceReleasePath;
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
    @Transactional(rollbackFor = Exception.class)
    public ProxyOrderUpdateResultDTO syncOrderDetails(Map<String, Object> params) throws Exception {

        // 拼接完整的请求 URL
        String fullUrl = baseUrl + orderQueryPath;

        // 业务参数转成最终的请求参数
        Map<String, Object> finalRequest = apiPacketUtil.pack(params);

        // 发送 HTTP 请求，返回的是 HTTP 响应体（Response Body）的全文内容
        log.info("同步订单详情 - 请求URL: {}, 请求参数: {}", fullUrl, objectMapper.writeValueAsString(finalRequest));
        String responseStr = sendPost(fullUrl, finalRequest);

        ProxyOrder order = processResponse(responseStr);
        if (order == null) return new ProxyOrderUpdateResultDTO();

        // 补全实例的本地字段：appOrderNo / orderId / userId / unit / duration
        List<ProxyInstance> instances = order.getInstances();
        if (instances != null && !instances.isEmpty() && order.getAppOrderNo() != null) {
            // 一次 DB 查询：本地订单（含 id、userId）
            ProxyOrder localOrder = proxyOrderDAO.selectByAppOrderNo(order.getAppOrderNo());
            Long localOrderId = localOrder != null ? localOrder.getId() : null;
            Long localUserId = localOrder != null ? localOrder.getUserId() : null;

            // 一次 DB 查询：购买明细（含 unit、duration），构建 productNo -> item 映射
            List<ProxyPurchaseOrderItem> purchaseItems = proxyOrderDAO.selectPurchaseItemsByAppOrderNo(order.getAppOrderNo());
            Map<String, ProxyPurchaseOrderItem> itemByProductNo = (purchaseItems != null ? purchaseItems.stream() : java.util.stream.Stream.<ProxyPurchaseOrderItem>empty()).collect(Collectors.toMap(ProxyPurchaseOrderItem::getProductNo, i -> i, (a, b) -> a));

            // 单次遍历，统一赋值所有本地补全字段
            for (ProxyInstance instance : instances) {
                if (instance.getAppOrderNo() == null) {
                    instance.setAppOrderNo(order.getAppOrderNo());
                }
                if (localOrderId != null) instance.setOrderId(localOrderId);
                if (localUserId != null) instance.setUserId(localUserId);
                if (instance.getProductNo() != null) {
                    ProxyPurchaseOrderItem item = itemByProductNo.get(instance.getProductNo());
                    if (item != null) {
                        instance.setUnit(item.getUnit());
                        instance.setDuration(item.getDuration());
                    }
                }
            }
        }

        return proxyOrderDAO.updateProxyPurchaseOrderByAppOrderNo(order);
    }

    @Override
    public PageResult<ProxyOrderVO> queryOrders(ProxyOrderQueryDTO dto) {
        // 1. DTO转换为DAO查询条件
        ProxyOrderSearchCondition condition = buildSearchCondition(dto);

        // 2. 调用DAO查询数据（列表+总数）
        List<ProxyOrder> orderList = proxyOrderDAO.selectListByCondition(condition);
        int total = proxyOrderDAO.countByCondition(condition);

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

    /**
     * 委托 {@link HttpClientUtil#sendPost}，保留为 public 便于集成测试 Spy 拦截网络调用。
     */
    public String sendPost(String url, Map<String, Object> body) throws Exception {
        return HttpClientUtil.sendPost(url, body, objectMapper);
    }

    private ProxyOrder processResponse(String responseStr) throws Exception {
        if (responseStr == null || responseStr.isEmpty()) {
            log.error("第三方API响应为空");
            throw new RuntimeException("第三方API响应为空");
        }
        log.info("第三方API原始响应: {}", responseStr);
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
            log.error("第三方API返回错误, code={}, msg={}, 完整响应: {}", root.path("code").asInt(), root.path("msg").asText(), responseStr);
            throw new RuntimeException("API错误: " + root.path("msg").asText());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoRollbackBusinessException.class)
    public ProxyPurchaseResultVO purchaseProxies(ProxyPurchaseDTO dto) {
        // 1. 校验支付密码
        PayPasswordVO payResult = payService.verifyPayPassword(dto.getPayPassword());
        if (!payResult.getPassed()) {
            throw new BusinessException(400, payResult.getMessage());
        }

        // 判断代理产品库存是否足够
        for (ProxyPurchaseItemDTO itemDto : dto.getParams()) {
            String productNo = itemDto.getProductNo();
            // 构建业务参数
            Map<String, Object> stockParams = new java.util.HashMap<>();
            stockParams.put("productNo", productNo);
            stockParams.put("proxyType", java.util.Arrays.asList(101, 102, 103, 104, 105, 201));
            List<ProxyProduct> products;
            try {
                products = proxyProductService.getProxyProducts(stockParams);
            } catch (Exception e) {
                log.error("查询产品库存失败，productNo: {}", productNo, e);
                throw new BusinessException("查询产品库存失败，请稍后重试");
            }
            if (products == null || products.isEmpty()) {
                throw new BusinessException(400, "产品不存在或已下架，productNo: " + productNo);
            }
            ProxyProduct product = products.get(0);
            Integer inventory = product.getInventory();
            Integer count = itemDto.getCount();
            if (inventory == null || inventory < count) {
                throw new BusinessException(400, "产品库存不足，productNo: " + productNo + "，当前库存: " + (inventory == null ? 0 : inventory) + "，需要数量: " + count);
            }
            // 库存充足，异步更新数据库产品信息，不阻塞当前下单线程
            final List<ProxyProduct> finalProducts = products;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    proxyProductDAO.batchSaveOrUpdate(finalProducts);
                } catch (Exception e) {
                    log.error("异步更新产品信息失败，productNo: {}", productNo, e);
                }
            });
        }

        /*
         * userId 目前没有传入参数，暂时写死一个值，后续可以通过鉴权上下文获取当前用户ID，或者在DTO中添加userId字段由调用方传入
         */
        Long userId = 2032958739262217115L;
        String appOrderNo = appOrderNoGenerator.generateBuyOrderId();
        log.info("成功生成appOrderNo，编号为：{}", appOrderNo);
        ProxyOrder order = new ProxyOrder();
        order.setAppOrderNo(appOrderNo);
        order.setUserId(userId);
        // 购买代理的订单类型就是1
        order.setOrderType(1);
        // 订单的初始状态就是1，表示待处理
        order.setStatus(1);
        order.setTotalQuantity(dto.getTotalQuantity());
        order.setHasRefund(0);
        // 先存储主订单数据
        Long orderId = proxyOrderDAO.insertOrder(order);
        List<ProxyPurchaseOrderItem> items = dto.getParams().stream().map(itemDto -> {
            ProxyPurchaseOrderItem item = new ProxyPurchaseOrderItem();
            // 订单明细需要存储代理购买订单的id
            item.setOrderId(orderId);
            // 订单明细需要存储代理购买订单的渠道商订单号
            item.setAppOrderNo(appOrderNo);
            item.setProductNo(itemDto.getProductNo());
            item.setProxyType(itemDto.getProxyType());
            item.setCountryCode(itemDto.getCountryCode());
            item.setStateCode(itemDto.getStateCode());
            item.setCityCode(itemDto.getCityCode());
            item.setUnit(itemDto.getUnit());
            item.setDuration(itemDto.getDuration());
            item.setCount(itemDto.getCount());
            item.setCycleTimes(itemDto.getCycleTimes());
            item.setFlow(0);
            item.setUseBridge(1);
            // item.setProjectId(itemDto.getProjectId());

            // 查库补全其它属性
            if (itemDto.getProductNo() != null) {
                ProxyProduct product = proxyProductDAO.selectByProductNo(itemDto.getProductNo());
                if (product == null) {
                    throw new BusinessException(400, "产品不存在: " + itemDto.getProductNo());
                }
                item.setUseType(product.getUseType());
                item.setProtocol(product.getProtocol());
                item.setUseLimit(product.getUseLimit());
                item.setAreaCode(product.getAreaCode());
                item.setDetail(product.getDetail());
                item.setCostPrice(product.getCostPrice());
                item.setRetailPrice(product.getRetailPrice());
                item.setIpType(product.getIpType());
                item.setIspType(product.getIspType());
                item.setNetType(product.getNetType());
                item.setBandWidth(product.getBandWidth());
                item.setBandWidthPrice(product.getBandWidthPrice());
                item.setMaxBandWidth(product.getMaxBandWidth());
                item.setFlow(product.getFlow());
                item.setCpu(product.getCpu());
                item.setMemory(product.getMemory());
                item.setSupplierCode(product.getSupplierCode());
                item.setIpCount(product.getIpCount());
                item.setIpDuration(product.getIpDuration());
                item.setParentNo(product.getParentNo());
                item.setProxyEverytimeChange(product.getProxyEverytimeChange());
                item.setProxyGlobalRandom(product.getProxyGlobalRandom());
                if (product.getProjectList() != null && !product.getProjectList().isEmpty()) {
                    item.setProjectId(product.getProjectList().get(0).getCode());
                }
            }
            return item;
        }).collect(Collectors.toList());
        order.setPurchaseItems(items);
        // 再存储订单明细数据
        proxyOrderDAO.insertProxyPurchaseOrderItems(order);
        String orderNo;
        BigDecimal amount = null;

        // 构建第三方API业务参数
        Map<String, Object> bizParams = new java.util.HashMap<>();
        bizParams.put("appOrderNo", appOrderNo);
        List<Map<String, Object>> paramList = new java.util.ArrayList<>();
        for (ProxyPurchaseOrderItem item : items) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("productNo", item.getProductNo());
            m.put("proxyType", item.getProxyType());
            m.put("countryCode", item.getCountryCode());
            m.put("stateCode", item.getStateCode());
            m.put("cityCode", item.getCityCode());
            m.put("unit", item.getUnit());
            m.put("duration", item.getDuration());
            m.put("count", item.getCount());
            m.put("cycleTimes", item.getCycleTimes());
            m.put("flow", item.getFlow());
            m.put("useBridge", item.getUseBridge());
            if (item.getProjectId() != null) {
                m.put("projectId", item.getProjectId());
            }
            paramList.add(m);
        }
        bizParams.put("params", paramList);
        try {
            Map<String, Object> req = apiPacketUtil.pack(bizParams);
            String url = baseUrl + orderOpenPath;
            log.info("开通代理 - 请求URL: {}, 请求参数: {}", url, objectMapper.writeValueAsString(req));

            // ===== 场景1：请求第三方接口失败，对方系统没有收到请求，重试3次，仍失败则回滚 =====
            String resp = null;
            Exception sendException = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    resp = HttpClientUtil.sendPost(url, req, objectMapper);
                    sendException = null;
                    break;
                } catch (java.net.ConnectException | java.net.UnknownHostException e) {
                    // 连接建立失败：对方系统完全没有收到请求，可以安全重试
                    sendException = e;
                    log.warn("开通代理 - 第{}次请求连接失败: {}", attempt, e.getMessage());
                    if (attempt < 3) {
                        try {
                            Thread.sleep(1000L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    // ===== 场景3：连接已建立，请求已发送到对方，但响应读取失败 =====
                    // 对方可能已落库，不可回滚，保留本地数据
                    log.error("开通代理 - 请求已发送但响应读取失败，对方可能已落库，保留本地数据，appOrderNo: {}", appOrderNo, e);
                    throw new NoRollbackBusinessException("开通代理请求已发送，但响应读取异常，请联系管理员确认订单结果，appOrderNo: " + appOrderNo, e);
                }
            }
            // 场景1：重试3次仍连接失败，可安全回滚
            if (sendException != null) {
                log.error("开通代理 - 重试3次后仍连接失败，回滚本地数据，appOrderNo: {}", appOrderNo, sendException);
                throw new BusinessException("开通代理请求失败，请稍后重试", sendException);
            }

            log.info("开通代理 - 响应: {}", resp);

            // ===== 场景4：sendPost返回空字符串，对方可能已落库，不可回滚 =====
            if (resp == null || resp.isEmpty()) {
                log.error("开通代理 - 响应为空，对方可能已落库，保留本地数据，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("开通代理响应为空，请联系管理员确认订单结果，appOrderNo: " + appOrderNo);
            }

            // 解析外层响应 JSON
            JsonNode root;
            try {
                root = objectMapper.readTree(resp);
            } catch (Exception e) {
                // 响应不是合法JSON，对方可能已落库，不可回滚
                log.error("开通代理 - 响应非法JSON，对方可能已落库，保留本地数据，appOrderNo: {}, resp: {}", appOrderNo, resp, e);
                throw new NoRollbackBusinessException("开通代理响应格式异常，请联系管理员确认订单结果，appOrderNo: " + appOrderNo, e);
            }

            // ===== 场景2/5：对方返回非200，业务处理失败，对方未落库，可回滚 =====
            int respCode = root.path("code").asInt(-1);
            if (respCode != 200) {
                String msg = root.path("msg").asText("");
                log.error("开通代理 - 对方返回业务失败, code={}, msg={}, appOrderNo: {}", respCode, msg, appOrderNo);
                throw new BusinessException("开通代理失败: " + msg);
            }

            // ===== 场景6：code=200，data节点缺失/null/空字符串，对方已落库，不可回滚 =====
            JsonNode dataJsonNode = root.path("data");
            if (dataJsonNode.isMissingNode() || dataJsonNode.isNull()) {
                log.error("开通代理 - code=200 但data节点缺失或为null，对方已落库，保留本地数据，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("开通代理响应data缺失，请联系管理员确认订单结果，appOrderNo: " + appOrderNo);
            }
            String encryptedData = dataJsonNode.asText();
            if (encryptedData.isEmpty()) {
                log.error("开通代理 - code=200 但data为空字符串，对方已落库，保留本地数据，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("开通代理响应data为空，请联系管理员确认订单结果，appOrderNo: " + appOrderNo);
            }

            // ===== 场景7：解密失败，对方已落库，不可回滚 =====
            String decryptedJson;
            try {
                decryptedJson = apiPacketUtil.unpack(encryptedData);
            } catch (Exception e) {
                log.error("开通代理 - 解密失败，对方已落库，保留本地数据，appOrderNo: {}", appOrderNo, e);
                throw new NoRollbackBusinessException("开通代理响应解密失败，请联系管理员确认订单结果，appOrderNo: " + appOrderNo, e);
            }
            log.info("开通代理接口返回数据解密成功: {}", decryptedJson);

            // ===== 场景8：解密成功，但JSON非法或orderNo/amount为空，对方已落库，不可回滚 =====
            try {
                JsonNode dataNode = objectMapper.readTree(decryptedJson);
                JsonNode orderNoNode = dataNode.path("orderNo");
                if (orderNoNode.isMissingNode() || orderNoNode.isNull() || orderNoNode.asText().isEmpty()) {
                    throw new IllegalStateException("未获取到orderNo");
                }
                orderNo = orderNoNode.asText();
                JsonNode amountNode = dataNode.path("amount");
                if (!amountNode.isMissingNode() && !amountNode.isNull() && !amountNode.asText().isEmpty()) {
                    amount = new java.math.BigDecimal(amountNode.asText());
                }
            } catch (Exception e) {
                log.error("开通代理 - 解密后数据解析失败或orderNo为空，对方已落库，保留本地数据，appOrderNo: {}, decryptedJson: {}", appOrderNo, decryptedJson, e);
                throw new NoRollbackBusinessException("开通代理响应数据解析失败，请联系管理员确认订单结果，appOrderNo: " + appOrderNo, e);
            }

            // 回写订单信息
            proxyOrderDAO.updateProxyPurchaseOrderByAppOrderNo(appOrderNo, orderNo, amount);
        } catch (NoRollbackBusinessException e) {
            // 对方已落库场景：不回滚本地数据，直接透传
            throw e;
        } catch (BusinessException e) {
            // 可回滚场景：@Transactional 自动回滚
            throw e;
        } catch (Exception e) {
            // 兜底：未预期异常，回滚数据
            throw new BusinessException("订单创建失败，请稍后重试", e);
        }
        ProxyPurchaseResultVO vo = new ProxyPurchaseResultVO();
        vo.setAppOrderNo(appOrderNo);
        vo.setStatus(1); // 待处理
        vo.setOrderNo(orderNo);
        vo.setAmount(amount);
        return vo;
    }

    @Override
    public ProxyOrderVO getOrderByAppOrderNo(String appOrderNo) {
        ProxyOrder order = proxyOrderDAO.selectByAppOrderNo(appOrderNo);
        if (order == null) {
            return null;
        }
        return convertToVO(order);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoRollbackBusinessException.class)
    public ProxyRenewResultVO renewProxies(List<ProxyRenewItemDTO> items) {
        Long userId = 2032958739262217115L;
        String appOrderNo = appOrderNoGenerator.generateRenewOrderId();
        log.info("成功生成appOrderNo，编号为：{}", appOrderNo);
        ProxyOrder order = new ProxyOrder();
        order.setAppOrderNo(appOrderNo);
        order.setUserId(userId);
        // 续费代理的订单类型就是2
        order.setOrderType(2);
        order.setStatus(1);
        order.setHasRefund(0);
        order.setTotalQuantity(items.size());
        order.setInstanceTotal(items.size());

        // 先存储主订单数据
        Long orderId = proxyOrderDAO.insertOrder(order);
        List<ProxyRenewOrderItem> orderItems = items.stream().map(itemDto -> {
            ProxyRenewOrderItem item = new ProxyRenewOrderItem();
            // 订单明细需要存储代理续费订单的id
            item.setOrderId(orderId);
            // 订单明细需要存储代理续费订单的渠道商订单号
            item.setAppOrderNo(appOrderNo);
            item.setInstanceNo(itemDto.getInstanceNo());
            item.setDuration(itemDto.getDuration());
            item.setUnit(itemDto.getUnit());
            item.setCycleTimes(itemDto.getCycleTimes());
            return item;
        }).collect(Collectors.toList());
        order.setRenewItems(orderItems);
        // 再存储订单明细数据
        proxyOrderDAO.insertProxyRenewOrderItems(order);

        // 构建第三方API业务参数
        Map<String, Object> bizParams = new java.util.HashMap<>();
        bizParams.put("appOrderNo", appOrderNo);
        List<Map<String, Object>> instanceList = new java.util.ArrayList<>();
        for (ProxyRenewItemDTO renewItem : items) {
            Map<String, Object> instMap = new java.util.HashMap<>();
            instMap.put("instanceNo", renewItem.getInstanceNo());
            instMap.put("duration", renewItem.getDuration());
            instMap.put("cycleTimes", renewItem.getCycleTimes());
            instanceList.add(instMap);
        }
        bizParams.put("instances", instanceList);

        try {
            // 加密打包请求参数
            Map<String, Object> req = apiPacketUtil.pack(bizParams);
            String url = baseUrl + instanceRenewPath;
            log.info("续费代理 - 请求URL: {}, 请求参数: {}", url, objectMapper.writeValueAsString(req));

            // 请求第三方接口失败，对方系统没有收到请求，重试3次，仍失败则回滚
            String resp = null;
            Exception sendException = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    resp = HttpClientUtil.sendPost(url, req, objectMapper);
                    sendException = null;
                    break;
                } catch (java.net.ConnectException | java.net.UnknownHostException e) {
                    // 场景1：连接建立失败，对方系统完全没有收到请求，可以安全重试
                    sendException = e;
                    log.warn("续费代理 - 第{}次请求连接失败: {}", attempt, e.getMessage());
                    if (attempt < 3) {
                        try {
                            Thread.sleep(1000L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    // 场景2：连接已建立，请求已发送到对方，但响应读取失败，对方可能已落库，不可回滚，保留本地数据
                    log.error("续费代理 - 请求已发送但响应读取失败，对方可能已落库，保留本地数据，appOrderNo: {}", appOrderNo, e);
                    throw new NoRollbackBusinessException("续费代理请求已发送，但响应读取异常，请联系管理员确认续费结果，appOrderNo: " + appOrderNo, e);
                }
            }
            // 场景3：重试3次仍连接失败，可安全回滚
            if (sendException != null) {
                log.error("续费代理 - 重试3次后仍连接失败，回滚本地数据，appOrderNo: {}", appOrderNo, sendException);
                throw new BusinessException("续费代理请求失败，请稍后重试", sendException);
            }

            log.info("续费代理 - 响应: {}", resp);

            // sendPost返回空字符串，对方可能已落库，不可回滚
            if (resp == null || resp.isEmpty()) {
                log.error("续费代理 - 响应为空，对方可能已落库，保留本地数据，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("续费代理响应为空，请联系管理员确认续费结果，appOrderNo: " + appOrderNo);
            }

            // 解析外层响应 JSON
            JsonNode root;
            try {
                root = objectMapper.readTree(resp);
            } catch (Exception e) {
                // 场景5：响应不是合法JSON，对方可能已落库，不可回滚
                log.error("续费代理 - 响应非法JSON，对方可能已落库，保留本地数据，appOrderNo: {}, resp: {}", appOrderNo, resp, e);
                throw new NoRollbackBusinessException("续费代理响应格式异常，请联系管理员确认续费结果，appOrderNo: " + appOrderNo, e);
            }

            // 场景6：对方返回非200，业务处理失败，对方未落库，可回滚
            int respCode = root.path("code").asInt(-1);
            if (respCode != 200) {
                String msg = root.path("msg").asText("");
                log.error("续费代理 - 对方返回业务失败, code={}, msg={}, appOrderNo: {}", respCode, msg, appOrderNo);
                throw new BusinessException("续费失败: " + msg);
            }

            // 场景7：code=200，data节点缺失/null/空字符串，对方已落库，不可回滚
            JsonNode dataJsonNode = root.path("data");
            if (dataJsonNode.isMissingNode() || dataJsonNode.isNull()) {
                log.error("续费代理 - code=200 但data节点缺失或为null，对方已落库，保留本地数据，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("续费代理响应data缺失，请联系管理员确认续费结果，appOrderNo: " + appOrderNo);
            }
            String encryptedData = dataJsonNode.asText();
            if (encryptedData.isEmpty()) {
                log.error("续费代理 - code=200 但data为空字符串，对方已落库，保留本地数据，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("续费代理响应data为空，请联系管理员确认续费结果，appOrderNo: " + appOrderNo);
            }

            // 场景8：解密失败，对方已落库，不可回滚
            String decryptedJson;
            try {
                decryptedJson = apiPacketUtil.unpack(encryptedData);
            } catch (Exception e) {
                log.error("续费代理 - 解密失败，对方已落库，保留本地数据，appOrderNo: {}", appOrderNo, e);
                throw new NoRollbackBusinessException("续费代理响应解密失败，请联系管理员确认续费结果，appOrderNo: " + appOrderNo, e);
            }
            log.info("续费代理接口返回数据解密成功: {}", decryptedJson);

            // 场景9：解密成功，但JSON非法或orderNo为空，对方已落库，不可回滚
            String orderNo;
            java.math.BigDecimal amount;
            try {
                JsonNode renewDataNode = objectMapper.readTree(decryptedJson);
                JsonNode orderNoNode = renewDataNode.path("orderNo");
                if (orderNoNode.isMissingNode() || orderNoNode.isNull() || orderNoNode.asText().isEmpty()) {
                    throw new IllegalStateException("未获取到orderNo");
                } else {
                    orderNo = orderNoNode.asText();
                }
                JsonNode amountNode = renewDataNode.path("amount");
                if (amountNode.isMissingNode() || amountNode.isNull() || amountNode.asText().isEmpty()) {
                    throw new IllegalStateException("未获取到amount");
                } else {
                    amount = new java.math.BigDecimal(amountNode.asText());
                }
            } catch (Exception e) {
                log.error("续费代理 - 解密后数据解析失败或orderNo为空，对方已落库，保留本地数据，appOrderNo: {}, decryptedJson: {}", appOrderNo, decryptedJson, e);
                throw new NoRollbackBusinessException("续费代理响应数据解析失败，请联系管理员确认续费结果，appOrderNo: " + appOrderNo, e);
            }

            // 更新主订单信息
            proxyOrderDAO.updateProxyRenewOrderByAppOrderNo(appOrderNo, orderNo, amount);
            // 构建返回VO
            ProxyRenewResultVO vo = new ProxyRenewResultVO();
            vo.setOrderNo(orderNo);
            vo.setAppOrderNo(appOrderNo);
            vo.setAmount(amount);
            vo.setStatus(1);
            return vo;
        } catch (Exception e) {
            // 兜底：未预期异常，回滚数据
            throw new BusinessException("续费代理失败，请稍后重试", e);
        }
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoRollbackBusinessException.class)
    public ProxyReleaseResultVO releaseProxies(ProxyReleaseDTO dto) {
        // 1. 校验支付密码
        PayPasswordVO payResult = payService.verifyPayPassword(dto.getPayPassword());
        if (!payResult.getPassed()) {
            throw new BusinessException(400, payResult.getMessage());
        }

        List<String> instanceNos = dto.getInstanceNos();
        Long userId = 2032958739262217115L;
        String appOrderNo = appOrderNoGenerator.generateReleaseOrderId();
        log.info("成功生成appOrderNo，编号为：{}", appOrderNo);
        ProxyOrder order = new ProxyOrder();
        order.setAppOrderNo(appOrderNo);
        order.setUserId(userId);
        order.setOrderType(3);
        order.setStatus(1);
        order.setHasRefund(0);
        order.setTotalQuantity(instanceNos.size());
        order.setInstanceTotal(instanceNos.size());

        Long orderId = proxyOrderDAO.insertOrder(order);
        List<ProxyReleaseOrderItem> orderItems = instanceNos.stream().map(instanceNo -> {
            ProxyReleaseOrderItem item = new ProxyReleaseOrderItem();
            item.setOrderId(orderId);
            item.setAppOrderNo(appOrderNo);
            item.setInstanceNo(instanceNo);
            return item;
        }).collect(Collectors.toList());
        order.setReleaseOrderItems(orderItems);
        proxyOrderDAO.insertProxyReleaseOrderItems(order);

        Map<String, Object> bizParams = new java.util.HashMap<>();
        bizParams.put("appOrderNo", appOrderNo);
        bizParams.put("instances", instanceNos);
        log.info("请求续费代理业务参数：{}", bizParams);

        try {
            Map<String, Object> req = apiPacketUtil.pack(bizParams);
            String url = baseUrl + instanceReleasePath;
            log.info("释放代理 - 请求URL: {}, 请求参数: {}", url, objectMapper.writeValueAsString(req));

            String resp = null;
            Exception sendException = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    resp = HttpClientUtil.sendPost(url, req, objectMapper);
                    sendException = null;
                    break;
                } catch (java.net.ConnectException | java.net.UnknownHostException e) {
                    sendException = e;
                    log.warn("释放代理 - 第{}次请求连接失败: {}", attempt, e.getMessage());
                    if (attempt < 3) {
                        try {
                            Thread.sleep(1000L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    log.error("释放代理 - 请求已发送但响应读取失败，appOrderNo: {}", appOrderNo, e);
                    throw new NoRollbackBusinessException("释放代理请求已发送，但响应读取异常，请联系管理员确认释放结果，appOrderNo: " + appOrderNo, e);
                }
            }
            if (sendException != null) {
                log.error("释放代理 - 重试3次后仍连接失败，回滚本地数据，appOrderNo: {}", appOrderNo, sendException);
                throw new BusinessException("释放代理请求失败，请稍后重试", sendException);
            }

            if (resp == null || resp.isEmpty()) {
                log.error("释放代理 - 响应为空，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("释放代理响应为空，请联系管理员确认释放结果，appOrderNo: " + appOrderNo);
            }

            JsonNode root;
            try {
                root = objectMapper.readTree(resp);
            } catch (Exception e) {
                log.error("释放代理 - 响应非法JSON，appOrderNo: {}, resp: {}", appOrderNo, resp, e);
                throw new NoRollbackBusinessException("释放代理响应格式异常，请联系管理员确认释放结果，appOrderNo: " + appOrderNo, e);
            }

            int respCode = root.path("code").asInt(-1);
            if (respCode != 200) {
                String msg = root.path("msg").asText("");
                log.error("释放代理 - 对方返回业务失败, code={}, msg={}, appOrderNo: {}", respCode, msg, appOrderNo);
                throw new BusinessException("释放失败: " + msg);
            }

            JsonNode dataJsonNode = root.path("data");
            if (dataJsonNode.isMissingNode() || dataJsonNode.isNull()) {
                log.error("释放代理 - data节点缺失，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("释放代理响应data缺失，请联系管理员确认释放结果，appOrderNo: " + appOrderNo);
            }
            String encryptedData = dataJsonNode.asText();
            if (encryptedData.isEmpty()) {
                log.error("释放代理 - data为空字符串，appOrderNo: {}", appOrderNo);
                throw new NoRollbackBusinessException("释放代理响应data为空，请联系管理员确认释放结果，appOrderNo: " + appOrderNo);
            }

            String decryptedJson;
            try {
                decryptedJson = apiPacketUtil.unpack(encryptedData);
            } catch (Exception e) {
                log.error("释放代理 - 解密失败，appOrderNo: {}", appOrderNo, e);
                throw new NoRollbackBusinessException("释放代理响应解密失败，请联系管理员确认释放结果，appOrderNo: " + appOrderNo, e);
            }
            log.info("释放代理接口返回数据解密成功: {}", decryptedJson);

            String orderNo;
            java.math.BigDecimal amount;
            try {
                JsonNode releaseDataNode = objectMapper.readTree(decryptedJson);
                JsonNode orderNoNode = releaseDataNode.path("orderNo");
                if (orderNoNode.isMissingNode() || orderNoNode.isNull() || orderNoNode.asText().isEmpty()) {
                    throw new IllegalStateException("未获取到orderNo");
                }
                orderNo = orderNoNode.asText();
                JsonNode amountNode = releaseDataNode.path("amount");
                if (amountNode.isMissingNode() || amountNode.isNull() || amountNode.asText().isEmpty()) {
                    throw new IllegalStateException("未获取到amount");
                } else {
                    amount = new java.math.BigDecimal(amountNode.asText());
                }
            } catch (Exception e) {
                log.error("释放代理 - 解密后数据解析失败，appOrderNo: {}, decryptedJson: {}", appOrderNo, decryptedJson, e);
                throw new NoRollbackBusinessException("释放代理响应数据解析失败，请联系管理员确认释放结果，appOrderNo: " + appOrderNo, e);
            }

            proxyOrderDAO.updateProxyReleaseOrderByAppOrderNo(appOrderNo, orderNo, amount);

            ProxyReleaseResultVO vo = new ProxyReleaseResultVO();
            vo.setOrderNo(orderNo);
            vo.setAppOrderNo(appOrderNo);
            vo.setAmount(amount);
            vo.setStatus(1);
            return vo;
        } catch (NoRollbackBusinessException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("释放代理失败，请稍后重试", e);
        }
    }

}
