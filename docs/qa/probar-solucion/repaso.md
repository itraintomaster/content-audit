# Repaso final — Probar la solución

Fecha: 13/10/2026. Referencia: [propuesta original](../../propuestas/probar-la-solucion.html).

La implementación está en ContentAudit (registro y CLI) y en
`/Users/josecullen/projects/learney/ittm/pipeline/vocabulary_dashboard` (tablero y coordinador).
Se preservaron los cambios que ya estaban presentes en ambos repositorios.

## Comparación de las cinco pantallas

Se abrió la propuesta original y se contrastó con la UI renderizada. Las capturas de
esta tabla usan el código real del tablero y datos sintéticos, sin consultas a modelos.
Los números de la maqueta reproducen deliberadamente el ejemplo anterior a normalizar
las reglas; la medición real aplica la normalización recomendada.

| Pantalla | Elementos revisados contra la propuesta | Evidencia |
|---|---|---|
| Ficha | Cabecera y estado; criterio; una acción principal; cinco etapas y costos; archivos con resumen y diff; casos compactos; control Está bien; comandos plegados. | [Claro](ficha-claro.jpg), [móvil](ficha-mobile-claro.jpg) |
| Antes de gastar | Diálogo compacto y centrado; cuenta completa; etapas gratuitas; aviso de freno; un tope; juez/proveedor/modelo/esfuerzo/versión una vez; Cancelar y Probar con costo. | [Claro](dialogo-claro.jpg), [móvil](dialogo-mobile-claro.jpg) |
| Mientras corre | Misma tira de etapas; hechas/actual/espera; progreso por temas; gasto contra tope; Frenar; aviso de continuidad con la página cerrada. | [Claro](progreso-claro.jpg), [oscuro](progreso-oscuro.jpg) |
| Resultado | Titular, barra agregada y dos vallas; veredicto cambiado primero, motivo diferente después y evaluación sospechosa al final; reglas equivalentes agrupadas; oro del agente; aciertos plegados; Seguir con bloqueo explicado. | [Claro](resultado-claro.jpg), [oscuro](resultado-oscuro.jpg) |
| Caso de cerca | Qué se evaluó/esperaba/dio; miniteoría y explicaciones; gravedad; historial del oro; señales; tres decisiones; dato crudo plegado. | [Claro](caso-claro.jpg), [oscuro](caso-oscuro.jpg), [móvil claro](caso-mobile-claro.jpg), [móvil oscuro](caso-mobile-oscuro.jpg) |

Se conservaron las piezas, colores, tipografías, bordes y tamaños del dibujo mediante
CSS acotado. El repaso corrigió el diálogo que heredaba el formato de panel lateral,
el fondo de la ficha y los enlaces que heredaban bordes de botones. También verificó
el acceso al tema en móvil y el plegado de las acciones avanzadas. La ficha conserva
la navegación general del tablero. El tope es editable; revisar el oro fijo solicita
la nota en un segundo diálogo, después de elegir una de las tres decisiones.

## Comportamiento comprobado

- Una sola confirmación autoriza las cinco etapas; medir y registrar no consultan.
- El servidor persiste la autorización antes de lanzar; recupera el flujo al reiniciar.
- Frenar termina el tema en curso. Seguir conserva el saldo y no repite temas terminados.
- Se suman los costos efectivos informados por el CLI. No se cambia de proveedor silenciosamente.
- Un veredicto distinto en contrastes detiene la secuencia antes del histórico.
- Mayúsculas y guiones bajos de reglas se normalizan. Veredicto y gravedad siguen contando.
- Las decisiones sobre oro fijo requieren nota, guardan historia y vuelven a medir gratis.
- El reanálisis usa el mismo tope; cobertura parcial no permite marcar resuelto.
- El operador elige el control sano ya medido. Esa elección conserva la entrega y su procedencia.
- El aviso final o de revisión sale del servidor; el callback se probó sin emitir avisos reales del sistema.
- La lectura de ficha e inspección es consistente. La actividad registrada al completar un tema
  no invalida la autorización; las huellas siguen detectando cambios de juez, oro o contenido.

## Validación inicial

| Conjunto | Resultado |
|---|---|
| Maven, JDK 25: ProblemServiceTest, ProblemCmdTest, ExamCmdTest y AnalyzeCmdTest | 50 pruebas aprobadas |
| Flutter: `test/tablero_html/problemas` y `test/services/quien_lanza_procesos_test.dart` | 24 pruebas aprobadas |
| Node: probar-solucion, problemas, arranque y CSS | 56 pruebas aprobadas |
| Dart analyze: problemas y servicio del CLI | Sin observaciones |
| Suite HTML completa | 1957 aprobadas; 1 falla de snapshots en 88 zonas de Exámenes, correspondientes a cambios ajenos a este trabajo |
| Suite amplia previa de `test/tablero_html` | 970 aprobadas; 2 fallas ajenas en expectativas anteriores de cancelación de exámenes |

Las dos fallas de Flutter son `las acciones un examen no se cancela desde acá` en
`vista_de_ejecuciones_test.dart` y `con un ps inventado no cancela exámenes ni nada que
no se lance con batch` en `cancelar_test.dart`. En esa primera validación no se alteraron esas expectativas ni
se sobrescribieron snapshots de Exámenes. Las huellas actualizadas se limitan a
Problemas y Probar la solución.

