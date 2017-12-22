package parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import config.TextType;

@TextType("default")
public class DefaultParsePortIp implements ParseIpPort {

	@Override
	public List<String> parse(String text) {
		Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+)");
		Matcher matcher = pattern.matcher(text);
		List<String> ips = new ArrayList<String>();
		String str = null;
		while (matcher.find()) {
			str = matcher.group();
			if (!ips.contains(str)) {
				ips.add(str);
			}
		}
		return ips;
	}

	// public static void main(String[] args) {
	// ParseIpPort p = new DefaultParsePortIp();
	// System.out.println(p.parse("1.1.1.1:80 2.2.2.2:80 2.2.2.2:80"));
	// }
}
