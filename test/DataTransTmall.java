import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.utils.TextUtils;



/**
 * <strong>DataTrans</strong><br>
 * <p>
 *
 * </p>
 * @since 0.1
 * @version $Id: DataTrans.java,v 0.1 2013-10-23 下午5:57:02 lex Exp $
 */
public class DataTransTmall {
	protected static DatatableConfig initDatatableConfig(String tableName) {
		DatatableConfig datatableConfig = DatatableConfig.createDatatableConfigByXml(tableName);
		datatableConfig.buildConfig();
		return datatableConfig;
	}
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		DatatableConfig datatableConfig = initDatatableConfig("datatrans_tmall_datatable_config.xml");
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		String sql = "SELECT * FROM ( SELECT A.*, ROWNUM RN FROM (select * from TBAS_SHOP_TMALL_ITEM where cid = '1512' order by id) A WHERE ROWNUM <= ?) WHERE RN >= ?";
		String countSql = "select count(*) from TBAS_SHOP_TMALL_ITEM where cid = '1512'";
		Long count = jdbcTemplate.queryForLong(countSql);
		int pageSize = 500;
//		int pageSize = 2;
		if (null != count && count > 0) {
			//每次取2000条记录
			int totalPage = (int) ((count.longValue()/pageSize)+1);
			for (int i=0; i<totalPage; i++) {
				try {
					int start = i*pageSize+1;
					int end = (i+1)*pageSize;
					System.out.printf("start:%d, end%d\r\n", start, end);
					List<Map<String, Object>> rows = (List<Map<String,Object>>)jdbcTemplate.query(sql, new Object[]{end, start}, new ColumnMapRowMapper());
					List<Map<String, Object>> dataRows = new ArrayList<Map<String,Object>>();
					if (!CollectionUtils.isEmpty(rows)) {
						for (Map<String, Object> row : rows) {
							Map<String, Object> rowData = transRowData(row);
//							System.out.println(rowData.toString());
							dataRows.add(rowData);
						}
						JdbcTemplate jdbcTemplate2 = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
						DatatableOperator datatableOperator = new DatatableOperator(datatableConfig, jdbcTemplate2);
						datatableOperator.saveData(dataRows, true);
					} else {
						System.out.println("没有查询到数据，退出。");
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	private static Map<String, Object> transRowData(Map<String, Object> row) {
		String paramData = (String)row.get("PROPS_NAME");
		Map<String, Object> rowData = new HashMap<String, Object>();
		rowData.putAll(row);
		//MOBILEBRAND
		String MOBILEBRAND = TextUtils.substringBetweenAndRemove(paramData, ":品牌:", ";", "");
		String MOBILETYPE = TextUtils.substringBetweenAndRemove(paramData, "型号:", ";", "");
		String MOBILECOLOR = TextUtils.extrValueByRegx("1627207:[\\d]*?:机身颜色:([\\S\\s]*?);", paramData, 1);
		String MOBILEOS = TextUtils.extrValueByRegx("20573:[\\d]*?:操作系统:([\\S\\s]*?);", paramData, 1);
		String MOBILEMARKETTIME = TextUtils.extrValueByRegx("30606:[\\d]*?:上市时间:([\\S\\s]*?);", paramData, 1);
		String MOBILESCREENSIZE = TextUtils.extrValueByRegx("1627099:[\\d]*?:主屏尺寸:([\\S\\s]*?);", paramData, 1);
		String MOBLECAMERA = TextUtils.extrValueByRegx("10002:[\\d]*?:后置摄像头:([\\S\\s]*?);", paramData, 1);
		String MOBLEWIFI = "";
		if (StringUtils.indexOfIgnoreCase(paramData, "WIFI") > 0) {
			MOBLEWIFI = "WIFI";
		}
		String MOBLENETWORKSTANDARD = TextUtils.extrValueByRegx("10004:[\\d]*?:网络类型:([\\S\\s]*?);", paramData, 1);
		String MOBLEBODYMEMORY = TextUtils.extrValueByRegx("12304035:[\\d]*?:机身内存:([\\S\\s]*?);", paramData, 1);
		String MOBLERUNNINGMEMORY = TextUtils.extrValueByRegx("12304004:[\\d]*?:运行内存RAM:([\\S\\s]*?);", paramData, 1);
		String MOBLECPUNUMBER = TextUtils.extrValueByRegx("31942019:[\\d]*?:cpu核心数:([\\S\\s]*?);", paramData, 1);
		String MOBLECPUFREQUENCY = TextUtils.substringBetweenAndRemove(paramData, "cpu频率:", ";", "");
		
		rowData.put("MOBILEBRAND", MOBILEBRAND);
		rowData.put("MOBILETYPE", MOBILETYPE);
		rowData.put("MOBILECOLOR", MOBILECOLOR);
		rowData.put("MOBILEOS", MOBILEOS);
		rowData.put("MOBILEMARKETTIME", MOBILEMARKETTIME);
		rowData.put("MOBILESCREENSIZE", MOBILESCREENSIZE);
		rowData.put("MOBLECAMERA", MOBLECAMERA);
		rowData.put("MOBLEWIFI", MOBLEWIFI);
		rowData.put("MOBLEWIFI", MOBLEWIFI);
		rowData.put("MOBLENETWORKSTANDARD", MOBLENETWORKSTANDARD);
		rowData.put("MOBLEBODYMEMORY", MOBLEBODYMEMORY);
		rowData.put("MOBLERUNNINGMEMORY", MOBLERUNNINGMEMORY);
		rowData.put("MOBLECPUNUMBER", MOBLECPUNUMBER);
		rowData.put("MOBLECPUFREQUENCY", MOBLECPUFREQUENCY);
		return rowData;
	}
}
