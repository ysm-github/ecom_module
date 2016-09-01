package com.sxit.crawler.econ.tamll.process;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;

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
import com.sxit.crawler.utils.TextUtils;

public class TmallPageSizeCrwalProcess extends CrawlProcess{

	private int pageSize = 0;
	public TmallPageSizeCrwalProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
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
						pageSize = -1;
						return;
					}
					
					Document doc = parseHtmlContent(fetchEntry);
					if (null == doc) {
						return;
					}
					String totalPageStr = JsoupUtils.extrFirstAttr(doc, "input[name=totalPage]", "value");
					if (StringUtils.isNotBlank(totalPageStr)) {
						try {
							pageSize = Integer.parseInt(totalPageStr);
						} catch (Exception e) {
						}
					}
				}
			};
		}
	}

	@Override
	public void process(Map<String, Object> param) {
		String url = (String)param.get(TmallSearchCrawlModule.PARAM_KEY_URL);
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setFetchQueueLength(1);
		config.setResultQueueLength(1);
		config.setFetchThreadPoolSize(1);
		config.setResultThreadPoolSize(1);
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		crawlJob.submitUrl(url, userAgentProvider);
		crawlJob.waitJobExit();
	}

	public int getPageSize() {
		return pageSize;
	}

}
