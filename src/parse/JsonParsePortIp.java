package parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import config.TextType;

@TextType("json")
public class JsonParsePortIp implements ParseIpPort {

	@Override
	public List<String> parse(String text) {
		List<String> portIps = new ArrayList<String>();
		String str = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map map = mapper.readValue(text, Map.class);
			List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("nodes");
			for (Map<String, Object> m : list) {
				str = m.get("ip") + ":" + m.get("port");
				if (!portIps.contains(str)) {
					portIps.add(str);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(text + ":不是合法的JSON格式！", e);
		}
		return portIps;
	}

//	public static void main(String[] args) {
//		ParseIpPort p = new JsonParsePortIp();
//		List<String> portIps = p.parse(
//				"{ \"name\": \"default\", \"nodes\": [ {\"id\": \"127.0.0.1\", "
//				+ "\"ip\": \"127.0.0.1\", \"port\": 9876 } ,{\"id\": \"127.0.0.1\", "
//				+ "\"ip\": \"127.0.0.1\", \"port\": 9876 } ,{\"id\": \"127.0.0.1\", "
//				+ "\"ip\": \"127.0.0.1\", \"port\": 9816 } ], \"partitions\": "
//				+ "[ { \"id\": 1, \"members\": [ \"127.0.0.1\" ] } ]}");
//		System.out.println(portIps);
//	}

}
