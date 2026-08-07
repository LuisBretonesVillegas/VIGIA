package dev.luisbretones.vigia.scheduler;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import dev.luisbretones.vigia.alert.AlertNotifier;
import dev.luisbretones.vigia.check.Check;
import dev.luisbretones.vigia.config.CheckDefinition;
import dev.luisbretones.vigia.state.ServiceStateMachine;

/**
 * Programa cada check con su propio intervalo. Los hilos del executor no son
 * daemon, así que mantienen vivo el proceso tras arrancar Spring.
 */
public final class CheckScheduler {

	private final AlertNotifier notifier;
	private final ScheduledExecutorService executor;
	private final Function<CheckDefinition, Check> checkFactory;

	public CheckScheduler(AlertNotifier notifier, ScheduledExecutorService executor) {
		this(notifier, executor, CheckFactory::create);
	}

	CheckScheduler(AlertNotifier notifier, ScheduledExecutorService executor,
			Function<CheckDefinition, Check> checkFactory) {
		this.notifier = notifier;
		this.executor = executor;
		this.checkFactory = checkFactory;
	}

	public void start(List<CheckDefinition> definitions) {
		for (CheckDefinition definition : definitions) {
			MonitorTask task = new MonitorTask(definition, checkFactory.apply(definition),
					new ServiceStateMachine(definition.failuresBeforeAlert()), notifier);
			executor.scheduleAtFixedRate(task, 0, definition.intervalSeconds(), TimeUnit.SECONDS);
		}
	}

	public void stop() {
		executor.shutdownNow();
	}
}
