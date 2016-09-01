package com.sxit.crawler;

public class TaobaoPhoneFromWebpage {

	/**
	 * @淘宝手机四合一抓取
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		//第一步：抓店铺
		CrawlTaobaoShop crawlTaobaoShop=new CrawlTaobaoShop();
		crawlTaobaoShop.main(args);
		
		//第二步：抓items和item
		CrawlTaobaoShopCrawerFromWebpage crawlTaobaoShopCrawer=new CrawlTaobaoShopCrawerFromWebpage();
		crawlTaobaoShopCrawer.main(args);
		
	}

}
