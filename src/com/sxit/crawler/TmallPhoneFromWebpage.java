package com.sxit.crawler;

public class TmallPhoneFromWebpage {

	/**
	 * @天猫手机四合一抓取
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		//第一步：抓店铺
		CrawlTmallShop2 crawlTmallShop=new CrawlTmallShop2();
		crawlTmallShop.main(args);
		//第一步：抓items和item
		CrawlTmallShopCrawerFromWebpage crawlTmallShopCrawer=new CrawlTmallShopCrawerFromWebpage();
		crawlTmallShopCrawer.main(args);
		
	}

}
