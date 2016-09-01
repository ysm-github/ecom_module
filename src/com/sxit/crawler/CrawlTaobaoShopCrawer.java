package com.sxit.crawler;


import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.econ.taobaoshop.TaobaoShopItemCrawlerModule;

public class CrawlTaobaoShopCrawer {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
//		System.setProperty(SystemConstant.APP_HOME_KEY, "E:\\lex\\workspace\\crawler\\ecom_module\\data");
//		System.setProperty(SystemConstant.MODULE_HOME_KEY, "E:\\lex\\workspace\\crawler\\ecom_module\\data");
		TaobaoShopItemCrawlerModule crawlerModule = new TaobaoShopItemCrawlerModule();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}
}
