package dev.luisbretones.vigia.check;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HttpCheckTest {

	static HttpServer server;
	static String baseUrl;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ok", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.createContext("/error", exchange -> {
			exchange.sendResponseHeaders(500, -1);
			exchange.close();
		});
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", "/ok");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/slow", exchange -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			exchange.sendResponseHeaders(200, -1);
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
	void respuesta200EsExito() {
		CheckResult result = new HttpCheck(baseUrl + "/ok", Duration.ofSeconds(10)).run();

		assertThat(result.success()).isTrue();
		assertThat(result.message()).contains("200");
		assertThat(result.latencyMillis()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void respuesta500EsFallo() {
		CheckResult result = new HttpCheck(baseUrl + "/error", Duration.ofSeconds(10)).run();

		assertThat(result.success()).isFalse();
		assertThat(result.message()).contains("500");
	}

	@Test
	void redireccion302EsExito() {
		CheckResult result = new HttpCheck(baseUrl + "/redirect", Duration.ofSeconds(10)).run();

		assertThat(result.success()).isTrue();
	}

	@Test
	void servidorInalcanzableEsFalloNoExcepcion() throws IOException {
		int freePort;
		try (ServerSocket socket = new ServerSocket(0)) {
			freePort = socket.getLocalPort();
		}

		CheckResult result = new HttpCheck("http://127.0.0.1:" + freePort + "/", Duration.ofSeconds(2)).run();

		assertThat(result.success()).isFalse();
	}

	@Test
	void timeoutEsFallo() {
		CheckResult result = new HttpCheck(baseUrl + "/slow", Duration.ofMillis(300)).run();

		assertThat(result.success()).isFalse();
		assertThat(result.message()).containsIgnoringCase("timeout");
	}

	@Test
	void targetInvalidoSeRechazaAlConstruir() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpCheck("no es una url", Duration.ofSeconds(10)));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpCheck("ftp://servidor/archivo", Duration.ofSeconds(10)));
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpCheck("", Duration.ofSeconds(10)));
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpCheck(null, Duration.ofSeconds(10)));
	}
}
