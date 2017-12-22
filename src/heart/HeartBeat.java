package heart;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import config.ReadConfigClass;
import config.WatchConfig;
import parse.ParseIpPort;

/**
 * Created by pros_cy on 2017/6/5.
 */
public class HeartBeat {
	private static Properties conf;

	private final static String FILE_NAME = "conf.properties";
	private final static String PARSE_CONFIG_FORMAT = "parseConfigFormat";
	private final static String DEFAULT_FORMAT = "default";
	private final static String HTTP_URL = "http_url";
	private final static String HOST_NAME = "hostName";

	private final static String DB_USER_NAME = "db_username";
	private final static String DB_PASSWORD = "db_password";
	private final static String DB_DRIVER = "db_driver";
	private final static String DB_URL = "db_url";
	private final static String DB_JAR = "db_jar";

	private final static String DB_TABLE = "db_table";
	private final static String DB_COLUMN = "db_column";

	private final static String BEAT_DELAY_TIME = "beat_delay_time";
	private final static String BEAT_GAP_TIME = "beat_gap_time";

	private final static String PARSE_CONFIG = "parseConfig";
	private final static String CONFIG_CHARSET_NAME = "configCharsetName";
	private final static String IP_PARAM = "${ip}";
	private final static String SUCCESS = "SUCCESS";

	private List<String> ips = new ArrayList<>();
	private String curSetIp;
	private int curSetIpIndex = 0;
	private Connection connection;

	Timer timer = new Timer();

	private final static Log LOGGER = LogFactory.getLog(HeartBeat.class);

	static {
		loadConfig();
	}

	/**
	 * 读取配置
	 */
	private static void loadConfig() {
		FileInputStream inputStream = null;
		try {
			conf = new Properties();
			inputStream = new FileInputStream(FILE_NAME);
			conf.load(inputStream);
		} catch (Exception ex) {
			LOGGER.error(FILE_NAME + " not exists!", ex);
		} finally {
			close(inputStream);
		}
	}

	/**
	 * 关闭
	 *
	 * @param closeable
	 *            Closeable
	 */
	private static void close(Closeable closeable) {
		try {
			if (Objects.nonNull(closeable)) {
				closeable.close();
			}
		} catch (Exception ex) {
			LOGGER.error(closeable + " closeable fail!", ex);
		}
	}

	/**
	 * 解析配置文件中的ip:port，如果没有配置，设置问本机
	 */
	private void parseHostName() {
		try {
			if (StringUtils.isBlank(conf.getProperty(PARSE_CONFIG))) {
				this.ips.add(conf.getProperty(HOST_NAME));
				return;
			}
			File parseFile = new File(conf.getProperty(PARSE_CONFIG));
			if (!parseFile.exists()) {
				this.ips.add(conf.getProperty(HOST_NAME));
				return;
			}

			String context = FileUtils.readFileToString(parseFile, conf.getProperty(CONFIG_CHARSET_NAME));
			parseIps(context);
		} catch (Exception ex) {
			LOGGER.error("parseHostName fail!", ex);
		}
	}

	/**
	 * 解析Nigx中的ip:port
	 *
	 * @param context
	 *            context
	 */
	private void parseIps(String context) {
		try {
			String format = conf.getProperty(PARSE_CONFIG_FORMAT);
			format = StringUtils.isBlank(format) ? DEFAULT_FORMAT : format;
			Map<String, Class<ParseIpPort>> map = ReadConfigClass.<ParseIpPort> readClass("parse");
			LOGGER.info(PARSE_CONFIG_FORMAT + " 支持的有:" + map.keySet());
			Class<ParseIpPort> cls = map.get(format);
			ParseIpPort parseIpPort = cls.newInstance();
			this.ips = parseIpPort.parse(context);
		} catch (Exception e) {
			LOGGER.error("parseIps fail!", e);
		}
	}

