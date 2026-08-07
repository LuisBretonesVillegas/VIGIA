package dev.luisbretones.vigia.check;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PingCheckTest {

	private static boolean pingDisponible() {
		String path = System.getenv("PATH");
		return path != null && Arrays.stream(path.split(":")).anyMatch(dir -> Files.isExecutable(Path.of(dir, "ping")));
	}

	@Test
	void localhostRespondeOk() {
		assumeTrue(pingDisponible(), "no hay binario ping en el PATH");

		CheckResult result = new PingCheck("127.0.0.1", Duration.ofSeconds(5)).run();

		assertThat(result.success()).isTrue();
		assertThat(result.latencyMillis()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void hostInexistenteEsFalloNoExcepcion() {
		assumeTrue(pingDisponible(), "no hay binario ping en el PATH");

		CheckResult result = new PingCheck("host-que-no-existe.invalid", Duration.ofSeconds(2)).run();

		assertThat(result.success()).isFalse();
	}

	@Test
	void targetQueParaceOpcionSeRechaza() {
		// "-f" (flood) o "-c 100000" no deben poder colarse como target
		assertThatIllegalArgumentException().isThrownBy(() -> new PingCheck("-f", Duration.ofSeconds(5)));
		assertThatIllegalArgumentException().isThrownBy(() -> new PingCheck("  -c", Duration.ofSeconds(5)));
	}

	@Test
	void targetVacioSeRechaza() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PingCheck("", Duration.ofSeconds(5)));
		assertThatIllegalArgumentException().isThrownBy(() -> new PingCheck("   ", Duration.ofSeconds(5)));
		assertThatIllegalArgumentException().isThrownBy(() -> new PingCheck(null, Duration.ofSeconds(5)));
	}
}
