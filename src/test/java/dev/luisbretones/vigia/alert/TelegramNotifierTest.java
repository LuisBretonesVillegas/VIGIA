package dev.luisbretones.vigia.alert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TelegramNotifierTest {

	static final String TOKEN = "123456:ABC-token-secreto";
	static final String CHAT_ID = "42";

	static HttpServer server;
	static String baseUrl;
	static final AtomicReference<String> lastPath = new AtomicReference<>();
	static final AtomicReference<String> lastBody = new AtomicReference<>();
	static volatile int responseStatus = 200;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			lastPath.set(exchange.getRequestURI().getPath());
			lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(responseStatus, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	@Test
	void enviaChatIdYTextoAlEndpointCorrecto() {
		responseStatus = 200;
		TelegramNotifier notifier = new TelegramNotifier(TOKEN, CHAT_ID, baseUrl);

		notifier.send("🔴 paperless está DOWN & recuperándose");

		assertThat(lastPath.get()).isEqualTo("/bot" + TOKEN + "/sendMessage");
		String decoded = URLDecoder.decode(lastBody.get(), StandardCharsets.UTF_8);
		assertThat(decoded).contains("chat_id=42");
		assertThat(decoded).contains("🔴 paperless está DOWN & recuperándose");
	}

	@Test
	void respuestaNo200LanzaAlertExceptionSinFiltrarElToken() {
		responseStatus = 500;
		TelegramNotifier notifier = new TelegramNotifier(TOKEN, CHAT_ID, baseUrl);

		assertThatExceptionOfType(AlertException.class).isThrownBy(() -> notifier.send("hola"))
				.withMessageContaining("500").withMessageNotContaining(TOKEN);
	}

	@Test
	void servidorCaidoLanzaAlertExceptionSinFiltrarElToken() throws IOException {
		int freePort;
		try (ServerSocket socket = new ServerSocket(0)) {
			freePort = socket.getLocalPort();
		}
		TelegramNotifier notifier = new TelegramNotifier(TOKEN, CHAT_ID, "http://127.0.0.1:" + freePort);

		assertThatExceptionOfType(AlertException.class).isThrownBy(() -> notifier.send("hola"))
				.withMessageNotContaining(TOKEN);
	}

	@Test
	void tokenOChatVaciosSeRechazan() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TelegramNotifier("", CHAT_ID));
		assertThatIllegalArgumentException().isThrownBy(() -> new TelegramNotifier(null, CHAT_ID));
		assertThatIllegalArgumentException().isThrownBy(() -> new TelegramNotifier(TOKEN, ""));
		assertThatIllegalArgumentException().isThrownBy(() -> new TelegramNotifier(TOKEN, null));
	}
}
