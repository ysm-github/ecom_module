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
import com.sxit.crawler.utils.JsoupUtils;
import com.sxit.crawler.utils.TextUtils;



/**
 * <strong>CrawlTmallShop</strong><br>
 * <p>
 * 搜索天猫店铺（通过“”“”“”“”“”“”“”淘宝的“”“”“”“”“”“”“”“”搜索接口）
 * </p>
 * @since 0.1
 * @version $Id: CrawlTmallShop.java,v 0.1 2013-10-15 下午6:10:45 lex Exp $
 */
public class CrawlTmallShop {
	static DatatableConfig datatableConfig = null;
	static DatatableOperator datatableOperator = null;
	
	public static void main(String[] args) {
		//数据表配置
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		
		//数据表配置
		datatableConfig = DatatableConfig.createDatatableConfigByXml("shoptmall_datatable_config.xml");
		datatableConfig.buildConfig();
		

		datatableOperator = new DatatableOperator(datatableConfig, (JdbcTemplate)BeanHelper.getBean("jdbcTemplate"));
		
		int pageSize = 24;
		for (int i = 0; i < pageSize; i++) {
			String url = "http://s.taobao.com/search?spm=0.0.0.0.gvXm5x&q=%E6%89%8B%E6%9C%BA&app=shopsearch&cat=1512&cps=1&fs=1&isb=1";
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
	
	private static void processPage(FetchEntry fetchEntry) {
		Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent().toString());
		Elements shopElts = doc.select("div#J_ShopList>div#list-content>ul#list-container>li.list-item");
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		if (!CollectionUtils.isEmpty(shopElts)) {
			for (Element shopElt : shopElts) {
				
				Map<String, Object> row = new HashMap<String, Object>();
				String shopHref = JsoupUtils.extrFirstAttr(shopElt, "ul.clearfix>li.list-img>a", "href");
				if (StringUtils.isBlank(shopHref)) {
					shopElt = Jsoup.parse(shopElt.text());
					shopHref = JsoupUtils.extrFirstAttr(shopElt, "ul.clearfix>li.list-img>a", "href");
				}
				
				String shopId = JsoupUtils.extrFirstAttr(shopElt, "ul.clearfix>li.list-info>p.shop-info>span.shop-info-list>span", "data-item");
				if (StringUtils.isBlank(shopId)) {
					shopId = TextUtils.substringBetweenAndRemove(shopHref, "shop", ".taobao", "");
				}
				if (StringUtils.isNotBlank(shopId)) {
					String shopName = JsoupUtils.extrFirstAttr(shopElt, "ul.clearfix>li.list-img>a", "title");
					String keyword = JsoupUtils.extrFirstText(shopElt, "ul.clearfix>li.list-info>p.main-cat>a");
					String nick = JsoupUtils.extrFirstText(shopElt, "ul.clearfix>li.list-info>p.shop-info>span.shop-info-list>a");
					nick = StringUtils.deleteWhitespace(nick);
					String wangwang = JsoupUtils.extrFirstAttr(shopElt, "ul.clearfix>li.list-info>p.shop-info>span.shop-info-list>span>a", "href");
					String city =  JsoupUtils.extrFirstText(shopElt, "ul.clearfix>li.list-info>p.shop-info>span.shop-address>span");
					Integer selseNum =  TextUtils.extrNumber(JsoupUtils.extrFirstText(shopElt, "ul.clearfix>li.list-info>span.pro-sale-num>span.info-sale>em"));
					Integer onSelsNum = TextUtils.extrNumber(JsoupUtils.extrFirstText(shopElt, "ul.clearfix>li.list-info>span.pro-sale-num>span.info-sum>em"));
					row.put("SHOPID", shopId);
					row.put("SHOPHREF", shopHref);
					row.put("SHOPNAME", shopName);
					row.put("KEYWORD", keyword);
					row.put("NICK", nick);
					row.put("WANGWANG", wangwang);
					row.put("CITY", city);
					row.put("SELSENUM", selseNum);
					row.put("ONSELSNUM", onSelsNum);
					row.put("ALIS_HREF", "");
					row.put("USER_ID", "");
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
