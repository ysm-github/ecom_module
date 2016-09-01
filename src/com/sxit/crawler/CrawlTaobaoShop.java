package com.sxit.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sxit.crawler.utils.UrlUtils;

/**
 * <strong>CrawlTmallShop</strong><br>
 * <p>
 * 
 * </p>
 * 
 * @since 0.1
 * @version $Id: CrawlTmallShop.java,v 0.1 2013-10-15 下午6:10:45 lex Exp $
 */
public class CrawlTaobaoShop {
	private static Logger log = LoggerFactory.getLogger(CrawlTaobaoShop.class);

	static DatatableConfig datatableConfig = null;

	static DatatableOperator datatableOperator = null;


	// static String[] locs = {
	// "河北", "河南", "湖北", "湖南", "福建", "江苏", "江西",
	// "广东", "广西", "海南", "浙江", "安徽", "吉林", "辽宁",
	// "黑龙江", "山东", "山西", "陕西", "新疆", "内蒙古", "云南",
	// "贵州", "四川", "甘肃", "宁夏", "青海", "西藏", "香港", "澳门", "台湾"
	// };
	static String[] locs = { 
		"河北", "河南", "湖北", "湖南", "福建", "江苏", "江西", "广东",
			"广西", "海南", "浙江", "安徽", "吉林", "辽宁", "黑龙江", "山东", "山西", "陕西", "新疆",
			"内蒙古", "云南", "贵州", "四川", "甘肃", "宁夏", "青海", "西藏", "香港", "澳门", "台湾",
			"北京", "天津", "上海", "重庆", "广州", "深圳", "中山", "珠海", "佛山", "东莞", "惠州",
			"海外"};
		//"美国,英国,法国,瑞士,澳洲,新西兰,加拿大,奥地利,韩国,日本,德国,意大利,西班牙,俄罗斯,泰国,印度,荷兰,新加坡,海外,其它国家" 

