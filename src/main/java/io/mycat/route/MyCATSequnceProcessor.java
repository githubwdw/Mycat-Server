package io.mycat.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.MycatServer;
import io.mycat.config.ErrorCode;
import io.mycat.route.parser.druid.DruidSequenceHandler;
import io.mycat.server.ServerConnection;
import io.mycat.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MyCATSequnceProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyCATSequnceProcessor.class);
	
	//使用Druid解析器实现sequence处理  @兵临城下
	private static final DruidSequenceHandler sequenceHandler = new DruidSequenceHandler(MycatServer
			.getInstance().getConfig().getSystem().getSequnceHandlerType());

	private static class InnerMyCATSequnceProcessor{
		private static MyCATSequnceProcessor INSTANCE = new MyCATSequnceProcessor();
	}

	public static MyCATSequnceProcessor getInstance(){
		return InnerMyCATSequnceProcessor.INSTANCE;
	}

	private MyCATSequnceProcessor() {
	}

	/**
	 *  锁的粒度控制到序列级别.一个序列一把锁.
	 *  如果是 db 方式, 可以 给 mycat_sequence表的 name 列 加索引.可以借助mysql 行级锁 提高并发
	 * @param pair
	 */
	public void executeSeq(SessionSQLPair pair) {
		try {
			/*// @micmiu 扩展NodeToString实现自定义全局序列号
			NodeToString strHandler = new ExtNodeToString4SEQ(MycatServer
					.getInstance().getConfig().getSystem()
					.getSequnceHandlerType());
			// 如果存在sequence 转化sequence为实际数值
			String charset = pair.session.getSource().getCharset();
			QueryTreeNode ast = SQLParserDelegate.parse(pair.sql,
					charset == null ? "utf-8" : charset);
			String sql = strHandler.toString(ast);
			if (sql.toUpperCase().startsWith("SELECT")) {
				String value=sql.substring("SELECT".length()).trim();
				outRawData(pair.session.getSource(),value);
				return;
			}*/

			// 使用Druid解析器实现sequence处理  @兵临城下
			DruidSequenceHandler sequenceHandler = new DruidSequenceHandler(MycatServer
					.getInstance().getConfig().getSystem().getSequnceHandlerType());

			// 生成可执行 SQL ：目前主要是生成 id
			String charset = pair.session.getSource().getCharset();
			String executeSql = sequenceHandler.getExecuteSql(pair.sql,charset == null ? "utf-8":charset);

			// 执行 SQL
			pair.session.getSource().routeEndExecuteSQL(executeSql, pair.type,pair.schema);
		} catch (Exception e) {
			LOGGER.error("MyCATSequenceProcessor.executeSeq(SesionSQLPair)",e);
			pair.session.getSource().writeErrMessage(ErrorCode.ER_YES,"mycat sequnce err." + e);
			return;
		}
	}
}