	/**
	 * 数据库连接
	 */
	private void getDataBaseConnection() {
		try {
			File file = new File(conf.getProperty(DB_JAR));
			URLClassLoader loader = new URLClassLoader(new URL[] { file.toURI().toURL() });

			Class cls = loader.loadClass(conf.getProperty(DB_DRIVER));
			Driver driver = (Driver) cls.newInstance();

			Properties info = new Properties();
			info.put("user", conf.getProperty(DB_USER_NAME));
			info.put("password", conf.getProperty(DB_PASSWORD));

			this.connection = driver.connect(conf.getProperty(DB_URL), info);
			this.connection.setAutoCommit(true);
		} catch (Exception ex) {
			LOGGER.error("getDataBaseConnection fail!", ex);
		}
	}

	/**
	 * 关闭数据库连接
	 */
	private void closeDataBaseConnection() {
		try {
			if (!Objects.isNull(this.connection)) {
				this.connection.close();
				this.connection = null;
			}
		} catch (Exception ex) {
			LOGGER.error("closeDataBaseConnection fail!", ex);
		}
	}

	/**
	 * 找到一个可用的http，且未被配置
	 *
	 * @return ip
	 */
	private String getNetWorkIp() {
		String http_url = conf.getProperty(HTTP_URL);
		URL url = null;
		String data = null;
		BufferedReader reader = null;
		int j = this.ips.size();
		this.curSetIpIndex = j <= this.curSetIpIndex ? 0 : this.curSetIpIndex;
		for (; this.curSetIpIndex < j; this.curSetIpIndex++) {
			try {
				LOGGER.info("check ip:Port :" + this.ips.get(this.curSetIpIndex));
				url = new URL(http_url.replace(IP_PARAM, this.ips.get(this.curSetIpIndex)));
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				data = reader.readLine().toUpperCase();
				LOGGER.info("response context: " + data);
				if (data.contains(SUCCESS)) {
					if (!this.ips.get(this.curSetIpIndex).equals(curSetIp)) {
						return this.ips.get(this.curSetIpIndex);
					}
					return null;
				}
			} catch (Exception ex) {
				LOGGER.error(this.ips.get(this.curSetIpIndex) + " connection fail!", ex);
			} finally {
				close(reader);
			}
		}
		return null;
	}

	/**
	 * 更新IP:Port数据
	 *
	 * @param ipPort
	 *            ipPort
	 */
	private void updateIpPort(String ipPort) {
		getDataBaseConnection();
		try {
			StringBuilder stringBuilder = new StringBuilder("UPDATE");
			stringBuilder.append(" ").append(conf.getProperty(DB_TABLE)).append(" SET ")
					.append(conf.getProperty(DB_COLUMN)).append("='").append(ipPort).append("'");
			LOGGER.info(stringBuilder.toString());

			PreparedStatement statement = this.connection.prepareStatement(stringBuilder.toString());
			LOGGER.info("update ip:Port :" + ipPort);
			statement.executeUpdate();

			curSetIp = ipPort;
		} catch (Exception ex) {
			LOGGER.error("update ip:Port fail!", ex);
		} finally {
			closeDataBaseConnection();
		}
	}

	private void execute0() {
		LOGGER.info("start execute.....");
		final int beatDelayTime = Integer.parseInt(conf.getProperty(BEAT_DELAY_TIME));
		final int beatGapTime = Integer.parseInt(conf.getProperty(BEAT_GAP_TIME));

		parseHostName();
		LOGGER.info("all ip:" + ips.toString());

		this.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				LOGGER.info("start heart beat.....");
				String ip = getNetWorkIp();
				if (Objects.nonNull(ip)) {
					updateIpPort(ip);
				}
			}
		}, beatDelayTime, beatGapTime);

	}

	/**
	 * 执行心跳检查
	 */
	public void execute() {
		execute0();

		WatchConfig watchConfig = new WatchConfig();
		File file = new File("");
		LOGGER.info("watch dir : " + file.getAbsolutePath());
		watchConfig.watch(file.getAbsolutePath(), "conf.properties");
		while (true) {
			if (watchConfig.getChange()) {
				LOGGER.info("restart heart beat...");
				this.timer.cancel();
				this.timer = new Timer();
				this.ips.clear();
				loadConfig();
				execute0();
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				LOGGER.info("restart heart beat fail !", e);
			}
		}
	}

	public static void main(String[] args) {
		HeartBeat heartBeat = new HeartBeat();
		heartBeat.execute();
	}

}
