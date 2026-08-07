package dev.luisbretones.vigia.check;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Éxito si el servicio responde 2xx o 3xx dentro del timeout. No sigue
 * redirecciones: un 3xx ya demuestra que el servicio está vivo.
 */
public final class HttpCheck implements Check {

	private final URI target;
	private final Duration timeout;
	private final HttpClient client;

	public HttpCheck(String target, Duration timeout) {
		if (target == null || target.isBlank()) {
			throw new IllegalArgumentException("target http vacío");
		}
		URI uri = URI.create(target.trim());
		String scheme = uri.getScheme();
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			throw new IllegalArgumentException("target http inválido: " + target);
		}
		this.target = uri;
		this.timeout = timeout;
		this.client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	@Override
	public CheckResult run() {
		HttpRequest request = HttpRequest.newBuilder(target).timeout(timeout).GET().build();
		long start = System.nanoTime();
		try {
			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
			long latency = elapsedMillis(start);
			int status = response.statusCode();
			if (status >= 200 && status < 400) {
				return CheckResult.ok(latency, "HTTP " + status);
			}
			return CheckResult.failure(latency, "HTTP " + status);
		} catch (IOException e) {
			return CheckResult.failure(elapsedMillis(start),
					e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return CheckResult.failure(elapsedMillis(start), "check interrumpido");
		}
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}
}
