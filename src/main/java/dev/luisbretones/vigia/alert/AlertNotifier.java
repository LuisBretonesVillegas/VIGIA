package dev.luisbretones.vigia.alert;

public interface AlertNotifier {

	/**
	 * Envía una alerta. Lanza {@link AlertException} si no se pudo entregar.
	 */
	void send(String message);
}
