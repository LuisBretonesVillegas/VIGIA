package dev.luisbretones.vigia.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Respaldo cuando no hay Telegram configurado: la alerta solo queda en el log.
 */
public final class LogNotifier implements AlertNotifier {

	private static final Logger log = LoggerFactory.getLogger(LogNotifier.class);

	@Override
	public void send(String message) {
		log.warn("ALERTA (Telegram sin configurar): {}", message);
	}
}
