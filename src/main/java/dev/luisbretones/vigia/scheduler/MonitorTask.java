package dev.luisbretones.vigia.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.luisbretones.vigia.alert.AlertNotifier;
import dev.luisbretones.vigia.check.Check;
import dev.luisbretones.vigia.check.CheckResult;
import dev.luisbretones.vigia.config.CheckDefinition;
import dev.luisbretones.vigia.state.AlertEvent;
import dev.luisbretones.vigia.state.ServiceStateMachine;

/**
 * Una ejecución periódica de un check. Nunca deja escapar excepciones: si lo
 * hiciera, el ScheduledExecutorService cancelaría el check para siempre.
 */
final class MonitorTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(MonitorTask.class);

	private final CheckDefinition definition;
	private final Check check;
	private final ServiceStateMachine machine;
	private final AlertNotifier notifier;

	MonitorTask(CheckDefinition definition, Check check, ServiceStateMachine machine, AlertNotifier notifier) {
		this.definition = definition;
		this.check = check;
		this.machine = machine;
		this.notifier = notifier;
	}

	@Override
	public void run() {
		CheckResult result;
		try {
			result = check.run();
		} catch (Exception e) {
			log.error("el check '{}' lanzó una excepción no controlada", definition.name(), e);
			result = CheckResult.failure(0, "excepción en el check: " + e.getClass().getSimpleName());
		}

		AlertEvent event = machine.onResult(result.success());
		log.info("check '{}' -> {} ({} ms, {})", definition.name(), result.success() ? "OK" : "FALLO",
				result.latencyMillis(), result.message());

		if (event == AlertEvent.NONE) {
			return;
		}
		String text = switch (event) {
			case WENT_DOWN -> "🔴 " + definition.name() + " está DOWN tras " + definition.failuresBeforeAlert()
					+ " fallos consecutivos. Último error: " + result.message();
			case RECOVERED ->
				"🟢 " + definition.name() + " se ha recuperado (latencia " + result.latencyMillis() + " ms)";
			case NONE -> throw new IllegalStateException("inalcanzable");
		};
		try {
			notifier.send(text);
		} catch (Exception e) {
			log.error("no se pudo enviar la alerta de '{}': {}", definition.name(), e.getMessage());
		}
	}
}
