import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.utils.CrawlerUtils;
import com.sxit.crawler.utils.JsoupUtils;
import com.sxit.crawler.utils.TextUtils;



/**
 * <strong>CrawlTmallShop</strong><br>
 * <p>
 * 搜索天猫店铺（通过“”“”“”“”“”“”“”“天猫的”“”“”“”“”“”“”“”搜索接口）
 * </p>
 * @since 0.1
 * @version $Id: CrawlTmallShop.java,v 0.1 2013-10-15 下午6:10:45 lex Exp $
 */
public class CrawlTmallShop2 {
	static DatatableConfig datatableConfig = null;
	static DatatableOperator datatableOperator = null;
	
	public static void main(String[] args) {
		//数据表配置
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		
		//数据表配置
		datatableConfig = DatatableConfig.createDatatableConfigByXml("shoptmall_datatable_config.xml");
		datatableConfig.buildConfig();
		

		datatableOperator = new DatatableOperator(datatableConfig, (JdbcTemplate)BeanHelper.getBean("jdbcTemplate"));
		
		int pageSize = 34;
		for (int i = 0; i < pageSize; i++) {
			String url = "http://list.tmall.com//search_product.htm?q=%CA%D6%BB%FA+&type=p&style=w&xl=%CA%D6%BB%FA+_2&from=xl_2_suggest";
			url += "&s="+(i*20);
			System.out.println(url);
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry = fetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				processPage(fetchEntry);
			}
		}
	}
	
	private static String extrShopId(String url) {
		FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
		FetchHTTP fetchHTTP = new FetchHTTP();
		fetchEntry = fetchHTTP.process(fetchEntry);
		if (null != fetchEntry 
				&& fetchEntry.getResult() != null 
				&& fetchEntry.getResult().getPageContent() != null) {
			String htmlContent = fetchEntry.getResult().getPageContent().toString();
			//通过正则提取ID列表
			String shopId = TextUtils.extrValueByRegx("\"shopId\"[\\s\\S]*?:[\\s\\S]*?\"([\\d]*?)\"", htmlContent, 1);
			return shopId;
		}
		return null;
	}
	
	private static void processPage(FetchEntry fetchEntry) {
		Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent().toString());
		Elements shopElts = doc.select("div#J_ItemList>div.j_ShopBox");
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		if (!CollectionUtils.isEmpty(shopElts)) {
			System.out.println("SHOP SIZE:"+shopElts.size());
			for (Element shopElt : shopElts) {
				String shopEltHtml = shopElt.html();
				Map<String, Object> row = new HashMap<String, Object>();
				String shopHref = JsoupUtils.extrFirstAttr(shopElt, "div.shopHeader>div.shopHeader-enter>a", "href");
				String stdShopHref = CrawlerUtils.extrRedirectHref(FetchEntityBuilder.buildFetchEntry(shopHref));
				String alisHref = null;
				if (StringUtils.startsWith(stdShopHref, "http://shop")) {
					//返回保准的店铺URL
					alisHref = CrawlerUtils.extrRedirectHref(FetchEntityBuilder.buildFetchEntry(stdShopHref));
				} else {
					//返回自定义域名的店铺URL
					alisHref = stdShopHref;
					if (!StringUtils.endsWith(stdShopHref, "/shop/viewShop.htm")) {
						stdShopHref =  stdShopHref + "/shop/viewShop.htm";
					}
					String shopId = extrShopId(stdShopHref);
					if (StringUtils.isBlank(shopId)) {
						continue;
					}
					stdShopHref = "http://shop"+shopId+"taobao.com";
				}
				String userid = TextUtils.extrValueByRegx("[\\S]*?user_number_id=([\\d]*)[\\S]*", shopHref);
				//http://store.taobao.com/shop/view_shop.htm?user_number_id=901409638&rn=c580cc64c644dd7d16c302aa328a5829
				String shopId = TextUtils.substringBetweenAndRemove(stdShopHref, "http://shop", ".", "");
				stdShopHref = StringUtils.substring(stdShopHref, 0, stdShopHref.indexOf("/", 9));
				alisHref = StringUtils.substring(alisHref, 0, alisHref.indexOf("/", 9));
				if (StringUtils.isNotBlank(shopId)) {
					String shopName = JsoupUtils.extrFirstText(shopElt, "div.shopHeader>div.shopHeader-info>a");
					String city =  TextUtils.substringBetweenAndRemove(shopEltHtml, "<p>所在地：", "</p>", "");
					row.put("SHOPID", shopId);
					row.put("SHOPHREF", stdShopHref);
					row.put("SHOPNAME", shopName);
					row.put("KEYWORD", "");
					row.put("NICK", "");
					row.put("WANGWANG", "");
					row.put("CITY", city);
					row.put("SELSENUM", "");
					row.put("ONSELSNUM", "");
					row.put("ALIS_HREF", alisHref);
					row.put("USER_ID", userid);
					list.add(row);
				} else {
					System.out.println("shopHref:"+shopHref);
					System.out.println(shopElt.text());
					System.out.println(shopElt.html());
					System.out.println(shopElt.tagName());
					System.out.println((null != shopElt.select("textarea.ks-datalazyload")));
					System.out.println("------------------------------------------------");
				}
			}
		}
		datatableOperator.saveData(list, true);
		
	}
}
