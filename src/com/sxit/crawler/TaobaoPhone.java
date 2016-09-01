package com.sxit.crawler;

public class TaobaoPhone {

	/**
	 * @淘宝手机四合一抓取
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		//第一步：抓店铺
		CrawlTaobaoShop crawlTaobaoShop=new CrawlTaobaoShop();
		crawlTaobaoShop.main(args);
		//第一步：抓items和item
		CrawlTaobaoShopCrawer crawlTaobaoShopCrawer=new CrawlTaobaoShopCrawer();
		crawlTaobaoShopCrawer.main(args);
		//第三步：汇总数据
		DataTransTaobao dataTransTaobao=new DataTransTaobao();
		dataTransTaobao.main(args);
	}

}
