# CLAUDE.md

Reglas de este proyecto. Se cargan en cada sesión.
Si algo aquí choca con lo que te pido en el chat, gana el chat — pero avísame de que estás
saltándote una regla.

---

## Proyecto

- **Qué es:** Vigía — monitor del homelab ED50. Checks HTTP y ping contra los servicios en
  producción y alertas por Telegram solo en cambio de estado (UP→DOWN y DOWN→UP).
- **Stack:** Java 21, Spring Boot 4.1.0, Maven con wrapper (`./mvnw`).
- **Entrada principal:** `src/main/java/dev/luisbretones/vigia/VigiaApplication.java`
- **Contexto extra:** @README.md
- **Alcance v1:** checks http/ping + alertas de caída y recuperación. Fuera de alcance:
  panel web, métricas, endpoints HTTP propios, heartbeat. No añadas nada de eso.
- **Despliegue final:** jar en un LXC Debian como servicio systemd, usuario sin privilegios.

## Regla de la pizarra

Este es un proyecto de aprendizaje. Claude genera scaffolding, pom, config y unidad
systemd; el motor de checks, el scheduler, la máquina de estados y la lógica de alertas
los escribe Luis a mano. Si te pido que escribas esas clases, recuérdame esta regla
antes de hacerlo.

## Comandos

`java` y `mvn` NO están en el PATH. El único JDK instalado es `~/.jdks/temurin-25.0.4`
(el bytecode sale con target 21 vía `java.version` del pom). Prefija siempre `JAVA_HOME`:

| Acción | Comando |
|---|---|
| Tests (todos) | `JAVA_HOME=$HOME/.jdks/temurin-25.0.4 ./mvnw test` |
| Un solo test | `JAVA_HOME=$HOME/.jdks/temurin-25.0.4 ./mvnw test -Dtest=NombreDelTest` |
| Build (jar + tests) | `JAVA_HOME=$HOME/.jdks/temurin-25.0.4 ./mvnw package` |
| Ejecutar en local | `JAVA_HOME=$HOME/.jdks/temurin-25.0.4 ./mvnw spring-boot:run -Dspring-boot.run.arguments=--vigia.config=./checks.yml` |
| Formatear | `JAVA_HOME=$HOME/.jdks/temurin-25.0.4 ./mvnw spotless:apply` |

Formateador: Spotless con el formatter de Eclipse (`spotless:check` corre en `verify`).
Ojo: los formatters palantir/google NO funcionan con el JDK 25 local (usan APIs internas
de javac eliminadas); el de Eclipse sí porque embebe su propio compilador.
Los tipos los comprueba el compilador en `test-compile`. Usa **siempre** estos comandos. No inventes otros, no instales herramientas
nuevas ni añadas dependencias sin preguntarme antes.
Para iterar rápido ejecuta el test concreto, no la suite entera. La suite entera solo antes
de dar algo por terminado.

## Secretos y configuración

- Los servicios vigilados van en un YAML externo pasado con `--vigia.config=/ruta/checks.yml`.
  Nada hardcodeado. El `checks.yml` real no se versiona; en el repo solo `checks.example.yml`.
- Token y chat de Telegram SOLO por variables de entorno `VIGIA_TG_TOKEN` y `VIGIA_TG_CHAT`.
  Nunca en YAML, código, logs, tests ni mensajes de error.
- Este repo acabará siendo público (portfolio). Nada de tokens ni datos sensibles en
  archivos versionados.

---

## Regla de oro

**Nada está hecho hasta que un comando lo demuestra.**

- No digas "listo", "funciona", "arreglado" o "debería funcionar" sin pegar la salida real
  del comando que lo prueba.
- Muestra evidencia: el comando que ejecutaste y lo que devolvió. No afirmes el éxito, enséñalo.
- Si algo no lo puedes verificar con un comando, dilo explícitamente en vez de asumirlo.

---

## Flujo obligatorio

Para cualquier cambio que no sea trivial (más de un archivo, o no sabrías describir el diff
en una frase):

### 1. Explorar
Lee el código relevante antes de tocar nada. Busca patrones ya existentes en el repo y
síguelos. No inventes una arquitectura paralela.
Si la investigación va a requerir leer muchos archivos, **usa un subagente** para no llenarme
el contexto.

### 2. Planear
Escribe el plan antes de escribir código: qué archivos cambian, qué interfaces, qué queda
**fuera de alcance**, y cómo se verificará al final.
Si el plan tiene decisiones ambiguas, pregúntame **antes** de implementar, no después.

### 3. RED — test que falla primero
- Escribe un test que describa el comportamiento deseado.
- Ejecútalo y **confirma que falla**. Pega la salida.
- Si pasa antes de existir la implementación, el test está mal: bórralo y reescríbelo.
- **NUNCA** escribas implementación antes de tener un test en rojo.

### 4. GREEN — mínimo código para pasar
- Solo lo necesario para poner el test en verde. Nada de "ya que estoy".
- Ejecuta los tests. Todos en verde.

