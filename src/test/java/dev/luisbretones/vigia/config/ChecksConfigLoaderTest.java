package dev.luisbretones.vigia.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ChecksConfigLoaderTest {

	@TempDir
	Path tempDir;

	private Path yaml(String content) throws IOException {
		Path file = tempDir.resolve("checks.yml");
		Files.writeString(file, content);
		return file;
	}

	@Test
	void cargaUnYamlValido() throws IOException {
		Path file = yaml("""
				checks:
				  - name: paperless
				    type: http
				    target: https://ejemplo.com
				    interval_seconds: 60
				    failures_before_alert: 3
				  - name: gateway
				    type: ping
				    target: 192.168.1.1
				    interval_seconds: 30
				    failures_before_alert: 2
				""");

		List<CheckDefinition> definitions = ChecksConfigLoader.load(file);

		assertThat(definitions).hasSize(2);
		assertThat(definitions.get(0))
				.isEqualTo(new CheckDefinition("paperless", CheckType.HTTP, "https://ejemplo.com", 60, 3));
		assertThat(definitions.get(1)).isEqualTo(new CheckDefinition("gateway", CheckType.PING, "192.168.1.1", 30, 2));
	}

	@Test
	void archivoInexistenteDaErrorClaro() {
		Path missing = tempDir.resolve("no-existe.yml");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(missing))
				.withMessageContaining("no-existe.yml");
	}

	@Test
	void yamlRotoDaErrorClaro() throws IOException {
		Path file = yaml("checks: [ { name: 'sin cerrar ");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file))
				.withMessageContaining("YAML inválido");
	}

	@Test
	void sinClaveChecksDaError() throws IOException {
		Path file = yaml("otra_clave: 1");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file));
	}

	@Test
	void listaVaciaDaError() throws IOException {
		Path file = yaml("checks: []");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file));
	}

	@Test
	void checkSinTargetDaError() throws IOException {
		Path file = yaml("""
				checks:
				  - name: cojo
				    type: http
				    interval_seconds: 60
				    failures_before_alert: 3
				""");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file))
				.withMessageContaining("cojo");
	}

	@Test
	void intervaloCeroONegativoDaError() throws IOException {
		Path file = yaml("""
				checks:
				  - name: rapido
				    type: http
				    target: https://ejemplo.com
				    interval_seconds: 0
				    failures_before_alert: 3
				""");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file))
				.withMessageContaining("interval_seconds");
	}

	@Test
	void intervaloNoEnteroDaError() throws IOException {
		Path file = yaml("""
				checks:
				  - name: raro
				    type: http
				    target: https://ejemplo.com
				    interval_seconds: sesenta
				    failures_before_alert: 3
				""");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file))
				.withMessageContaining("interval_seconds");
	}

	@Test
	void typeDesconocidoDaError() throws IOException {
		Path file = yaml("""
				checks:
				  - name: misterio
				    type: tcp
				    target: 192.168.1.1
				    interval_seconds: 60
				    failures_before_alert: 3
				""");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file))
				.withMessageContaining("tcp");
	}

	@Test
	void nombreDuplicadoDaError() throws IOException {
		Path file = yaml("""
				checks:
				  - name: repe
				    type: http
				    target: https://uno.com
				    interval_seconds: 60
				    failures_before_alert: 3
				  - name: repe
				    type: http
				    target: https://dos.com
				    interval_seconds: 60
				    failures_before_alert: 3
				""");

		assertThatExceptionOfType(ConfigException.class).isThrownBy(() -> ChecksConfigLoader.load(file))
				.withMessageContaining("repe");
	}
}
