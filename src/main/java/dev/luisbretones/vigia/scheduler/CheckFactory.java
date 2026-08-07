package dev.luisbretones.vigia.scheduler;

import java.time.Duration;

import dev.luisbretones.vigia.check.Check;
import dev.luisbretones.vigia.check.HttpCheck;
import dev.luisbretones.vigia.check.PingCheck;
import dev.luisbretones.vigia.config.CheckDefinition;

public final class CheckFactory {

	private static final Duration CHECK_TIMEOUT = Duration.ofSeconds(10);

	private CheckFactory() {
	}

	public static Check create(CheckDefinition definition) {
		return switch (definition.type()) {
			case HTTP -> new HttpCheck(definition.target(), CHECK_TIMEOUT);
			case PING -> new PingCheck(definition.target(), CHECK_TIMEOUT);
		};
	}
}
