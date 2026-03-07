package cn.linkfast.service;

/**
 * @author liaowenxiong
 * @version 1.0
 * @description TODO
 * @since 2026/3/7 14:06
 */
@SpringBootTest
class ProxyProductServiceTest {
    @Autowired
    private ProxyProductService productService;

    @Test
    void testGetProxyProducts() throws Exception {
        ProxyProductQueryDTO dto = new ProxyProductQueryDTO();
        dto.setCountryCode("US");
        dto.setCityCode("NY");
        dto.setPage(1);
        dto.setPageSize(10);

        PageResult<ProxyProductVO> result = productService.getProxyProducts(dto);
        assertNotNull(result);
        System.out.println("查询到数据条数: " + result.getTotal());
    }
}