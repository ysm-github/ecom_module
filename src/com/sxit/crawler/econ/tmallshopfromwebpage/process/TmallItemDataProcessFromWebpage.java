package com.sxit.crawler.econ.tmallshopfromwebpage.process;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.econ.tmallshopfromwebpage.TmallShopItemCrawlerFromWebpageModule;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.sxit.crawler.utils.TextUtils;


/**
 * <strong>TaobaoItemDataProcess</strong><br>
 * <p>
 * 抓取商铺的明细数据
 * </p>
 * @since 0.1
 * @version $Id: TaobaoItemDataProcess.java,v 0.1 2013-10-17 上午10:44:44 lex Exp $
 */
public class TmallItemDataProcessFromWebpage extends CrawlProcess{

	private static Logger log = LoggerFactory.getLogger(TmallItemDataProcessFromWebpage.class);
	private static String headUrl = "http://detail.tmall.com/item.htm?id=";
	
	public TmallItemDataProcessFromWebpage(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
	}
	@SuppressWarnings("unchecked")
	@Override
	public void process(Map<String, Object> param) {
		String countSql = "select count(*) from TBAS_SHOP_TMALL_ITEMIDS where PRC_FLAG = 0";
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		Long count = jdbcTemplate.queryForLong(countSql);
		log.info("共有商品条码没有处理数量-->{}", count);
		String sql = "select A.*, ROWNUM from (SELECT * FROM TBAS_SHOP_TMALL_ITEMIDS where PRC_FLAG = 0  order by crt_date) A where ROWNUM <= ? ";
		int pageSize = 20;
		long startTime, endTime;
		if (null != count && count > 0) {
			//每次取20条记录
			int totalPage = (int) ((count.longValue()/pageSize)+1);
			for (int i=0; i<totalPage; i++) {
				LinkedHashMap<String, String> idMaps = new LinkedHashMap<String, String>();
				try {
					List<Map<String, Object>> rows = (List<Map<String,Object>>)jdbcTemplate.query(sql, new Object[]{pageSize}, new ColumnMapRowMapper());
					if (!CollectionUtils.isEmpty(rows)) {
						for (Map<String, Object> row : rows) {
							String itemId = (String)row.get("ITEM_ID");
							String shopId = (String)row.get("SHOP_ID");
							idMaps.put(itemId, shopId);
						}
					} else {
						log.info("没有查询到数据，退出。");
						break;
					}
					log.info("开始采集第{}页详细数据-->{}", (i+1)+"/"+totalPage,idMaps);
					startTime = System.currentTimeMillis();
					crawlItemDataByIds(idMaps);
					endTime = System.currentTimeMillis();
					log.info("抓取第{}页完成，耗时:{}", (i+1)+"/"+totalPage,(endTime-startTime));
					Thread.sleep(1000L);
				} catch (Exception e) {
					log.error("数据采集错误-->"+idMaps, e);        
				}
			}
		}
	}

