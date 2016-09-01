

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.commons.SystemConstant;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.econ.searchcomm.SearchModuleHelper;
import com.sxit.crawler.econ.tamll.TmallSearchCrawlModule;

public class TmallTest {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		System.setProperty(SystemConstant.APP_HOME_KEY, "E:/lex/workspace/crawler/ecom_module/data");
		System.setProperty(SystemConstant.MODULE_HOME_KEY, "E:/lex/workspace/crawler/ecom_module/data/tamll");
		TmallSearchCrawlModule tmallSearchCrawlModule = new TmallSearchCrawlModule();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName("TmallSearchCrawlModule");
		crawlConfig.setAppId(51);
		tmallSearchCrawlModule.setCrawlConfig(crawlConfig);
		tmallSearchCrawlModule.execute();
	}
}
