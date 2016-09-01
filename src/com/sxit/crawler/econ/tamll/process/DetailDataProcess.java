package com.sxit.crawler.econ.tamll.process;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;

/**
 * 提取详细数据
 * @author Administrator
 *
 */
public class DetailDataProcess {

	private FetchHTTP fetchHTTP;
	
	public DetailDataProcess() {
		this.fetchHTTP = new FetchHTTP();
	}

	public Map<String, Object> process(FetchEntry fetchEntry, Map<String, Object> dataRow) {
		String catid = "";
		if (StringUtils.equalsIgnoreCase("50024400", catid)) {
			//对手机类数据做详细处理
			return extrMobilePhoneDetailData(fetchEntry, dataRow);
		}
		return dataRow;
	}
	
	/**
	 * 提取手机详情
	 * @param oldFetchEntry
	 * @param dataRow
	 * @return
	 */
	private Map<String, Object> extrMobilePhoneDetailData(FetchEntry oldFetchEntry, Map<String, Object> dataRow) {
		String url = (String)dataRow.get("URL");
		FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
		fetchEntry = fetchHTTP.process(fetchEntry);
		
		
		
		return dataRow;
	}
}