	/**
	 * 依据宝贝ID抓取宝贝数据
	 * @param itemIds key 为商品id，value为店铺id
	 */
	private void crawlItemDataByIds(LinkedHashMap<String, String> idMaps) throws Exception{
		if (CollectionUtils.isEmpty(idMaps)) {
			return;
		}
		Iterator<String> it = idMaps.keySet().iterator();
		while (it.hasNext()) {
			try {
				String itemId = it.next();
				String shopId = idMaps.get(itemId);
				String purl = headUrl + itemId;
				log.info(purl);
				FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(purl);
				FetchHTTP fetchHTTP = new FetchHTTP();
				fetchEntry = fetchHTTP.process(fetchEntry);
				if (fetchEntry.getResult() != null) {
					processPage(fetchEntry, itemId, shopId);
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		if (!CollectionUtils.isEmpty(idMaps)) {
			List<String> ids = new ArrayList<String>();
			ids.addAll(idMaps.keySet());
			updateItemIdsTableState(ids);
			ids.clear();
		}
	}
	private void processPage(FetchEntry fetchEntry, String itemId, String shopId) {
		String content=fetchEntry.getResult().getPageContent().toString();
		content=content.replaceAll("&nbsp;", "");
		StringBuffer sb=new StringBuffer();
		sb.append(content);
		fetchEntry.getResult().setPageContent(sb);
		int cid = StringUtils.indexOfIgnoreCase(content, 
//				"categoryId':'1512'"
				"categoryId=1512" //2014-02-25 天猫手机商品详细有改动
		);
		content=null;
		if (cid > -1) {
			processMobilePage(fetchEntry, itemId, shopId);
		}
	}
	
	private void processMobilePage(FetchEntry fetchEntry, String itemId, String shopId) {
		String content=fetchEntry.getResult().getPageContent().toString();
		Document doc = Jsoup.parse(content);

		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		Map<String, Object> row = new HashMap<String, Object>();

		String paramData = getText(doc, "div#J_Detail>div>table>tbody", 0);

		String ID = "";
		String DETAIL_URL = fetchEntry.getUrl();
		String ITEM_ID = itemId;
		String PRICE = TextUtils.substringBetweenAndRemove(content, "\"reservePrice\":\"", "\"", "");
		String MOBILEBRAND = TextUtils.substringBetweenAndRemove(paramData, "品牌", "型号", "");
		if(StringUtils.isBlank(MOBILEBRAND)){
			MOBILEBRAND = TextUtils.substringBetweenAndRemove(paramData, "品牌 ", " ", "");
		}
		String MOBILETYPE = TextUtils.substringBetweenAndRemove(paramData, "型号", "上市时间", "");
		String MOBILECOLOR = TextUtils.substringBetweenAndRemove(paramData, "机身颜色", "操作系统", "");
		String MOBILEOS = TextUtils.substringBetweenAndRemove(paramData, "操作系统", "高级功能", "");
		String MOBILEMARKETTIME = TextUtils.substringBetweenAndRemove(paramData, "上市时间", "上市月份", "");
		String MOBILESCREENSIZE = TextUtils.substringBetweenAndRemove(paramData, "主屏尺寸", "触摸屏", "");
		String MOBLECAMERA = TextUtils.substringBetweenAndRemove(paramData, "后置摄像头", "摄像头类型", "");
		String MOBLEWIFI = "";
		if (StringUtils.indexOfIgnoreCase(paramData, "WIFI") > 0) {
			MOBLEWIFI = "WIFI";
		}
		String MOBLENETWORKSTANDARD = TextUtils.substringBetweenAndRemove(paramData, "网络类型", "网络模式", "");
		String MOBLEBODYMEMORY = TextUtils.substringBetweenAndRemove(paramData, "机身内存", "网络", "");
		String MOBLERUNNINGMEMORY = TextUtils.substringBetweenAndRemove(paramData, "运行内存RAM", "机身内存", "");
		String MOBLECPUNUMBER = TextUtils.substringBetweenAndRemove(paramData, "cpu核心数", "cpu频率", "");
		String MOBLECPUFREQUENCY = TextUtils.substringBetweenAndRemove(paramData, "cpu频率", "电池容量", "");

		String MOBILEPRICESALES = null;
		String PROPS_NAME = paramData;
		String TITLE = getText(doc, "h3[data-spm=1000983", 0);

		row.put("ID", ID);// 数据库主键
		row.put("DETAIL_URL", DETAIL_URL);// 详情url
		row.put("ITEM_ID", ITEM_ID);// 商品ID
		row.put("PRICE", PRICE);// 价格
		row.put("MOBILEBRAND", MOBILEBRAND);// 手机商标
		row.put("MOBILETYPE", MOBILETYPE);// 手机类型
		row.put("MOBILECOLOR", MOBILECOLOR);// 手机颜色
		row.put("MOBILEOS", MOBILEOS);// 手机操作系统
		row.put("MOBILEMARKETTIME", MOBILEMARKETTIME);// 手机上市时间
		row.put("MOBILESCREENSIZE", MOBILESCREENSIZE);// 主屏尺寸
		row.put("MOBLECAMERA", MOBLECAMERA);// 摄像头像素
		row.put("MOBLEWIFI", MOBLEWIFI);// wifi
		row.put("MOBLENETWORKSTANDARD", MOBLENETWORKSTANDARD);// 网络类型
		row.put("MOBLEBODYMEMORY", MOBLEBODYMEMORY);// 机身内存
		row.put("MOBLERUNNINGMEMORY", MOBLERUNNINGMEMORY);// 运行内存
		row.put("MOBLECPUNUMBER", MOBLECPUNUMBER);// cpu核心数
		row.put("MOBLECPUFREQUENCY", MOBLECPUFREQUENCY);// cpu频率
		row.put("MOBILEPRICESALES", MOBILEPRICESALES);
		row.put("PROPS_NAME", PROPS_NAME);// 备注
		row.put("TITLE", TITLE);// 数据库主键

		rows.add(row);

		if (!CollectionUtils.isEmpty(rows)) {
			JdbcTemplate jdbcTemplate = (JdbcTemplate) BeanHelper.getBean("jdbcTemplate");
			DatatableOperator datatableOperator = new DatatableOperator(((TmallShopItemCrawlerFromWebpageModule) crawlModule).tmallItemDatatableConfig, jdbcTemplate);
			datatableOperator.saveData(rows, true);
		}
	}
	
	private String getText(Document doc, String query, int n) {
		try {
			return doc.select(query).get(n).text();
		} catch (Exception e) {
			return null;
		}
	}
	/**
	 * 修改TBAS_SHOP_TMALL_ITEMIDS表中的状态
	 * @param idMaps
	 */
	private synchronized void updateItemIdsTableState(final List<String> itemIds) {
		if (!CollectionUtils.isEmpty(itemIds)) {
			JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
			String sql = "update TBAS_SHOP_TMALL_ITEMIDS set prc_flag = '1', prc_date = sysdate where ITEM_ID = ?";
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int idx) throws SQLException {
					ps.setString(1, itemIds.get(idx));
				}
				
				@Override
				public int getBatchSize() {
					return itemIds.size();
				}
			});
		}
	}
}
