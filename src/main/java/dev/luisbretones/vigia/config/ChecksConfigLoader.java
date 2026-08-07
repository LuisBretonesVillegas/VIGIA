package dev.luisbretones.vigia.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Carga y valida el YAML de servicios vigilados. Formato:
 *
 * <pre>
 * checks:
 *   - name: paperless
 *     type: http
 *     target: https://ejemplo.com
 *     interval_seconds: 60
 *     failures_before_alert: 3
 * </pre>
 */
public final class ChecksConfigLoader {

	private ChecksConfigLoader() {
	}

	public static List<CheckDefinition> load(Path file) {
		if (!Files.isReadable(file)) {
			throw new ConfigException("no se puede leer el archivo de configuración: " + file);
		}
		Object root;
		try (InputStream in = Files.newInputStream(file)) {
			root = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
		} catch (IOException e) {
			throw new ConfigException("error leyendo " + file + ": " + e.getMessage());
		} catch (YAMLException e) {
			throw new ConfigException("YAML inválido en " + file + ": " + e.getMessage());
		}

		if (!(root instanceof Map<?, ?> rootMap)) {
			throw new ConfigException("el YAML debe tener una clave raíz 'checks': " + file);
		}
		Object checksNode = rootMap.get("checks");
		if (!(checksNode instanceof List<?> entries) || entries.isEmpty()) {
			throw new ConfigException("'checks' debe ser una lista con al menos un check: " + file);
		}

		List<CheckDefinition> definitions = new ArrayList<>();
		Set<String> names = new HashSet<>();
		for (Object entry : entries) {
			if (!(entry instanceof Map<?, ?> map)) {
				throw new ConfigException("cada check debe ser un mapa con name/type/target: " + entry);
			}
			CheckDefinition definition = new CheckDefinition(string(map, "name"), CheckType.from(string(map, "type")),
					string(map, "target"), integer(map, "interval_seconds"), integer(map, "failures_before_alert"));
			if (!names.add(definition.name())) {
				throw new ConfigException("nombre de check duplicado: '" + definition.name() + "'");
			}
			definitions.add(definition);
		}
		return List.copyOf(definitions);
	}

	private static String string(Map<?, ?> map, String key) {
		Object value = map.get(key);
		return value == null ? null : value.toString();
	}

	private static int integer(Map<?, ?> map, String key) {
		Object value = map.get(key);
		if (value instanceof Integer number) {
			return number;
		}
		throw new ConfigException(
				"'" + key + "' falta o no es un entero en el check '" + map.get("name") + "', recibido: " + value);
	}
}
