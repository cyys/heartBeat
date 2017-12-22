package config;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReadConfigClass {
	private static final String FILE_SEP = "file:/";
	private static final String JAVA_CLASS = ".class";
	private static final String JAR_FILE = ".jar!";
	private static final String CUR_PACKAGE = "config/";
	private static final String JAR_FILE_SEP = "!/";

	public static <T> Map<String, Class<T>> readClass(String packageName) {
		Map<String, Class<T>> map = new HashMap<>();
		try {
			String packagePath = packageName.replaceAll("\\.", "/");
			String path = ReadConfigClass.class.getResource("").getFile().replace(CUR_PACKAGE, "");

			path += packagePath;
			List<String> clsList = null;
			if (path.contains(JAR_FILE)) {
				clsList = parseJarFile(path);
			} else {
				clsList = parseDirFile(path, packageName);
			}

			Class cls = null;
			TextType textType = null;
			for (String className : clsList) {
				cls = Class.forName(className);
				if (!cls.isInterface()) {
					textType = (TextType) cls.getAnnotation(TextType.class);
					map.put(textType.value(), cls);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(packageName + ":包不存在！", e);
		}
		return map;
	}

	private static List<String> parseDirFile(String path, String packageName) {
		String className = null;
		File file = new File(path);
		List<String> clsList = new ArrayList<>();
		for (File f : file.listFiles()) {
			className = packageName + "." + f.getName().replace(JAVA_CLASS, "");
			clsList.add(className);
		}
		return clsList;
	}

	private static List<String> parseJarFile(String path) {
		List<String> clsList = new ArrayList<>();
		try {
			String[] params = path.split(JAR_FILE_SEP);
			JarFile jar = new JarFile(new File(params[0].replace(FILE_SEP, "")));
			Enumeration<JarEntry> em = jar.entries();
			String clsName = null;

			while (em.hasMoreElements()) {
				clsName = em.nextElement().getName();
				if (clsName.startsWith(params[1]) && clsName.endsWith(JAVA_CLASS)) {
					clsList.add(clsName.replaceAll("/", ".").replace(JAVA_CLASS, ""));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(path + " :jar文件不存在！", e);
		}
		return clsList;
	}

	// public static void main(String[] args) {
	// System.out.println(readClass("parse"));
	// }
}
