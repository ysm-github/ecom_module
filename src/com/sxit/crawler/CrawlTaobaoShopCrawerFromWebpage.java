package com.sxit.crawler;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.econ.taobaoshopfromwebpage.TaobaoShopItemCrawlerFromWebpageModule;

public class CrawlTaobaoShopCrawerFromWebpage {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		TaobaoShopItemCrawlerFromWebpageModule crawlerModule = new TaobaoShopItemCrawlerFromWebpageModule();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}
}
