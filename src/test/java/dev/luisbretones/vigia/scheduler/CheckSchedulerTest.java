package dev.luisbretones.vigia.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.luisbretones.vigia.alert.AlertNotifier;
import dev.luisbretones.vigia.check.CheckResult;
import dev.luisbretones.vigia.config.CheckDefinition;
import dev.luisbretones.vigia.config.CheckType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CheckSchedulerTest {

	@Test
	void ejecutaElCheckYAlertaSegunSuUmbral() {
		List<String> messages = new CopyOnWriteArrayList<>();
		AlertNotifier notifier = messages::add;
		AtomicInteger executions = new AtomicInteger();
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		CheckScheduler scheduler = new CheckScheduler(notifier, executor, definition -> () -> {
			executions.incrementAndGet();
			return CheckResult.failure(1, "fallo simulado");
		});
		CheckDefinition definition = new CheckDefinition("caido", CheckType.HTTP, "https://ejemplo.com", 1, 1);

		try {
			scheduler.start(List.of(definition));

			await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
				assertThat(executions.get()).isGreaterThanOrEqualTo(1);
				assertThat(messages).hasSize(1);
				assertThat(messages.get(0)).contains("caido");
			});
		} finally {
			scheduler.stop();
		}
	}

	@Test
	void stopDetieneLasEjecuciones() throws InterruptedException {
		AtomicInteger executions = new AtomicInteger();
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		CheckScheduler scheduler = new CheckScheduler(message -> {
		}, executor, definition -> () -> {
			executions.incrementAndGet();
			return CheckResult.ok(1, "ok");
		});
		CheckDefinition definition = new CheckDefinition("efimero", CheckType.HTTP, "https://ejemplo.com", 1, 3);

		scheduler.start(List.of(definition));
		await().atMost(Duration.ofSeconds(5)).until(() -> executions.get() >= 1);
		scheduler.stop();

		int after = executions.get();
		Thread.sleep(1500);
		assertThat(executions.get()).isEqualTo(after);
	}
}
