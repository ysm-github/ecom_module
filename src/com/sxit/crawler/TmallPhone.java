package com.sxit.crawler;

public class TmallPhone {

	/**
	 * @天猫手机四合一抓取
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		//第一步：抓店铺
		CrawlTmallShop2 crawlTmallShop=new CrawlTmallShop2();
		crawlTmallShop.main(args);
		//第一步：抓items和item
		CrawlTmallShopCrawer crawlTmallShopCrawer=new CrawlTmallShopCrawer();
		crawlTmallShopCrawer.main(args);
		//第三步：汇总数据
		DataTransTmall dataTransTmall=new DataTransTmall();
		dataTransTmall.main(args);
	}

}
