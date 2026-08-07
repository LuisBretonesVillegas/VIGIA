package dev.luisbretones.vigia.check;

/**
 * Un check de vivacidad contra un servicio. Las implementaciones nunca lanzan
 * por un fallo del servicio vigilado: eso es un {@link CheckResult} de fallo.
 */
public interface Check {

	CheckResult run();
}
