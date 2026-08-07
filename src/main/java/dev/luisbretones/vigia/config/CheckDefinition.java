package dev.luisbretones.vigia.config;

public record CheckDefinition(String name, CheckType type, String target, int intervalSeconds,
		int failuresBeforeAlert) {

	public CheckDefinition {
		if (name == null || name.isBlank()) {
			throw new ConfigException("un check no tiene 'name'");
		}
		name = name.trim();
		if (type == null) {
			throw new ConfigException("el check '" + name + "' no tiene 'type'");
		}
		if (target == null || target.isBlank()) {
			throw new ConfigException("el check '" + name + "' no tiene 'target'");
		}
		target = target.trim();
		if (intervalSeconds < 1) {
			throw new ConfigException(
					"'interval_seconds' del check '" + name + "' debe ser >= 1, recibido: " + intervalSeconds);
		}
		if (failuresBeforeAlert < 1) {
			throw new ConfigException(
					"'failures_before_alert' del check '" + name + "' debe ser >= 1, recibido: " + failuresBeforeAlert);
		}
	}
}
