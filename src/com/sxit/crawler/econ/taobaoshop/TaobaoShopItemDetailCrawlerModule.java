package com.sxit.crawler.econ.taobaoshop;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.econ.taobaoshop.process.TaobaoItemDataProcess;


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
public class TaobaoShopItemDetailCrawlerModule extends TaobaoShopItemCrawlerModule{
	private static Logger log = LoggerFactory.getLogger(TaobaoShopItemDetailCrawlerModule.class);
	private final static String DEFAULT_JOB_NAME = TaobaoShopItemDetailCrawlerModule.class.getSimpleName();
	
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	
	public final static String PARAM_KEY_SHOP_HREF = "shop.href";//淘宝店铺地址

	
	@Override
	public void execute() {
		//数据表配置
		taobaoItemDatatableConfig = initDatatableConfig("shoptaobaoitem_datatable_config.xml");
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