### 5. REFACTOR
- Limpia sin cambiar comportamiento. Tests en verde después de cada paso.

### 6. Rompe tu propio código
Ver la sección **Fase adversarial** más abajo. No te la saltes.

### 7. Revisión
Lanza `/code-review`, o un subagente en contexto limpio que solo vea el diff y el plan.
Instrucción para el revisor: **reporta huecos de corrección o requisitos incumplidos, no
preferencias de estilo.**
Un revisor al que le pides fallos siempre encuentra alguno; no persigas todos o acabaremos
con abstracciones y código defensivo de más.

### 8. Commit
Solo cuando yo lo pida (reviso el diff antes). Mensaje descriptivo, en imperativo,
explicando el *porqué* no el *qué*. Un commit por cambio lógico.

---

## Fase adversarial — intenta romperlo

Cuando los tests estén en verde, **cambia de sombrero**: tu trabajo ahora es demostrar que
el código está mal. Usa el subagente `rompedor` o recorre esta lista tú mismo, y por cada
punto que aplique escribe un test que intente romperlo:

**Entradas**
- Vacío, `null`, cadena vacía, lista vacía, `0`, negativos.
- Valores enormes (desbordamiento, off-by-one en los límites).
- Unicode, saltos de línea, espacios al inicio/final, mayúsculas/minúsculas.
- Tipos incorrectos y entradas malformadas (YAML roto incluido).

**Estado y flujo**
- ¿Qué pasa si se llama dos veces? ¿Es idempotente?
- ¿Y si se llama en orden distinto al esperado?
- Condiciones de carrera, concurrencia, reentrada (el scheduler es concurrente).
- Estado a medio escribir cuando algo falla a mitad: ¿queda consistente?

**Entorno**
- Fallo de red, timeout, respuesta 500, respuesta con formato inesperado.
- Archivo inexistente, sin permisos, ruta con espacios.
- Variable de entorno ausente o vacía.

**Seguridad**
- Secretos o credenciales en el código, en logs o en mensajes de error.
- Datos sensibles expuestos en trazas de error.

**Calidad del test**
- ¿Este test falla si rompo la implementación a propósito? Compruébalo: introduce el fallo,
  verifica que el test se pone rojo, deshazlo.
- Un test tautológico (`esperado = funcion(x); assert resultado == esperado`) es verde por
  construcción y no prueba nada. Bórralo.

**Cada fallo que encuentres se convierte en un test de regresión permanente** en
`src/test/java/dev/luisbretones/vigia/regression/`. No lo arregles en silencio:
enséñame el test rojo antes de arreglarlo.

---

## Cuando algo se rompe

1. **Reproduce primero.** Escribe un test que falle reproduciendo el bug. Sin test que falle
   no hay arreglo.
2. **Causa raíz, no síntoma.** Explícame por qué falla antes de tocarlo. Nada de parches que
   hagan desaparecer el error sin entenderlo.
3. Arregla con el cambio **más pequeño** posible.
4. Ejecuta el test: rojo → verde. Ejecuta la suite completa: sin regresiones.
5. El test se queda en el repo para siempre.

**Si llevas 3 intentos fallidos con el mismo problema: PARA.** No sigas probando variaciones.
Resume qué has intentado, qué has descartado y cuál es tu mejor hipótesis, y pregúntame.

---

## Prohibido

Estas son las formas de aparentar que algo funciona sin que funcione. **No hagas ninguna:**

-  Modificar, relajar o borrar un test para que pase. Si un test falla, el problema es el código.
-  `@Disabled`, comentar tests o bajar el umbral de cobertura.
-  Hardcodear el valor que espera el test en vez de calcularlo.
- `catch` vacío o tragarse excepciones para que el flujo continúe.
-  Suprimir avisos del compilador (`@SuppressWarnings`) sin explicarme por qué.
-  Mockear precisamente aquello que se está probando.
-  Ampliar el alcance: tocar archivos que no tienen que ver con la tarea.
-  Dejar `TODO`, código muerto, o métodos que devuelven valores falsos "de momento".
-  Commitear `.env`, claves, tokens o credenciales.

Si crees que una de estas está justificada, **para y pregúntame**. No la apliques por tu cuenta.

---

## Git

- Rama única: `main`.
- Haz commit al cerrar cada hito lógico, sin preguntar (pedido el 2026-08-07). Push solo
  cuando yo lo pida.
- No hagas `git add .` — añade archivos explícitamente.
- No reescribas historia (`rebase`, `--force`, `reset --hard`) sin que te lo pida.

---

## Definición de HECHO

Antes de decirme que has terminado, comprueba y pégame la salida de todo esto:

- [ ] Tests nuevos escritos **antes** de la implementación y vistos en rojo primero
- [ ] Suite completa en verde (`./mvnw test`)
- [ ] Build correcto (`./mvnw package`)
- [ ] Fase adversarial ejecutada, con los tests de regresión que salieron de ella
- [ ] Diff revisado: nada fuera de alcance, sin secretos, sin código muerto
- [ ] Resumen de qué cambiaste, qué decidiste y qué queda pendiente
