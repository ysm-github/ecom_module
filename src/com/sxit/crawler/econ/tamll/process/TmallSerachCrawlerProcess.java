package com.sxit.crawler.econ.tamll.process;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.CrawlJob;
import com.sxit.crawler.core.fetch.DefaultFetchExecutorBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.econ.tamll.TmallSearchCrawlModule;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.sxit.crawler.utils.JsoupUtils;
import com.sxit.crawler.utils.Md5Utils;
import com.sxit.crawler.utils.TextUtils;
import com.sxit.crawler.utils.UrlUtils;

public class TmallSerachCrawlerProcess extends CrawlProcess{
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private Map<String, String> catMap;//分类数据；
	
	private DetailDataProcess detailDataProcess;

	public TmallSerachCrawlerProcess(
			UserAgentProvider userAgentProvider, CrawlModule crawlModule,
			CrawlConfig crawlConfig, DatatableConfig datatableConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		detailDataProcess = new DetailDataProcess();
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
					String htmlContent = fetchEntry.getResult().getPageContent().toString();
					
					String str = TextUtils.extrValueByRegx("没有找到(.*)相关的商品哦", htmlContent);//判断是否有结果结合
					if (StringUtils.isNotBlank(str)) {
						//存在 "没有找到(.*)相关的商品哦 提示字符串"，则表示没有搜索到商品
						return;
					}
					
					Document doc = parseHtmlContent(fetchEntry);
					if (null == doc) {
						return;
					}
					Elements prods = doc.select("div.product");
					if (CollectionUtils.isEmpty(prods)) {
						return;
					}
					
					for (Element prod : prods) {
						Element titleElt = JsoupUtils.extrFirstElt(prod, "div.product>div.product-iWrap>p.productTitle>a");
						String url = JsoupUtils.extrAttr(titleElt, "href");
						String catId = TextUtils.substringBetweenAndRemove(url, "cat_id=", "&", "");
						if (!catMap.containsKey(catId)) {
							return;
						}
						
						String id = prod.attr("data-id");
						String price = JsoupUtils.extrFirstHtml(prod, "div.product>div.product-iWrap>p.productPrice>em");
						String title = JsoupUtils.extrText(titleElt);
						String brandName = StringUtils.substringBefore(title, " ");
						String model = StringUtils.substringAfter(title, " ");
						
						String urlMd5 = Md5Utils.getMD5(url);
						String catName = catMap.get(catId);
						
						Map<String, Object> dataMap = new HashMap<String, Object>();
						dataMap.put("SYSURLMD5", urlMd5);
						dataMap.put("SRCCLASSNAME", catName);
						dataMap.put("SRCCLASSID", catName);
						dataMap.put("URL", url);
						
						
						dataMap.put("GOODSID", id);
						dataMap.put("GOODSBRAND", brandName);
						dataMap.put("GOODSMODEL", model);
						dataMap.put("GOODSPRICE", price);
						dataMap.put("TITLE", title);
						
						dataMap = detailDataProcess.process(fetchEntry, dataMap); 
						//保存数据，并且检查重复
						crawlModule.getDatatableOperator(dataMap).saveColumnData(dataMap, true);
					}
				}
			};
		}
	}
	
	private final static String SERACHE_URL = "http://list.tmall.com/search_product.htm?cat=#cat#&q=#keyword#&sort=pt&style=g";
	@Override
	public void process(Map<String, Object> param) {
		@SuppressWarnings("unchecked")
		Map<String, String> catMap = (Map<String, String>)param.get(TmallSearchCrawlModule.PARAM_KEY_CAT_MAPS);
		this.catMap = catMap;
		
		if (CollectionUtils.isEmpty(catMap)) {
			log.info("类目信息是空，退出当前抓取");
			return;
		}
		
		String keyword = (String)param.get(TmallSearchCrawlModule.PARAM_KEY_KEYWORD);
		if (StringUtils.isBlank(keyword)) {
			log.info("搜索关键字为空，退出当前抓取");
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
		
		if (!CollectionUtils.isEmpty(catMap)) {
			for (String id : catMap.keySet()) {
				log.info("开始依据商品类目【{}】和关键字【{}】搜索....", id, keyword);
				if (StringUtils.isBlank(id)) 
					continue;
				
				try {
					String url = SERACHE_URL.replaceAll("#cat#", id);
					url = url.replaceAll("#keyword#", UrlUtils.encodeUrl(keyword, "GB2312"));
					crawlData(crawlJob, url);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		crawlJob.waitJobExit();
	}
	
	//依据分页执行采集
	private void crawlData(CrawlJob crawlJob, String url) {
		int pageSize = getPageSize(url);
		if (pageSize < 0) {
			log.info("没有结果");
			return;
		}
		System.out.println("=======================================");
		System.out.println("=======================================");
		System.out.println("=======================================");
		System.out.println("=======================================");
		System.out.println("=======================================");
		log.info("一共需要采集【{}】页", pageSize);
		System.out.println("=======================================");
		System.out.println("=======================================");
		System.out.println("=======================================");
		System.out.println("=======================================");
		System.out.println("=======================================");
		
		int nums = 84;//每页记录条数
		for (int i=0; i<=pageSize; i++) {
			String crawlUrl = url+"&s="+(i*nums);
			crawlJob.submitUrl(crawlUrl, userAgentProvider);
		}
	}
	
	private int getPageSize(String url) {
		TmallPageSizeCrwalProcess tmallPageSizeCrwalProcess = new TmallPageSizeCrwalProcess(userAgentProvider, crawlModule, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put(TmallSearchCrawlModule.PARAM_KEY_URL, url);
		tmallPageSizeCrwalProcess.process(param);
		return tmallPageSizeCrwalProcess.getPageSize();
	}
	
}
