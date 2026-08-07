package dev.luisbretones.vigia.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.luisbretones.vigia.alert.AlertException;
import dev.luisbretones.vigia.alert.AlertNotifier;
import dev.luisbretones.vigia.check.Check;
import dev.luisbretones.vigia.check.CheckResult;
import dev.luisbretones.vigia.config.CheckDefinition;
import dev.luisbretones.vigia.config.CheckType;
import dev.luisbretones.vigia.state.ServiceStateMachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class MonitorTaskTest {

	static final class RecordingNotifier implements AlertNotifier {
		final List<String> messages = new ArrayList<>();

		@Override
		public void send(String message) {
			messages.add(message);
		}
	}

	private static CheckDefinition definition(int failuresBeforeAlert) {
		return new CheckDefinition("servicio", CheckType.HTTP, "https://ejemplo.com", 60, failuresBeforeAlert);
	}

	private static MonitorTask task(CheckDefinition definition, Check check, AlertNotifier notifier) {
		return new MonitorTask(definition, check, new ServiceStateMachine(definition.failuresBeforeAlert()), notifier);
	}

	@Test
	void alertaDeDownTrasLosFallosConfigurados() {
		RecordingNotifier notifier = new RecordingNotifier();
		Check failing = () -> CheckResult.failure(10, "HTTP 500");
		MonitorTask task = task(definition(2), failing, notifier);

		task.run();
		assertThat(notifier.messages).isEmpty();
		task.run();

		assertThat(notifier.messages).hasSize(1);
		assertThat(notifier.messages.get(0)).contains("servicio").contains("DOWN").contains("HTTP 500");
	}

	@Test
	void caidaSostenidaNoDuplicaAlertas() {
		RecordingNotifier notifier = new RecordingNotifier();
		Check failing = () -> CheckResult.failure(10, "HTTP 500");
		MonitorTask task = task(definition(2), failing, notifier);

		for (int i = 0; i < 10; i++) {
			task.run();
		}

		assertThat(notifier.messages).hasSize(1);
	}

	@Test
	void recuperacionAlertaUnaVez() {
		RecordingNotifier notifier = new RecordingNotifier();
		final boolean[] up = {false};
		Check flappy = () -> up[0] ? CheckResult.ok(5, "HTTP 200") : CheckResult.failure(10, "HTTP 500");
		MonitorTask task = task(definition(2), flappy, notifier);

		task.run();
		task.run();
		up[0] = true;
		task.run();
		task.run();

		assertThat(notifier.messages).hasSize(2);
		assertThat(notifier.messages.get(1)).contains("servicio").contains("recuperado");
	}

	@Test
	void unCheckQueLanzaExcepcionCuentaComoFalloYNoRevientaLaTarea() {
		RecordingNotifier notifier = new RecordingNotifier();
		Check broken = () -> {
			throw new RuntimeException("bug interno");
		};
		MonitorTask task = task(definition(1), broken, notifier);

		assertThatNoException().isThrownBy(task::run);
		assertThat(notifier.messages).hasSize(1);
		assertThat(notifier.messages.get(0)).contains("DOWN");
	}

	@Test
	void unNotifierQueFallaNoRevientaLaTarea() {
		AlertNotifier failingNotifier = message -> {
			throw new AlertException("Telegram caído");
		};
		Check failing = () -> CheckResult.failure(10, "HTTP 500");
		MonitorTask task = task(definition(1), failing, failingNotifier);

		assertThatNoException().isThrownBy(task::run);
		// la tarea sigue funcionando en ejecuciones posteriores
		assertThatNoException().isThrownBy(task::run);
	}

	@Test
	void sinCambioDeEstadoNoSeEnviaNada() {
		RecordingNotifier notifier = new RecordingNotifier();
		Check healthy = () -> CheckResult.ok(5, "HTTP 200");
		MonitorTask task = task(definition(2), healthy, notifier);

		task.run();
		task.run();

		assertThat(notifier.messages).isEmpty();
	}
}
