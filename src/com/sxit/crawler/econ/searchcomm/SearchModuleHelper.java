package com.sxit.crawler.econ.searchcomm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.sxit.crawler.commons.BeanHelper;


public class SearchModuleHelper {

	/**
	 * 装载所有关键字
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<String> laodAllKeywords() {
		String sql = "select keyword from tbas_ecom_keywords where eff_state = '1' order by order_no asc";
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		List<String> list = (List<String>)jdbcTemplate.query(sql, new RowMapper() {

			@Override
			public Object mapRow(ResultSet rs, int idx) throws SQLException {
				String keyword = rs.getString(1);
				return keyword;
			}
			
		});
		return list;
	}
}
