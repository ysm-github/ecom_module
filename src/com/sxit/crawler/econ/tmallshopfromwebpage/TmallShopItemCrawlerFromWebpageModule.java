package com.sxit.crawler.econ.tmallshopfromwebpage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.econ.tmallshopfromwebpage.process.TmallItemDataProcessFromWebpage;
import com.sxit.crawler.econ.tmallshopfromwebpage.process.TmallShopItemCrawlerFromWebpageProcess;
import com.sxit.crawler.module.CrawlModule;


/**
 * <strong>TaobaoShopItemCrawlerModule</strong><br>
 * <p>
 * 淘宝店铺商品抓取模块
 * 1、通过tbas_shop_taobao表获取所有手机类型的淘宝店铺
 * 2、（获取宝贝ID和店铺关联信息）针对每个淘宝店铺进入店铺搜索页面，将搜索页面所有的宝贝ID（宝贝ID、店铺ID、商家昵称）提取出来，并记录至TBAS_SHOP_TAOBAL_ITEMIDS表
 * 3、（此块完善店铺信息）针对每个搜索结果页面，提取店铺的基本信息（店铺ID、用户ID、用户昵称、店铺个性化URL、店铺标准URL）并记录至TBAS_SHOP_TAOBAO_IDREL
 * 4、针对TBAS_SHOP_TAOBAL_ITEMIDS表中的数据，调用TAOBAO API接口获取详细的宝贝数据
 * 
 * </p>
 * @since 0.1
 * @version $Id: TaobaoShopItemCrawlerModule.java,v 0.1 2013-10-16 下午3:04:01 lex Exp $
 */
public class TmallShopItemCrawlerFromWebpageModule extends CrawlModule{
	private static Logger log = LoggerFactory.getLogger(TmallShopItemCrawlerFromWebpageModule.class);
	private final static String DEFAULT_JOB_NAME = TmallShopItemCrawlerFromWebpageModule.class.getSimpleName();
	
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	
	public final static String PARAM_KEY_SHOP_HREF = "shop.href";//店铺地址

	public DatatableConfig tmallIdRelDatatableOperatorConfig = null;
	public DatatableConfig tmallItemIdsDatatableOperatorConfig = null;
	public DatatableConfig tmallItemDatatableConfig = null;
	/**
	 * 加载淘宝店铺地址
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<String> loadShopHrefs() {
		String sql = "select alis_href from tbas_shop_tmall";
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		List<String> hrefs = (List<String>)jdbcTemplate.query(sql, new RowMapper() {
			@Override
			public Object mapRow(ResultSet rs, int idx) throws SQLException {
				String href = rs.getString(1);
				return href;
			}
		});
		return hrefs;
	}
	
	@Override
	public void execute() {
		//数据表配置
		tmallIdRelDatatableOperatorConfig = initDatatableConfig("shoptmallidrel_datatable_config.xml");
		tmallItemIdsDatatableOperatorConfig = initDatatableConfig("shoptmallitemids_datatable_config.xml");
		tmallItemDatatableConfig= initDatatableConfig("datatrans_tmall_datatable_fromwebpage_config.xml");
		List<String> shopHrefs = loadShopHrefs();
		if (CollectionUtils.isEmpty(shopHrefs)) {
			log.warn("没有店铺URL地址");
		}
		
		//获取店铺链接
		for (String shopHref : shopHrefs) {
			try {
				Map<String, Object> param = new HashMap<String, Object>();
				param.put(PARAM_KEY_SHOP_HREF, shopHref);
				TmallShopItemCrawlerFromWebpageProcess TmallShopItemCrawlerProcess = 
						new TmallShopItemCrawlerFromWebpageProcess(USER_AGENT_PROVIDER, this, crawlConfig);
				
				//抓取店铺基本信息、以及各个店铺的宝贝ID信息
				TmallShopItemCrawlerProcess.process(param);
			} catch (Exception e) {
				log.error("店铺数据采集错误："+shopHref, e);
			}
		}
		log.info("开始采集商品详细信息。");
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		TmallItemDataProcessFromWebpage tmallItemDataProcess = new TmallItemDataProcessFromWebpage(USER_AGENT_PROVIDER, this, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		tmallItemDataProcess.process(param);
		endTime = System.currentTimeMillis();
		log.info("商品信息采集完成，耗时:{}", (endTime-startTime));
	}
}
