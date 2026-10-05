# ContentAudit — guía de trabajo

ContentAudit es un proyecto Java 17, Maven multimódulo, organizado con límites hexagonales. El código bajo `src/main/java` y `src/test/java` se mantiene manualmente, aunque algunos archivos todavía conserven una anotación histórica `@Generated`.

## Estado de Sentinel

- Sentinel ya no gobierna ni regenera este repositorio.
- No ejecutar `sentinel generate`, `sentinel verify`, `sentinel report` ni aplicar patches de Sentinel.
- `sentinel.yaml`, `.sentinel/`, los snapshots y baselines son material histórico hasta completar su archivo o retiro.
- Las anotaciones `@Generated` de Sentinel son legado y no restringen la edición.
- `sentinel-agents` sigue siendo, temporalmente, una dependencia de runtime para ejecutar agentes y Codex. No confundirla con el generador retirado.

## Reglas de desarrollo

- Respetar los límites de módulos documentados en los `AGENTS.md` locales y en los POMs.
- Usar inyección por constructor para dependencias.
- Preservar las reglas de negocio y trazabilidad de `requirements/**/REQUIREMENT.md`; los `@Tag("F-…")` siguen siendo documentación útil.
- Escribir o ajustar tests junto con el cambio de comportamiento. No hace falta declararlos en `sentinel.yaml`.
- No borrar contratos, tests o requisitos sólo porque hayan sido generados originalmente.
- Preservar cambios ajenos presentes en el worktree.

## Build

- Maven no debe invocar Sentinel ni modificar archivos versionados.
- `mvn test` y `mvn verify` ejecutan el ciclo normal y liviano.
- JaCoCo y PIT quedan disponibles como herramientas explícitas; no se ejecutan automáticamente.
- En esta máquina, usar JDK 25 para las dependencias locales actuales, aunque el código compile con `source/target 17`.

## Procesos con modelos

- No ejecutar jueces, exámenes ni consultas reales sin presupuesto o autorización explícita.
- `codex-cli` con la sesión local de ChatGPT es el proveedor por omisión.
- El operador siempre puede elegir explícitamente otro proveedor y modelo.
- No hacer fallback silencioso entre proveedores.
- Registrar proveedor, modelo y versión efectivos en la procedencia.
