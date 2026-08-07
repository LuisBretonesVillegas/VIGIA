package dev.luisbretones.vigia.config;

import java.util.Locale;

public enum CheckType {
	HTTP, PING;

	public static CheckType from(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new ConfigException("un check no tiene 'type'");
		}
		return switch (raw.trim().toLowerCase(Locale.ROOT)) {
			case "http" -> HTTP;
			case "ping" -> PING;
			default -> throw new ConfigException("type desconocido: '" + raw + "' (válidos: http, ping)");
		};
	}
}
