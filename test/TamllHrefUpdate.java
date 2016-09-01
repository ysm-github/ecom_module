import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.utils.CrawlerUtils;
import com.sxit.crawler.utils.UrlUtils;



/**
 * <strong>TamllHrefUpdate</strong><br>
 * <p>
 * 天猫链接补充更新程序：
 * 1、从淘宝接口采集到的天猫链接为：http://shop57300646.taobao.com
 * 2、实际上抓取存在跳转，因此需要将上述链接进行一次提取
 * </p>
 * @since 0.1
 * @version $Id: TamllHrefUpdate.java,v 0.1 2013-10-18 下午4:10:48 lex Exp $
 */
public class TamllHrefUpdate {

	public static void main(String[] args) {
		//数据表配置
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(jdbcTemplate);
		String sql = "select shop_id, shop_href from TBAS_SHOP_TMALL where alis_href is null";
		
		//key为shopid
		final Map<String, String> shopUrlMap = new LinkedHashMap<String, String>();
		jdbcTemplate.query(sql, new RowMapper() {
			@Override
			public Object mapRow(ResultSet rs, int idx) throws SQLException {
				String shopId = rs.getString("SHOP_ID");
				String shopHref = rs.getString("SHOP_HREF");
				shopUrlMap.put(shopId, shopHref);
				return null;
			}
		});
		System.out.printf("共有%d个店铺链接需要处理", shopUrlMap.size());
		List<Map<String, Object>> updParams = new ArrayList<Map<String,Object>>();
		int i=0;
		String updSql = "update TBAS_SHOP_TMALL set alis_href = :ALIS_HREF where shop_id = :SHOP_ID";
		for (String shopId : shopUrlMap.keySet()) {
			i++;
			Map<String, Object> updParam = new HashMap<String, Object>();
			String shopUrl = shopUrlMap.get(shopId);
			if (!StringUtils.endsWith(shopUrl, "/shop/viewShop.htm")) {
				shopUrl += "/shop/viewShop.htm";
			}
			String alisUrl = CrawlerUtils.extrRedirectHref(FetchEntityBuilder.buildFetchEntry(shopUrl));
			alisUrl = UrlUtils.getBase(alisUrl);
			System.out.println("shopId:"+shopId);
			System.out.println("alisUrl:"+alisUrl);
			System.out.println("shopUrl:"+shopUrl);
			System.out.println("-----------------------------------------");
			updParam.put("SHOP_ID", shopId);
			updParam.put("ALIS_HREF", alisUrl);
			updParams.add(updParam);
			if (i%20==0) {
				System.out.println("达到阀值开始保存。");
				Map<String, Object> updMapParas[] = new Map[updParams.size()];
				updMapParas = updParams.toArray(updMapParas);
				simpleJdbcTemplate.batchUpdate(updSql, updMapParas);
				updParams.clear();
			}
		}
		Map<String, Object> updMapParas[] = new Map[updParams.size()];
		updMapParas = updParams.toArray(updMapParas);
		simpleJdbcTemplate.batchUpdate(updSql, updMapParas);
	}
}
