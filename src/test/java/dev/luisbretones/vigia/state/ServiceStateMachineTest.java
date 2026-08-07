package dev.luisbretones.vigia.state;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ServiceStateMachineTest {

	@Test
	void empiezaEnUp() {
		ServiceStateMachine machine = new ServiceStateMachine(3);

		assertThat(machine.status()).isEqualTo(ServiceStatus.UP);
	}

	@Test
	void unFalloAisladoNoAlerta() {
		ServiceStateMachine machine = new ServiceStateMachine(3);

		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.status()).isEqualTo(ServiceStatus.PENDING_DOWN);
	}

	@Test
	void alertaAlAcumularLosFallosConfigurados() {
		ServiceStateMachine machine = new ServiceStateMachine(3);

		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.WENT_DOWN);
		assertThat(machine.status()).isEqualTo(ServiceStatus.DOWN);
	}

	@Test
	void caidaSostenidaNoRepiteLaAlerta() {
		ServiceStateMachine machine = new ServiceStateMachine(3);
		machine.onResult(false);
		machine.onResult(false);
		machine.onResult(false);

		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.status()).isEqualTo(ServiceStatus.DOWN);
	}

	@Test
	void primerExitoTrasDownAlertaRecuperacion() {
		ServiceStateMachine machine = new ServiceStateMachine(3);
		machine.onResult(false);
		machine.onResult(false);
		machine.onResult(false);

		assertThat(machine.onResult(true)).isEqualTo(AlertEvent.RECOVERED);
		assertThat(machine.status()).isEqualTo(ServiceStatus.UP);
	}

	@Test
	void exitoTrasFalloAisladoVuelveAUpSinAlertar() {
		ServiceStateMachine machine = new ServiceStateMachine(3);
		machine.onResult(false);

		assertThat(machine.onResult(true)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.status()).isEqualTo(ServiceStatus.UP);
	}

	@Test
	void elContadorSeReiniciaConCadaExito() {
		ServiceStateMachine machine = new ServiceStateMachine(3);
		machine.onResult(false);
		machine.onResult(false);
		machine.onResult(true);
		machine.onResult(false);
		machine.onResult(false);

		// dos fallos tras el reinicio: aún no llega al umbral de 3
		assertThat(machine.status()).isEqualTo(ServiceStatus.PENDING_DOWN);
		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.WENT_DOWN);
	}

	@Test
	void umbralDeUnoAlertaAlPrimerFallo() {
		ServiceStateMachine machine = new ServiceStateMachine(1);

		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.WENT_DOWN);
		assertThat(machine.status()).isEqualTo(ServiceStatus.DOWN);
	}

	@Test
	void exitosConsecutivosNoAlertan() {
		ServiceStateMachine machine = new ServiceStateMachine(3);

		assertThat(machine.onResult(true)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.onResult(true)).isEqualTo(AlertEvent.NONE);
		assertThat(machine.status()).isEqualTo(ServiceStatus.UP);
	}

	@Test
	void umbralCeroONegativoEsInvalido() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServiceStateMachine(0));
		assertThatIllegalArgumentException().isThrownBy(() -> new ServiceStateMachine(-1));
	}

	@Test
	void trasRecuperarseUnaNuevaCaidaVuelveAAlertar() {
		ServiceStateMachine machine = new ServiceStateMachine(2);
		machine.onResult(false);
		machine.onResult(false);
		machine.onResult(true);

		machine.onResult(false);
		assertThat(machine.onResult(false)).isEqualTo(AlertEvent.WENT_DOWN);
	}
}
