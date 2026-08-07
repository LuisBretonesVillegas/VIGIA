package dev.luisbretones.vigia.check;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Éxito si {@code ping -c 1} contra el target sale con código 0. Usa el binario
 * del sistema (iputils): ICMP sin privilegios desde Java no es fiable.
 */
public final class PingCheck implements Check {

	private final String target;
	private final Duration timeout;

	public PingCheck(String target, Duration timeout) {
		if (target == null || target.isBlank()) {
			throw new IllegalArgumentException("target de ping vacío");
		}
		String trimmed = target.trim();
		// un target que empiece por '-' se interpretaría como opción de ping
		if (trimmed.startsWith("-")) {
			throw new IllegalArgumentException("target de ping inválido: " + target);
		}
		this.target = trimmed;
		this.timeout = timeout;
	}

	@Override
	public CheckResult run() {
		long waitSeconds = Math.max(1, timeout.toSeconds());
		ProcessBuilder builder = new ProcessBuilder("ping", "-c", "1", "-W", String.valueOf(waitSeconds), target);
		builder.redirectErrorStream(true);
		long start = System.nanoTime();
		try {
			Process process = builder.start();
			boolean finished = process.waitFor(timeout.toMillis() + 2000, TimeUnit.MILLISECONDS);
			long latency = elapsedMillis(start);
			if (!finished) {
				process.destroyForcibly();
				return CheckResult.failure(latency, "ping sin respuesta (timeout)");
			}
			int exitCode = process.exitValue();
			if (exitCode == 0) {
				return CheckResult.ok(latency, "ping OK");
			}
			return CheckResult.failure(latency, "ping devolvió " + exitCode);
		} catch (IOException e) {
			return CheckResult.failure(elapsedMillis(start), "no se pudo ejecutar ping: " + e.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return CheckResult.failure(elapsedMillis(start), "check interrumpido");
		}
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}
}
