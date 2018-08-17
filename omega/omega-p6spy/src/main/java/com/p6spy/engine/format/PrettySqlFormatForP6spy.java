package com.p6spy.engine.format;

import com.p6spy.engine.spy.appender.MultiLineFormat;

/**
 * SQL Format Tool
 * 
 * @author Gannalyo
 */
public class PrettySqlFormatForP6spy extends MultiLineFormat {

	private final MyHibernateSqlFormatter formatter = new MyHibernateSqlFormatter();

	@Override
	public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared,
			String sql) {

		return super.formatMessage(connectionId, now, elapsed, category, formatter.format(prepared),
				formatter.format(sql));
	}
}
