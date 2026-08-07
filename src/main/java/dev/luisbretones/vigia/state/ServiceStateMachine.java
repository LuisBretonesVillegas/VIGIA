package dev.luisbretones.vigia.state;

/**
 * Estado de un servicio vigilado. Un fallo aislado no alerta: se pasa a DOWN (y
 * se emite WENT_DOWN) al acumular {@code failuresBeforeAlert} fallos
 * consecutivos, y se vuelve a UP (con RECOVERED) con el primer éxito tras DOWN.
 */
public final class ServiceStateMachine {

	private final int failuresBeforeAlert;
	private ServiceStatus status = ServiceStatus.UP;
	private int consecutiveFailures = 0;

	public ServiceStateMachine(int failuresBeforeAlert) {
		if (failuresBeforeAlert < 1) {
			throw new IllegalArgumentException("failuresBeforeAlert debe ser >= 1, recibido: " + failuresBeforeAlert);
		}
		this.failuresBeforeAlert = failuresBeforeAlert;
	}

	public synchronized AlertEvent onResult(boolean success) {
		if (success) {
			boolean wasDown = status == ServiceStatus.DOWN;
			status = ServiceStatus.UP;
			consecutiveFailures = 0;
			return wasDown ? AlertEvent.RECOVERED : AlertEvent.NONE;
		}
		consecutiveFailures++;
		if (status == ServiceStatus.DOWN) {
			return AlertEvent.NONE;
		}
		if (consecutiveFailures >= failuresBeforeAlert) {
			status = ServiceStatus.DOWN;
			return AlertEvent.WENT_DOWN;
		}
		status = ServiceStatus.PENDING_DOWN;
		return AlertEvent.NONE;
	}

	public synchronized ServiceStatus status() {
		return status;
	}
}
