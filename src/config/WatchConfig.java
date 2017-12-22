package config;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;

public class WatchConfig {
	private volatile Boolean isChange = Boolean.FALSE;

	public void watch(String dir, String... fileNames) {

		Thread watchDog = new Thread(() -> {
			FileSystem fileSystem = FileSystems.getDefault();
			try {
				WatchService watchService = fileSystem.newWatchService();
				Path path = Paths.get(dir);
				path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
				WatchKey watchKey = null;
				List<String> watchFiles = Arrays.asList(fileNames);
				do {
					watchKey = watchService.take();
					List<WatchEvent<?>> ws = watchKey.pollEvents();
					for (WatchEvent<?> wk : ws) {
						if (watchFiles.isEmpty() || watchFiles.contains(wk.context().toString())) {
							isChange = Boolean.valueOf(Boolean.TRUE);
//							System.out.println(isChange);
						}
					}
				} while (watchKey.reset());
			} catch (Exception e) {
				new RuntimeException("监控文件失败！", e);
			}
		});

		watchDog.start();
	}

	public Boolean getChange() {
		Boolean bool = isChange;
		isChange = Boolean.FALSE;
		return bool;
	}

//	public static void main(String[] args) {
//		WatchConfig watchConfig = new WatchConfig();
//		File f = new File("C:\\Users\\pros_cy\\Desktop\\heartbeat");
//		watchConfig.watch(f.getAbsolutePath());
//	}

}
