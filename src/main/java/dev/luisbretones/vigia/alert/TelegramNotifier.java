package dev.luisbretones.vigia.alert;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envía alertas con el método sendMessage del Bot API de Telegram. El token
 * jamás debe aparecer en logs ni en mensajes de excepción.
 */
public final class TelegramNotifier implements AlertNotifier {

	public static final String DEFAULT_API_BASE = "https://api.telegram.org";
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final String token;
	private final String chatId;
	private final URI endpoint;
	private final HttpClient client;

	public TelegramNotifier(String token, String chatId) {
		this(token, chatId, DEFAULT_API_BASE);
	}

	TelegramNotifier(String token, String chatId, String apiBase) {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("token de Telegram vacío");
		}
		if (chatId == null || chatId.isBlank()) {
			throw new IllegalArgumentException("chat ID de Telegram vacío");
		}
		this.token = token;
		this.chatId = chatId.trim();
		this.endpoint = URI.create(apiBase + "/bot" + token + "/sendMessage");
		this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	}

	@Override
	public void send(String message) {
		String body = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8) + "&text="
				+ URLEncoder.encode(message, StandardCharsets.UTF_8);
		HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new AlertException(
						"Telegram devolvió HTTP " + response.statusCode() + ": " + redact(truncate(response.body())));
			}
		} catch (IOException e) {
			throw new AlertException("fallo de red enviando a Telegram: " + redact(e.getMessage()));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AlertException("envío a Telegram interrumpido");
		}
	}

	private String redact(String text) {
		return text == null ? "" : text.replace(token, "***");
	}

	private static String truncate(String text) {
		if (text == null) {
			return "";
		}
		return text.length() > 200 ? text.substring(0, 200) + "…" : text;
	}
}
