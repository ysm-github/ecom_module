package com.sxit.crawler.econ.tmallshop.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.CrawlJob;
import com.sxit.crawler.core.fetch.DefaultFetchExecutorBuilder;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.econ.taobaoshop.TaobaoShopItemCrawlerModule;
import com.sxit.crawler.econ.tmallshop.TmallShopItemCrawlerModule;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.sxit.crawler.utils.JsoupUtils;
import com.sxit.crawler.utils.TextUtils;
import com.sxit.crawler.utils.UrlUtils;


/**
 * <strong>TaobaoShopItemCrawlerProcess</strong><br>
 * <p>
 * 抓取淘宝店铺的宝贝列表数据
 * </p>
 * @since 0.1
 * @version $Id: TaobaoShopItemCrawlerProcess.java,v 0.1 2013-10-16 下午3:05:43 lex Exp $
 */
public class TmallShopItemCrawlerProcess extends CrawlProcess{

	private static Logger log = LoggerFactory.getLogger(TmallShopItemCrawlerProcess.class);
	private DatatableConfig taobaoIdRelDatatableOperatorConfig;
	private DatatableConfig taobaoItemIdsDatatableOperatorConfig;
	public TmallShopItemCrawlerProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		taobaoIdRelDatatableOperatorConfig = ((TmallShopItemCrawlerModule)crawlModule).tmallIdRelDatatableOperatorConfig;
		taobaoItemIdsDatatableOperatorConfig = ((TmallShopItemCrawlerModule)crawlModule).tmallItemIdsDatatableOperatorConfig;
	}
	
	private final class MyResultExecutorBuilder implements ResultExecutorBuilder {
		@Override
		public ResultExecutor buildResultExecutor(
				BlockingQueue<FetchEntry> resultQueue, FetchEntry fetchEntry) {
			return new ResultExecutor(resultQueue, fetchEntry) {
				@Override
				public void processResult() {
					if (!verifyRequired()) {
						return;
					}
					try {
						processShopInfo(fetchEntry);//处理店铺数据
					} catch (Exception e) {
						log.error("店铺数据入库错误："+fetchEntry.getUrl(), e);
					}
					String htmlContent = fetchEntry.getResult().getPageContent().toString();
					//通过正则提取ID列表
					String shopId = TextUtils.extrValueByRegx("\"shopId\"[\\s\\S]*?:[\\s\\S]*?\"([\\d]*?)\"", htmlContent, 1);
					String userNick = TextUtils.extrValueByRegx("\"user_nick\"[\\s\\S]*?:[\\s\\S]*?\"([\\S]*?)\"", htmlContent, 1);
					if (StringUtils.isNotBlank(userNick)) {
						userNick = UrlUtils.decodeUrl(userNick, "UTF-8");
					}
					Document doc = parseHtmlContent(fetchEntry);
					Elements hrefElts = doc.select("a[href^=http://detail.tmall.com/item.htm?]");
					if (CollectionUtils.isEmpty(hrefElts)) {
						log.warn("没有商品-->{}", fetchEntry.getUrl());
					}
					Set<String> ids = new LinkedHashSet<String>();//商品ID   去重复用
					for (Element hrefElt : hrefElts) {
						String href = JsoupUtils.extrAttr(hrefElt, "href");
						String id = TextUtils.extrValueByRegx("id=([\\d]*)", href, 1);
						if (StringUtils.isNotBlank(id)) {
							ids.add(id);
						}
					}
					List<Map<String, Object>> rows = new ArrayList<Map<String,Object>>();
					System.out.println("url:"+fetchEntry.getUrl());
					System.out.println("ids:"+ids.size());
					for (String itemId : ids) {
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("ITEM_ID", itemId);
						row.put("SHOP_ID", shopId);
						row.put("USER_NICK", userNick);
						rows.add(row);
					}
					
					if (!CollectionUtils.isEmpty(rows)) {
						JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
						DatatableOperator datatableOperator = new DatatableOperator(taobaoItemIdsDatatableOperatorConfig, jdbcTemplate);
						datatableOperator.saveData(rows, true);
					}
				}
			};
		}
	}

	@Override
	public void process(Map<String, Object> param) {
		if (CollectionUtils.isEmpty(param)) {
			log.warn("参数为空");
			return;
		}
		String taobaoShopHref = (String)param.get(TaobaoShopItemCrawlerModule.PARAM_KEY_SHOP_HREF);
		if (StringUtils.isBlank(taobaoShopHref)) {
			log.warn("店铺连接为空");
		}
		String pageParam = "&pageNo=";
		//search.htm?search=y
		String extParam = "/shop/viewShop.htm?type=p&style=&cat=all&search=y&newHeader_b=s_from&searcy_type=item";
		String url = taobaoShopHref+"/"+extParam;
		int pageNum = getPageNum(url);//获取分页数量
		if (pageNum <= 0) {
			log.warn("没有数据-->{}", url);
			return;
		}
		
		
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setFetchQueueLength(2048);
		config.setResultQueueLength(2048);
		config.setFetchThreadPoolSize(10);
		config.setResultThreadPoolSize(5);
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		
		for (int i = 1; i<=pageNum; i++) {
			String curl = url + pageParam + i;
			log.info("开始采集店铺数据, 第{}页, url:{}", i, curl);
			crawlJob.submitUrl(curl, userAgentProvider);
			
		}
		crawlJob.waitJobExit();
	}

	
	/**
	 * 处理店铺信息 
	 * 
	 * */
	private void processShopInfo(FetchEntry fetchEntry) {
		String str = fetchEntry.getResult().getPageContent().toString();
		String shopId = TextUtils.extrValueByRegx("\"shopId\"[\\s\\S]*?:[\\s\\S]*?\"([\\d]*?)\"", str, 1);
		String userId = TextUtils.extrValueByRegx("\"userId\"[\\s\\S]*?:[\\s\\S]*?\"([\\d]*?)\"", str, 1);
		String userNick = TextUtils.extrValueByRegx("\"user_nick\"[\\s\\S]*?:[\\s\\S]*?\"([\\S]*?)\"", str, 1);
		String alisHref = fetchEntry.getBaseUrl();
		if (StringUtils.isNotBlank(userNick)) {
			userNick = UrlUtils.decodeUrl(userNick, "UTF-8");
		}
		if (StringUtils.isBlank(shopId)) {
			return;
		}
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("SHOP_ID", shopId);
		row.put("USER_ID", userId);
		row.put("USER_NICK", userNick);
		row.put("ALIS_HREF", alisHref);
		row.put("HREF", "http://shop"+shopId+".taobao.com");
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		DatatableOperator datatableOperator = new DatatableOperator(taobaoIdRelDatatableOperatorConfig, jdbcTemplate);
		datatableOperator.saveData(row, true);
	}
	
	private static int getPageNum(String url) {
		FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
		FetchHTTP fetchHTTP = new FetchHTTP();
		fetchEntry = fetchHTTP.process(fetchEntry);
		if (fetchEntry.getResult() != null && null != fetchEntry.getResult().getPageContent()) {
			//<span class="page-info">2/2</span>
			//TextUtils.extrValueByRegx("<span[\\s\\S]*?class=\"page-info\"[\\s\\S]*?>.*/([\\d]*?)</span>", str, 1);
			String html = fetchEntry.getResult().getPageContent().toString();
			return TextUtils.extrNumber(
					TextUtils.extrValueByRegx(
							"<b[\\s\\S]*?class=\"ui-page-s-len\"[\\s\\S]*?>.*/([\\d]*?)</b>", 
							html, 
							1)
					);
		}
		return -1;
	}
	
}
