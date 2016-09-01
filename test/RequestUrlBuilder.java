

import java.util.List;
import java.util.TreeMap;

import com.sxit.crawler.utils.DateUtils;
import com.sxit.crawler.utils.Md5Utils;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.domain.Item;
import com.taobao.api.domain.Shop;
import com.taobao.api.domain.Task;
import com.taobao.api.request.ItemsListGetRequest;
import com.taobao.api.request.ShopGetRequest;
import com.taobao.api.request.TopatsItemcatsGetRequest;
import com.taobao.api.request.TopatsResultGetRequest;
import com.taobao.api.request.UserSellerGetRequest;
import com.taobao.api.response.ItemsListGetResponse;
import com.taobao.api.response.ShopGetResponse;
import com.taobao.api.response.TopatsItemcatsGetResponse;
import com.taobao.api.response.TopatsResultGetResponse;

public class RequestUrlBuilder {

	private TreeMap<String, String> paramMap = new TreeMap<String, String>();
	
	public final static String KEY = "200144d4280aaaedeb6f41c78ea60f43";
	
	private String appKey = "12679450";
	private String format = "json";
	private String from = "wm";
	private String method = "tmall.items.search";
	private String sign_method = "md5";
	private String v = "2.0";
//	@SuppressWarnings("deprecation")
//	public void setParams(String cat, String start, String q) {
//		paramMap.clear();
//		paramMap.put("format", format);
//		paramMap.put("from", from);
//		paramMap.put("method", method);
//		paramMap.put("app_key", appKey);
//		paramMap.put("sign_method", sign_method);
//		paramMap.put("v", v);
//		paramMap.put("cat", cat);
//		paramMap.put("start", start);
//		paramMap.put("s", "1000");
//		paramMap.put("q", q);
//		paramMap.put("timestamp", DateUtils.getCurrentDateStr(DateUtils.YYYY_MM_DD_HH_MM_SS));
//	}
	
//	public void setParams(LinkedHashMap<String, String> params) {
//		paramMap.put("format", format);
//		paramMap.put("from", from);
//		paramMap.put("method", method);
//		paramMap.put("app_key", appKey);
//		paramMap.put("sign_method", sign_method);
//	}
	
	public String getSign() {
		StringBuffer sb = new StringBuffer();
		sb.append(KEY);
		for (String key : paramMap.keySet()) {
			sb.append(key).append(paramMap.get(key));
		}
		sb.append(KEY);
		return Md5Utils.getMD5(sb.toString()).toUpperCase();
	}
	
	public String getRequestString() {
		String sign = getSign();
		StringBuffer sb = new StringBuffer();
		sb.append("sign=").append(sign);
		sb.append("&");
		int size = paramMap.keySet().size();
		for (String key : paramMap.keySet()) {
			sb.append(key).append("=").append(paramMap.get(key));
			if (size > 0) {
				sb.append("&");
			}
			size--;
		}
		return sb.toString();
	}
	
//	public static String getItemCatas() {
//		RequestUrlBuilder requestUrlBuilder = new RequestUrlBuilder();
//		TreeMap<String, String> param = new TreeMap<String, String>();
//		param.put("method", "taobao.topats.itemcats.get");
//		param.put("timestamp", DateUtils.getCurrentDateStr(DateUtils.YYYY_MM_DD_HH_MM_SS));
//		param.put("format", "json");
//		param.put("app_key", "12679450");
//		param.put("v", "2.0");
//		param.put("sign_method", "md5");
//		requestUrlBuilder.paramMap = param;
//		return requestUrlBuilder.getRequestString();
//	}
	
//	public static String getCatAttr() {
//		RequestUrlBuilder requestUrlBuilder = new RequestUrlBuilder();
////		requestUrlBuilder.setParams("50024400", "1", "");
////		System.out.println(requestUrlBuilder.getRequestString());
//		
//		TreeMap<String, String> param = new TreeMap<String, String>();
//		param.put("method", "taobao.itempropvalues.get");
//		param.put("timestamp", DateUtils.getCurrentDateStr(DateUtils.YYYY_MM_DD_HH_MM_SS));
//		param.put("format", "json");
//		param.put("app_key", "12679450");
//		param.put("v", "2.0");
//		param.put("sign_method", "md5");
//		param.put("fields", "cid,pid,prop_name,vid,name,name_alias,status,sort_order");
//		param.put("cid", "50010538");
//		requestUrlBuilder.paramMap = param;
//		return requestUrlBuilder.getRequestString();
//	}
	
