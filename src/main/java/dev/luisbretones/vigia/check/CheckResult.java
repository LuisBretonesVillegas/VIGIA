package dev.luisbretones.vigia.check;

public record CheckResult(boolean success, long latencyMillis, String message) {

	public static CheckResult ok(long latencyMillis, String message) {
		return new CheckResult(true, latencyMillis, message);
	}

	public static CheckResult failure(long latencyMillis, String message) {
		return new CheckResult(false, latencyMillis, message);
	}
}
