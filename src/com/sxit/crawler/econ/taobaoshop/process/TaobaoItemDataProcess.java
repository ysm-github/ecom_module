package com.sxit.crawler.econ.taobaoshop.process;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.econ.taobaoshop.TaobaoShopItemCrawlerModule;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.domain.Item;
import com.taobao.api.domain.Location;
import com.taobao.api.request.ItemsListGetRequest;
import com.taobao.api.response.ItemsListGetResponse;


/**
 * <strong>TaobaoItemDataProcess</strong><br>
 * <p>
 * 抓取商铺的明细数据
 * </p>
 * @since 0.1
 * @version $Id: TaobaoItemDataProcess.java,v 0.1 2013-10-17 上午10:44:44 lex Exp $
 */
public class TaobaoItemDataProcess extends CrawlProcess{

	private static Logger log = LoggerFactory.getLogger(TaobaoItemDataProcess.class);
	
	static String url = "http://gw.api.taobao.com/router/rest";
	static String appkey = "21271339";
	static String secret = "a5d6cc9fbf9d991df7a5accabaef5388";
	public TaobaoItemDataProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void process(Map<String, Object> param) {
		String countSql = "select count(*) from TBAS_SHOP_TAOBAL_ITEMIDS where PRC_FLAG = 0";
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		Long count = jdbcTemplate.queryForLong(countSql);
		log.info("共有个商品条码没有处理-->", count);
		String sql = "SELECT * FROM ( SELECT A.*, ROWNUM RN FROM (SELECT * FROM TBAS_SHOP_TAOBAL_ITEMIDS where PRC_FLAG = 0 order by crt_date) A WHERE ROWNUM <= ?) WHERE RN >= ?";
		int pageSize = 20;
		long startTime, endTime;
		if (null != count && count > 0) {
			//每次取200条记录
			int totalPage = (int) ((count.longValue()/pageSize)+1);
			for (int i=0; i<totalPage; i++) {
				LinkedHashMap<String, String> idMaps = new LinkedHashMap<String, String>();
				try {
					int start = i*pageSize+1;
					int end = (i+1)*pageSize;
					System.out.printf("start:%d, end%d\r\n", start, end);
					List<Map<String, Object>> rows = (List<Map<String,Object>>)jdbcTemplate.query(sql, new Object[]{end, start}, new ColumnMapRowMapper());
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
					log.info("开始采集详细数据-->{}", idMaps);
					startTime = System.currentTimeMillis();
					crawlItemDataByIds(idMaps);
					endTime = System.currentTimeMillis();
					log.info("抓取完成，耗时:{}", (endTime-startTime));
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
		if (idMaps.size() > 20) {
			log.warn("淘宝API每次只能搜索20记录-->{}", idMaps.size());
			return;
		}
		
		TaobaoClient client=new DefaultTaobaoClient(url, appkey, secret);
		ItemsListGetRequest itemsListGetRequest = new ItemsListGetRequest();
		itemsListGetRequest.setFields("num_iid,title,nick,price,detail_url,props_name," +
				"created,valid_thru,cid,pic_url,num," +
				"list_time,delist_time,stuff_status,location,has_discount," +
				"approve_status,product_id,is_virtual,is_taobao,is_ex,is_timing,is_3D,second_kill," +
				"is_lightning_consignment,freight_payer,after_sale_id");
		itemsListGetRequest.setNumIids(StringUtils.join(idMaps.keySet(), ","));
		ItemsListGetResponse itemsListGetResponse = client.execute(itemsListGetRequest);
		if (itemsListGetResponse.isSuccess()) {
			List<Item> items = itemsListGetResponse.getItems();
			List<Map<String, Object>> rows = new ArrayList<Map<String,Object>>();
			for (Item item : items) {
				Map<String, Object> row = new HashMap<String, Object>();
				row.put("ITEM_ID", String.valueOf(item.getNumIid()));//商品ID
				row.put("SHOP_ID", idMaps.get(String.valueOf(item.getNumIid())));//店铺ID
				row.put("TITLE", item.getTitle());//商品标题
				row.put("NICK", item.getNick());//商家昵称
				row.put("PRICE", item.getPrice());//价格
				row.put("DETAIL_URL", item.getDetailUrl());//宝贝页面
				row.put("PROPS_NAME", item.getPropsName());//属性表
				row.put("CREATED", item.getCreated());//发布时间
				row.put("VALID_THRU", item.getValidThru());//有效期
				row.put("CID", String.valueOf(item.getCid()));//叶子类目ID
				row.put("PIC_URL", item.getPicUrl());//宝贝主图
				row.put("NUM", item.getNum());//宝贝数量
				row.put("LIST_TIME", item.getListTime());//上架时间
				row.put("DELIST_TIME", item.getDelistTime());//下架时间
				row.put("STUFF_STATUS", item.getStuffStatus());//商品新旧程度 全新:new，闲置:unused，二手：second
				row.put("HAS_DISCOUNT", String.valueOf(item.getHasDiscount()));//支持会员打折,true/false
				row.put("APPROVE_STATUS", item.getApproveStatus());//商品上传后的状态。onsale出售中，instock库中
				row.put("PRODUCT_ID", String.valueOf(item.getProductId()));//宝贝所属产品的id(可能为空). 该字段可以通过taobao.products.search 得到
				row.put("IS_VIRTUAL", String.valueOf(item.getIsVirtual()));//虚拟商品的状态字段
				row.put("IS_TAOBAO", String.valueOf(item.getIsTaobao()));//是否在淘宝显示
				row.put("IS_EX", String.valueOf(item.getIsEx()));//是否在外部网店显示
				row.put("IS_3D", String.valueOf(item.getIs3D()));//是否是3D淘宝的商品
				row.put("SECOND_KILL", String.valueOf(item.getSecondKill()));//是否24小时闪电发货
				row.put("IS_LIGHTNING_CONSIGNMENT", String.valueOf(item.getIsLightningConsignment()));//是否24小时闪电发货
				row.put("FREIGHT_PAYER", item.getFreightPayer());//运费承担方式,seller（卖家承担），buyer(买家承担）
				if (null != item.getLocation()) {
					Location loc = item.getLocation();
					row.put("COUNTRY", loc.getCountry());//国家
					row.put("STATE", loc.getState());//省
					row.put("CITY", loc.getCity());//市
					row.put("DISTRICT", loc.getDistrict());//区（县）
					row.put("ADDRESS", loc.getAddress());//详细地址
					row.put("ZIP", loc.getZip());//邮编
				}
				rows.add(row);
			}
			
			if (!CollectionUtils.isEmpty(rows)) {
				JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
				DatatableOperator datatableOperator = new DatatableOperator(((TaobaoShopItemCrawlerModule)crawlModule).taobaoItemDatatableConfig, jdbcTemplate);
				if (datatableOperator.saveData(rows, true)) {
					List<String> ids = new ArrayList<String>();
					ids.addAll(idMaps.keySet());
					updateItemIdsTableState(ids);
					ids.clear();
				}
			}
		}
	}
	
	/**
	 * 修改TBAS_SHOP_TAOBAL_ITEMIDS表中的状态
	 * @param idMaps
	 */
	private synchronized void updateItemIdsTableState(final List<String> itemIds) {
		if (!CollectionUtils.isEmpty(itemIds)) {
			JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
			String sql = "update TBAS_SHOP_TAOBAL_ITEMIDS set prc_flag = '1', prc_date = sysdate where ITEM_ID = ?";
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
