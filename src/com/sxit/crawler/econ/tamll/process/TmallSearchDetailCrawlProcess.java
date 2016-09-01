package com.sxit.crawler.econ.tamll.process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.fetch.DefaultFetchExecutorBuilder;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;

public class TmallSearchDetailCrawlProcess extends CrawlProcess{

	private final ConcurrentHashMap<String, String> idMaps;
	private final ConcurrentHashMap<String, Map<String, String>> idDataMap;
	
	public TmallSearchDetailCrawlProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig, ConcurrentHashMap<String, String> idMaps,
			ConcurrentHashMap<String, Map<String, String>> idDataMap) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
//		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		this.idMaps = idMaps;
		this.idDataMap = idDataMap;
	}
	
//	private final class MyResultExecutorBuilder implements ResultExecutorBuilder {
//		@Override
//		public ResultExecutor buildResultExecutor(
//				BlockingQueue<FetchEntry> resultQueue, FetchEntry fetchEntry) {
//			return new ResultExecutor(resultQueue, fetchEntry) {
//				@Override
//				public void processResult() {
//					if (!verifyRequired()) {
//						return;
//					}
//					Document doc = parseHtmlContent(fetchEntry);
//					if (null == doc) {
//						return;
//					}
//					String title = JsoupUtils.extrFirstAttr(doc, "div#J_DetailMeta>div.tb-property>div.tb-wrap>div.tb-detail-hd>h3>a", "href");
//					Elements prods = doc.select("div.product");
//					if (CollectionUtils.isEmpty(prods)) {
//						return;
//					}
//					
//					for (Element prod : prods) {
//						String id = prod.attr("data-id");
//						String url = JsoupUtils.extrFirstAttr(prod, "div.productMain>a.productImg", "href");
//						idMaps.put(id, url);
//					}
//				}
//			};
//		}
//	}
//
	@Override
	public void process(Map<String, Object> param) {
		// TODO Auto-generated method stub
		
	}

}