	public static String getCatItemsUrl() throws Exception{
		TaobaoClient client=new DefaultTaobaoClient(url, appkey, secret);
		TopatsItemcatsGetRequest req=new TopatsItemcatsGetRequest();
		TopatsItemcatsGetResponse response = client.execute(req);
		System.out.println(response.getBody());
		
		TopatsResultGetRequest topatsResultGetRequest = new TopatsResultGetRequest();
		topatsResultGetRequest.setTaskId(203495008L);
		TopatsResultGetResponse topatsResultGetResponse = client.execute(topatsResultGetRequest);
		if (topatsResultGetResponse.isSuccess() && topatsResultGetResponse.getTask().getStatus().equals("done")) {
			Task task = topatsResultGetResponse.getTask();
			String downloadUrl = task.getDownloadUrl();
			return downloadUrl;
		}
		return null;
	}
	
	public static void getShopInfo() throws Exception {
		TaobaoClient client=new DefaultTaobaoClient(url, appkey, secret);
		//获取店铺信息
		ShopGetRequest shopGetRequest = new ShopGetRequest();
		shopGetRequest.setFields("sid, cid, nick, title, desc, bulletin, pic_path, created, modified, shop_score");
		shopGetRequest.setNick("tearing_angel");
		ShopGetResponse shopGetResponse = client.execute(shopGetRequest);
		if (shopGetResponse.isSuccess()) {
			Shop shop = shopGetResponse.getShop();
			System.out.printf("sid:%s\r\n", shop.getSid()); //店铺编号。shop+sid.taobao.com即店铺地址，如shop123456.taobao.com
			System.out.printf("cid:%s\r\n", shop.getCid()); //店铺所属的类目编号
			System.out.printf("nick:%s\r\n", shop.getNick()); //卖家昵称
			System.out.printf("title:%s\r\n", shop.getTitle());//店铺标题
			System.out.printf("bulletin:%s\r\n", shop.getBulletin());//店铺公告
			System.out.printf("pic_path:%s\r\n", shop.getPicPath());//店标地址。返回相对路径，可以用"http://logo.taobao.com/shop-logo"来拼接成绝对路径
			System.out.printf("created:%s\r\n", shop.getCreated().toLocaleString());//开店时间。格式：yyyy-MM-dd HH:mm:ss
			System.out.printf("modified:%s\r\n", shop.getModified().toLocaleString());//最后修改时间。格式：yyyy-MM-dd HH:mm:ss
			System.out.printf("shop_score:%s\r\n", shop.getShopScore().getServiceScore());//店铺动态评分信息
			System.out.printf("desc:%s\r\n", shop.getDesc());//店铺描述
		}
	}
	/**
	 * a5d6cc9fbf9d991df7a5accabaef5388
	 */
	static String url = "http://gw.api.taobao.com/router/rest";
	static String appkey = "21271339";
	static String secret = "a5d6cc9fbf9d991df7a5accabaef5388";
	public static void main(String[] args) throws Exception{
		//http://gw.api.taobao.com/router/rest
//		String url = "http://tdc.taobao.com/tdc/app?sign=********&timestamp=2013-02-21+16%3A02%3A37&app_key=******&sign_method=md5&partner_id=**********o&format=csv&where=id+lt+4";
//		System.out.println("http://gw.api.taobao.com/router/rest?"+getItemCatas());
		//System.out.println("http://gw.api.taobao.com/router/rest?"+requestUrlBuilder.getRequestString());
//		TaobaoClient client=new DefaultTaobaoClient(url, appkey, secret);
//		ProductGetRequest productGetRequest = new ProductGetRequest();
//		productGetRequest.setFields("product_id");
//		productGetRequest.setProductId(18195181523L);
//		ProductGetResponse productGetResponse = client.execute(productGetRequest);
//		System.out.println(productGetResponse.getBody());
		//product_id,outer_id,created,tsc,cid,cat_name,props,props_str,binds_str,sale_props_str,collect_num,product_imgs,product_extra_infos,sell_pt,cspu_feature,name,binds,sale_props,price,desc,pic_url,modified,product_prop_imgs,status,level,pic_path,rate_num,sale_num,shop_price,standard_price,vertical_market,customer_props,property_alias
		//taobao.products.get
		TaobaoClient client=new DefaultTaobaoClient(url, appkey, secret);
//		TopatsResultGetRequest topatsResultGetRequest = new TopatsResultGetRequest();
//		topatsResultGetRequest.setTaskId(203495008L);
//		TopatsResultGetResponse rsp = client.execute(topatsResultGetRequest);
//		if (rsp.isSuccess() && rsp.getTask().getStatus().equals("done")) {
//	         Task task = rsp.getTask();
//	         String url = task.getDownloadUrl();
//	         File taskFile = AtsUtils.download(url, new File("c:/topats/result")); // 下载文件到本地
//	         File resultFile = new File("c:/topats/unzip", task.getTaskId() + ""); // 解压后的结果文件夹
//	         List<File> files = AtsUtils.unzip(taskFile, resultFile); // 解压缩并写入到指定的文件夹
//	         // 遍历解压到的文件列表并读取结果文件进行解释 …
//		} else {
//		         // TODO 处理错误信息
//		}
		
//		ProductsGetRequest productsGetRequest = new ProductsGetRequest();
//		productsGetRequest.setFields("product_id,tsc,cat_name,name");
//		productsGetRequest.setNick("tearing_angel");
//		productsGetRequest.setPageNo(1L);
//		productsGetRequest.setPageSize(10L);
//		ProductsGetResponse productsGetResponse = client.execute(productsGetRequest);
//		String body = productsGetResponse.getBody();
//		System.out.println(body);
		
//		getShopInfo();
		
		//获取关联店铺
//		ShoprecommendShopsGetRequest shoprecommendShopsGetRequest = new ShoprecommendShopsGetRequest();
//		shoprecommendShopsGetRequest.setSellerId(217225L);
//		shoprecommendShopsGetRequest.setRecommendType(1L);
//		shoprecommendShopsGetRequest.setCount(160L);
//		
//		ShoprecommendShopsGetResponse shoprecommendShopsGetResponse = client.execute(shoprecommendShopsGetRequest);
//		if (shoprecommendShopsGetResponse.isSuccess()) {
//			List<FavoriteShop> favoriteShops = shoprecommendShopsGetResponse.getFavoriteShops();
//			for (FavoriteShop favoriteShop : favoriteShops) {
//				System.out.println("favoriteShop.getShopId():"+favoriteShop.getShopId());
//				System.out.println("favoriteShop.getSellerId():"+favoriteShop.getSellerId());
//				System.out.println("favoriteShop.getSellerNick():"+favoriteShop.getSellerNick());
//				System.out.println("favoriteShop.getShopName():"+favoriteShop.getShopName());
//				System.out.println("favoriteShop.getShopPic():"+favoriteShop.getShopPic());
//				System.out.println("favoriteShop.getShopUrl():"+favoriteShop.getShopUrl());
//				System.out.println("favoriteShop.getRate():"+favoriteShop.getRate());
//				System.out.println("==========================================");
//			}
//			System.out.println(shoprecommendShopsGetResponse.getBody());
//			System.out.println(favoriteShops.size());
//		}
		
		ItemsListGetRequest itemsListGetRequest = new ItemsListGetRequest();
		itemsListGetRequest.setFields("num_iid,title,nick,price,detail_url,props_name," +
				"created,features,valid_thru,cid,pic_url,num," +
				"list_time,delist_time,stuff_status,location,has_discount," +
				"approve_status,product_id,is_virtual,is_taobao,is_ex,is_timing,is_3D,second_kill," +
				"is_lightning_consignment,freight_payer,after_sale_id");
		itemsListGetRequest.setNumIids("16173236242");
//		itemsListGetRequest.setNumIids("17133131058");
		ItemsListGetResponse itemsListGetResponse = client.execute(itemsListGetRequest);
		if (itemsListGetResponse.isSuccess()) {
			System.out.println(itemsListGetResponse.getBody());
			List<Item> items = itemsListGetResponse.getItems();
			for (Item item : items) {
				System.out.println("item.getNumIid():"+item.getNumIid());//商品ID
				System.out.println("item.getTitle():"+item.getTitle());//商品标题
				System.out.println("item.getNick():"+item.getNick());//商家昵称
				System.out.println("item.getPrice():"+item.getPrice());//价格
				System.out.println("item.getDetailUrl():"+item.getDetailUrl());//宝贝页面
//				System.out.println("item.getDesc():"+item.getDesc());
				System.out.println("item.getPropsName():"+item.getPropsName());//属性表
				System.out.println("item.getCreated():"+item.getCreated());//发布时间
				System.out.println("item.getValidThru():"+item.getValidThru());//有效期
				System.out.println("item.getCid():"+item.getCid());//叶子类目ID
				System.out.println("item.getPicUrl():"+item.getPicUrl());//宝贝主图
				System.out.println("item.getNum():"+item.getNum());//宝贝数量
				System.out.println("item.getListTime():"+item.getListTime());//上架时间
				System.out.println("item.getDelistTime():"+item.getDelistTime());//下架时间
				System.out.println("item.getStuffStatus():"+item.getStuffStatus());//商品新旧程度 全新:new，闲置:unused，二手：second
				System.out.println("item.getLocation():"
				+item.getLocation().getDistrict()
				+":"+item.getLocation().getAddress()
				+":"+item.getLocation().getCity()
				+":"+item.getLocation().getCountry()
				+":"+item.getLocation().getState()
				+":"+item.getLocation().getZip()
				);
				System.out.println("item.getHasDiscount():"+item.getHasDiscount());//支持会员打折,true/false
				System.out.println("item.getFreightPayer():"+item.getFreightPayer());//运费承担方式,seller（卖家承担），buyer(买家承担）
				System.out.println("item.getApproveStatus():"+item.getApproveStatus());//商品上传后的状态。onsale出售中，instock库中
				System.out.println("item.getProductId():"+item.getProductId());//宝贝所属产品的id(可能为空). 该字段可以通过taobao.products.search 得到
				System.out.println("item.getIsVirtual():"+item.getIsVirtual());//虚拟商品的状态字段
				System.out.println("item.getIsTaobao():"+item.getIsTaobao());//是否在淘宝显示
				System.out.println("item.getIsEx():"+item.getIsEx());//是否在外部网店显示
				System.out.println("item.getIs3D():"+item.getIs3D());//是否是3D淘宝的商品
				System.out.println("item.getIsLightningConsignment():"+item.getIsLightningConsignment());//是否24小时闪电发货
				System.out.println("item.getSecondKill():"+item.getSecondKill());//是否24小时闪电发货
				System.out.println("item.getAfterSaleId():"+item.getAfterSaleId());//是否24小时闪电发货
				System.out.println("-----------------------------------------------");
			}
			
			
		}
	}
}
