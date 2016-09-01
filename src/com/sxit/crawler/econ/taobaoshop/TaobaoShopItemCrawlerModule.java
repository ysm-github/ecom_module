package com.sxit.crawler.econ.taobaoshop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.sun.swing.internal.plaf.synth.resources.synth;
import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.econ.taobaoshop.process.TaobaoItemDataProcess;
import com.sxit.crawler.econ.taobaoshop.process.TaobaoShopItemCrawlerProcess;
import com.sxit.crawler.module.CrawlModule;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.domain.Item;
import com.taobao.api.domain.Location;
import com.taobao.api.request.ItemsListGetRequest;
import com.taobao.api.response.ItemsListGetResponse;


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
public class TaobaoShopItemCrawlerModule extends CrawlModule{
	private static Logger log = LoggerFactory.getLogger(TaobaoShopItemCrawlerModule.class);
	private final static String DEFAULT_JOB_NAME = TaobaoShopItemCrawlerModule.class.getSimpleName();
	
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	
	public final static String PARAM_KEY_SHOP_HREF = "shop.href";//淘宝店铺地址

	public DatatableConfig taobaoIdRelDatatableOperatorConfig = null;
	public DatatableConfig taobaoItemIdsDatatableOperatorConfig = null;
	public DatatableConfig taobaoItemDatatableConfig = null;

	/**
	 * 加载淘宝店铺地址
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<String> loadShopHrefs() {
		String sql = "select shop_href from tbas_shop_taobao";
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
		taobaoIdRelDatatableOperatorConfig = initDatatableConfig("shoptaobaoidrel_datatable_config.xml");
		taobaoItemIdsDatatableOperatorConfig = initDatatableConfig("shoptaobaoitemids_datatable_config.xml");
		taobaoItemDatatableConfig = initDatatableConfig("shoptaobaoitem_datatable_config.xml");
		List<String> shopHrefs = loadShopHrefs();
		if (CollectionUtils.isEmpty(shopHrefs)) {
			log.warn("没有店铺URL地址");
		}
		
		//获取店铺链接
		for (String shopHref : shopHrefs) {
			try {
				Map<String, Object> param = new HashMap<String, Object>();
				param.put(PARAM_KEY_SHOP_HREF, shopHref);
				TaobaoShopItemCrawlerProcess taobaoShopItemCrawlerProcess = 
						new TaobaoShopItemCrawlerProcess(USER_AGENT_PROVIDER, this, crawlConfig);
				
				//抓取店铺基本信息、以及各个店铺的宝贝ID信息
				taobaoShopItemCrawlerProcess.process(param);
			} catch (Exception e) {
				log.error("店铺数据采集错误："+shopHref, e);
			}
		}
		log.info("开始采集商品详细信息。");
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		TaobaoItemDataProcess taobaoItemDataProcess = new TaobaoItemDataProcess(USER_AGENT_PROVIDER, this, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		taobaoItemDataProcess.process(param);
		endTime = System.currentTimeMillis();
		log.info("商品信息采集完成，耗时", (endTime-startTime));
	}
}