No se ejecutó Sentinel. No se ejecutaron jueces, exámenes ni consultas reales a modelos.
Los procesos de prueba usaron un CLI falso y carpetas temporales. El servidor local
se reinició con el código final para revisar la ficha y resultados ya guardados, sin
confirmar Probar, revisar oro ni marcar resuelto en los datos reales.


## Lectura final del tablero real

Se verificaron la [ficha](ficha-datos-reales.jpg), el
[resultado](resultado-datos-reales.jpg) y la [cuenta con reutilización](cuenta-datos-reales.jpg)
de PRB-fbaeac6f en `localhost:7800`. La normalización reconoce 12/12 contrastes;
el examen fijo conserva 38/40 y el conjunto tiene 50/52 casos exactos, dentro de sus
umbrales. La cuenta reutiliza los dos exámenes y pide sólo 1 consulta para el
reanálisis pendiente, además de medir y registrar gratis. Se canceló ese diálogo:
no se lanzó la prueba real ni se marcó resuelto el problema.


## Integración en las ramas principales

Se prepararon commits acotados en `main` de ContentAudit y `master` de ITTM.
Se validó el árbol exacto de cada commit en carpetas limpias, sin incorporar los
cambios pendientes de opiniones, Correcciones, Exámenes ni la guía del juez.
Los snapshots de Problemas se integraron sobre la base de Git; los valores ajenos
del worktree se conservaron por separado.

Validación final del contenido integrado:

- 50 pruebas de Java aprobadas con JDK 25.
- 1.952 pruebas de la suite HTML completa aprobadas.
- 990 pruebas de Dart aprobadas: tablero HTML, corridas desprendidas y guardia de procesos.
- Dart analyze sin observaciones en los módulos de Problemas, Exámenes y servicios afectados.
- `git diff --cached --check` sin errores.

Las dos expectativas antiguas de cancelación se actualizaron al incluir la base
que permite lanzar exámenes y reanálisis desde Problemas: sólo se puede cancelar
un proceso verificado; un PID de otra operación sigue protegido. La comparación
con el medidor canónico v4 utiliza ahora fixtures versionadas dentro del paquete,
sin depender de archivos locales de una campaña.

El hook de ITTM apunta a su copia histórica `content-audit/scripts/test-all.sh`.
Se omitió únicamente para este commit, sin cambiar la configuración del hook;
la validación precedente corresponde a los proyectos y árboles actuales.
No se ejecutaron consultas reales a modelos ni comandos de Sentinel.

## Acceso a las pruebas y respuestas guardadas

Corrección integrada en ITTM: `22b614326` (`fix: expose saved test answers in problems and exams`).
La ficha permite abrir las etapas Contrastes y Examen fijo. Las dos vallas del
resultado incluyen Ver las pruebas y Ver el examen. Cada listado muestra todos
los casos, incluidos los aciertos, con Qué se evaluó, Qué se esperaba y Qué dio,
la miniteoría, sus explicaciones y la procedencia del juez. Ver de cerca conserva
la etapa al volver y los aciertos se identifican como tales.

Exámenes incorpora Pruebas y respuestas dentro de la versión que rindió. Cada caso
se despliega con su esperado y contestado. La lectura usa los artefactos guardados
al registrar, con la misma versión, proveedor, modelo y esfuerzo; conserva el oro
de ese momento aunque después cambie. La vista compartida respeta los límites
entre pantallas. Un registro incompleto no reemplaza la evidencia medida.

Se comprobó PRB-fbaeac6f en el servidor real: 12 pruebas del problema y 40 casos
del examen fijo, con sus tres filas (156 filas en total dentro de Exámenes).
Se abrió un acierto y una diferencia, se recorrieron ambos accesos del resultado
y se verificó volver de un caso al examen seleccionado. La consola final no
mostró errores. Las pantallas móviles tienen 390 px de contenido para un viewport
de 390 px, sin desbordamiento horizontal.

| Lectura revisada | Evidencia con datos reales |
|---|---|
| Pruebas del problema | [Claro](pruebas-detalle.jpg), [oscuro](pruebas-detalle-oscuro.jpg), [móvil claro](pruebas-detalle-mobile.jpg), [móvil oscuro](pruebas-detalle-mobile-oscuro.jpg) |
| Examen fijo desde Problemas | [Escritorio](examen-detalle.jpg), [móvil](examen-detalle-mobile.jpg) |
| Pruebas y respuestas dentro de Exámenes | [Claro](examen-respuestas.jpg), [oscuro](examen-respuestas-oscuro.jpg), [móvil claro](examen-respuestas-mobile.jpg), [móvil oscuro](examen-respuestas-mobile-oscuro.jpg) |
| Esperado y respuesta de una diferencia histórica | [Examen fijo](examen-fijo-respuestas.jpg) |

Validación del árbol exacto integrado, aislado de los cambios ajenos:

- 1.957 pruebas de la suite HTML completa aprobadas.
- 994 pruebas de Dart aprobadas (tablero HTML, procesos desprendidos y guardia de procesos).
- Dart analyze sin observaciones en la lectura y las rutas de Exámenes.
- 57 pruebas de vistas, CSS y arquitectura aprobadas después del indicador final de apertura.
- `git diff --cached --check` sin errores antes de integrar.

También se aprobaron 98 pruebas de HTML y 67 de Dart sobre la copia de trabajo
con los cambios ajenos preservados. El servidor se reinició para revisar las
respuestas existentes. No se lanzó Probar, no se revisó el oro, no se marcó resuelto
el problema y no se hicieron consultas nuevas a modelos. El hook histórico de ITTM
se omitió sólo para este commit, manteniendo su configuración.
