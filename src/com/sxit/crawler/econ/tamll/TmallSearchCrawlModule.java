package com.sxit.crawler.econ.tamll;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.econ.searchcomm.SearchModuleHelper;
import com.sxit.crawler.econ.tamll.process.TmallSerachCrawlerProcess;
import com.sxit.crawler.module.CrawlModule;

public class TmallSearchCrawlModule extends CrawlModule{
	private static Logger log = LoggerFactory.getLogger(TmallSearchCrawlModule.class);
	
	private final static String DEFAULT_JOB_NAME = TmallSearchCrawlModule.class.getSimpleName();
	
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	
	public final static String PARAM_KEY_CAT_MAPS = "param.cat.idsmap";//商品类目ID集合
	public final static String PARAM_KEY_KEYWORD = "param.keyword";//搜索关键字
	public final static String PARAM_KEY_URL = "param.url";
	
	private final Map<String, String> catMap = new LinkedHashMap<String, String>();//分类数据；
	
	private DatatableConfig datatableConfig = null;
	
	@Override
	public void execute() {
		
		//数据表配置
		datatableConfig = initDatatableConfig("tmall_datatable_config.xml");
		try {
			loadCatMap();
			List<String> keywords = SearchModuleHelper.laodAllKeywords();
			for (String keyword : keywords) {
				Map<String, Object> param = new HashMap<String, Object>();
				param.put(TmallSearchCrawlModule.PARAM_KEY_CAT_MAPS, catMap);
				param.put(TmallSearchCrawlModule.PARAM_KEY_KEYWORD, keyword);
				TmallSerachCrawlerProcess tmallSerachCrawlerProcess = new TmallSerachCrawlerProcess(
						USER_AGENT_PROVIDER, 
						this,
						crawlConfig, 
						datatableConfig);
				
				log.info("开始依据关键字采集:"+keyword);
				tmallSerachCrawlerProcess.process(param);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DatatableOperator getDatatableOperator(Map<String, Object> param) {
		return new DatatableOperator(datatableConfig, (JdbcTemplate)BeanHelper.getBean("jdbcTemplate"));
	}
	
	
	private void loadCatMap() {
		catMap.clear();
		try {
			ClassPathResource resource = new ClassPathResource("tmall_cats.txt");
			List<String> brandStrs = FileUtils.readLines(resource.getFile());
			for (String brandStr : brandStrs) {
				String[] brands = StringUtils.split(brandStr, ",");
				if (null != brands && brands.length == 2) {
					String catId = brands[0];
					String catName = brands[1];
					catMap.put(catId, catName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