	static int rowSum = 0;
	public static void main(String[] args) {
		// 数据表配置
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");

		// 数据表配置
		datatableConfig = DatatableConfig
				.createDatatableConfigByXml("shoptaobao_datatable_config.xml");
		datatableConfig.buildConfig();

		datatableOperator = new DatatableOperator(datatableConfig,
				(JdbcTemplate) BeanHelper.getBean("jdbcTemplate"));

		int totalPageNum = 0;
		for (String loc : locs) {
			try {
				String url = "http://s.taobao.com/search?spm=0.0.0.0.NkcY5w&q=%CA%D6%BB%FA&app=shopsearch&cat=1512&cps=1&fs=1&isb=0&loc="
						+ UrlUtils.encodeUrl(loc, "GB2312");
				int n = -1;
				try {
					n = getPageNum(url);
					log.info("get local[" + loc + "] pageNum:[" + n+"]");
				} catch (Exception e) {
					log.error("get local[" + loc + "] pageNum error!", e);
				}
				if (n > 0) {
					int pageSize = n;
					totalPageNum += n;
					for (int i = 0; i < pageSize; i++) {
						String purl = url + "&s=" + (i * 20);
						log.info("page["+(i+1)+"/"+pageSize+"],"+purl);
					 	FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(purl);
						try {
							FetchHTTP fetchHTTP = new FetchHTTP();
							fetchEntry = fetchHTTP.process(fetchEntry);
						} catch (Exception e) {
							log.error("httpfetch local[" + loc + "/"+pageSize+"],page["+(i+1)+"] error!,"+purl);
						}
						if (fetchEntry.getResult() != null) {
								try {
									processPage(fetchEntry, loc);
								} catch (RuntimeException e) {
									log.error("process local[" + loc + "/"+pageSize+"],page["+(i+1)+"] error!",e);
								}
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		log.info("totalPageNum:" + totalPageNum);
		log.info("rowSum:" + rowSum);

	}

	private static int getPageNum(String url) {
		FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
		FetchHTTP fetchHTTP = new FetchHTTP();
		fetchEntry = fetchHTTP.process(fetchEntry);
		if (fetchEntry.getResult() != null
				&& null != fetchEntry.getResult().getPageContent()) {
			String html = fetchEntry.getResult().getPageContent().toString();
			int rowNum = TextUtils
					.extrNumber(TextUtils
							.extrValueByRegx(
									"<span[\\s\\S]*?class=\"shop-count\"[\\s\\S]*?>[\\s\\S]*?<b>([\\d]*?)</b>[\\s\\S]*?</span>",
									html, 1));
			rowSum += rowNum;
			return TextUtils.extrNumber(TextUtils.extrValueByRegx(
					"<span class=\"page-info\">1/(.*)</span>", html, 1));
		}
		return -1;
	}

	private static void processPage(FetchEntry fetchEntry, String loc) {
		Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent()
				.toString());
		Elements shopElts = doc
				.select("div#J_ShopList>div#list-content>ul#list-container>li.list-item");
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		if (!CollectionUtils.isEmpty(shopElts)) {
			for (Element shopElt : shopElts) {
				Map<String, Object> row = new HashMap<String, Object>();
				String shopHref = JsoupUtils.extrFirstAttr(shopElt,
						"ul.clearfix>li.list-img>a", "href");
				if (StringUtils.isBlank(shopHref)) {
					shopElt = Jsoup.parse(shopElt.text());
					shopHref = JsoupUtils.extrFirstAttr(shopElt,
							"ul.clearfix>li.list-img>a", "href");
				}
				String shopId = JsoupUtils
						.extrFirstAttr(
								shopElt,
								"ul.clearfix>li.list-info>p.shop-info>span.shop-info-list>span",
								"data-item");
				if (StringUtils.isBlank(shopId)) {
					shopId = TextUtils.substringBetweenAndRemove(shopHref,
							"shop", ".taobao", "");
				}
				if (StringUtils.isNotBlank(shopId)) {
					String shopName = JsoupUtils.extrFirstAttr(shopElt,
							"ul.clearfix>li.list-img>a", "title");
					String keyword = JsoupUtils.extrFirstText(shopElt,
							"ul.clearfix>li.list-info>p.main-cat>a");
					String nick = null;
					Integer selseNum = -1;
					Integer onSelsNum = -1;
					String wangwang = null;
					String city = null;

					if (StringUtils.isBlank(keyword)) {
						// 特殊模块
						// 伟华通讯数码超市 卖家信用 卖家: 孟伟716 河北 唐山 主营:手机 销量347共16 上新2优惠7
						// 好评率: 99.70% 消费者保障七天退换 ￥829.01 ￥650.00 ￥820.00
						// ￥1050.00 找相似店铺 >
						// 郑州鼎好通讯 卖家信用 卖家: 河南手机 河南 郑州 主营:手机 销量2共6 上新7优惠1 好评率:
						// 95.06% 消费者保障七天退换 ￥868.00 ￥780.00 ￥1758.00 ￥1799.00
						// 找相似店铺 >
						String info = shopElt.text();
						keyword = StringUtils.substringBetween(info, "主营:",
								"销量");
						nick = StringUtils.substringBetween(info, "卖家: ", " ");
						city = loc
								+ " "
								+ StringUtils.substringBetween(info, loc, "主营")
										.trim();
						try {
							selseNum = Integer.parseInt(StringUtils
									.substringBetween(info, "销量", "共"));
						} catch (Exception e) {
							e.printStackTrace();
						}
						try {
							onSelsNum = Integer.parseInt(StringUtils
									.substringBetween(info, "共", " ").trim());
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						nick = JsoupUtils
								.extrFirstText(shopElt,
										"ul.clearfix>li.list-info>p.shop-info>span.shop-info-list>a");
						nick = StringUtils.deleteWhitespace(nick);
						wangwang = JsoupUtils
								.extrFirstAttr(
										shopElt,
										"ul.clearfix>li.list-info>p.shop-info>span.shop-info-list>span>a",
										"href");
						city = JsoupUtils
								.extrFirstText(shopElt,
										"ul.clearfix>li.list-info>p.shop-info>span.shop-address");
						selseNum = TextUtils
								.extrNumber(JsoupUtils
										.extrFirstText(shopElt,
												"ul.clearfix>li.list-info>span.pro-sale-num>span.info-sale>em"));
						onSelsNum = TextUtils
								.extrNumber(JsoupUtils
										.extrFirstText(shopElt,
												"ul.clearfix>li.list-info>span.pro-sale-num>span.info-sum>em"));
					}
					row.put("SHOPID", shopId);
					row.put("SHOPHREF", shopHref);
					row.put("SHOPNAME", shopName);
					row.put("KEYWORD", keyword);
					row.put("NICK", nick);
					row.put("WANGWANG", wangwang);
					row.put("CITY", city);
					row.put("SELSENUM", selseNum);
					row.put("ONSELSNUM", onSelsNum);
					list.add(row);
				} else {
					log.info("shopHref:" + shopHref);
					log.info(shopElt.text());
					log
							.info("------------------------------------------------");
				}
			}
		}
		log.info("get shop size["+list.size()+"]");
		// 7060
		datatableOperator.saveData(list, true);
	}
}
