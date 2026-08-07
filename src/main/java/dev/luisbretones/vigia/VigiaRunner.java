package dev.luisbretones.vigia;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import dev.luisbretones.vigia.alert.AlertNotifier;
import dev.luisbretones.vigia.alert.LogNotifier;
import dev.luisbretones.vigia.alert.TelegramNotifier;
import dev.luisbretones.vigia.config.CheckDefinition;
import dev.luisbretones.vigia.config.ChecksConfigLoader;
import dev.luisbretones.vigia.scheduler.CheckScheduler;

@Component
class VigiaRunner implements ApplicationRunner, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(VigiaRunner.class);

	private final Environment environment;
	private CheckScheduler scheduler;

	VigiaRunner(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		String configPath = environment.getProperty("vigia.config");
		if (configPath == null || configPath.isBlank()) {
			throw new IllegalStateException(
					"falta la propiedad vigia.config (ruta al checks.yml). Ejemplo: --vigia.config=/opt/vigia/checks.yml");
		}
		List<CheckDefinition> definitions = ChecksConfigLoader.load(Path.of(configPath));
		scheduler = new CheckScheduler(buildNotifier(), Executors.newScheduledThreadPool(2));
		scheduler.start(definitions);
		log.info("Vigía en marcha: {} checks cargados desde {}", definitions.size(), configPath);
	}

	@Override
	public void destroy() {
		if (scheduler != null) {
			scheduler.stop();
		}
	}

	private AlertNotifier buildNotifier() {
		String token = environment.getProperty("VIGIA_TG_TOKEN");
		String chatId = environment.getProperty("VIGIA_TG_CHAT");
		if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
			log.warn("VIGIA_TG_TOKEN/VIGIA_TG_CHAT sin definir: las alertas solo saldrán por el log");
			return new LogNotifier();
		}
		return new TelegramNotifier(token, chatId);
	}
}
