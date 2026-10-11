# Examen oficial de Calidad de distractores v2

## Alcance y autorización

José pidió el 11/10/2026 actualizar el entendimiento del tablero HTML y hacer
que Calidad de distractores rinda, aprovechando el trabajo anterior. Este paso
tiene **tope cero de consultas nuevas**. No usa el remanente de ningún plan
anterior, no analiza el curso, no corrige ni publica propuestas.

Se conserva el examen v1 reprobado sin modificar su oro o sus registros.
El corpus v2, su rúbrica y las salidas iniciales se congelaron antes del ciclo
sintético del 9/10. Ese ciclo no aprobó oficialmente al juez; esta operación
completa el recorrido oficial mediante `exam run` y `exam record`.

## Reutilización comprobada

- Fuente: `../2026-10-09-choice-correction-cycle-v2/`; archivo original
  `raw-evidence.tgz`, con 25 juicios de calidad.
- Se recuperan sólo los **16 registros iniciales**, sin cambiar sus bytes:
  misma entrada, versión, proveedor `codex-cli`, modelo `gpt-6.1-sol` y esfuerzo
  `medium`. Los 9 juicios de propuestas corregidas no forman parte del examen.
- `course/` representa exclusivamente las entradas visibles del corpus v2.
  `cases.json` sólo contiene identificadores: nunca oro ni expectativas.
- Un ensayo real del CLI, en una carpeta aislada y sin `--budget`, reutilizó
  **16/16**, cero pendientes y cero consultas nuevas. Su exportación está en
  `rehearsal-verdicts.jsonl`.
- `measured-result.json` aplica la rúbrica preexistente: 9/9 defectos detectados
  con categorías compatibles y 7/7 controles válidos. Exige todos los casos,
  misma versión/runtime, y rechaza resultados sin evaluar o diagnósticos que
  no coincidan con los registros originales.

Esto acredita el examen sintético fijo v2, no generalización al curso ni
aprobación humana de las correcciones del experimento. El resultado del v1
permanece reprobado. La aprobación oficial sólo existe después de que el
servicio acepte el registro para una identidad que haya rendido completo.

## Recorrido de registro, sin modelos

Desde la raíz del repositorio, con JDK 25 y el runtime explícito:

1. Respaldar el registro de exámenes de la campaña y verificar que no haya
   operaciones vivas o duplicadas; guardar las huellas del checkpoint y los
   libros del curso.
2. `prepare_choice_quality_exam.py <campaña> --recover-into` restaura sólo
   `exam_choice-quality-judge.jsonl`. Se niega si ya existe; no toca el registro
   de aprobación ni ningún libro del curso.
3. `exam run multiple-choice-quality --cases <cases.json> --course <course/>`
   sin presupuesto, con una salida nueva. Debe dar 16 con veredicto y cero
   pendientes. El servicio registra RENDIDO completo.
4. Medir esa exportación con `prepare_choice_quality_exam.py <salida-medida>
   --measure <veredictos>`. No cambiar versión, corpus ni rúbrica para aprobar.
5. `exam record <salida-medida>` registra el resultado en esa campaña. La UI
   HTML lo obtiene de `exam status -f json`; releer Exámenes no gasta consultas.
6. Comprobar que checkpoint, decisiones, libros del curso y examen v1 siguen
   idénticos. Si faltara cobertura o cambiara la versión, conservar evidencia y
   pedir un plan nuevo; no abrir consultas automáticamente.

## Registro completado el 11/10

La campaña A1 V11 aceptó **TAKEN** a las 15:30:01 ART y **APPROVED** a las
15:30:20 ART mediante las dos operaciones oficiales, sin presupuesto nuevo.
La identidad efectiva examinada fue `9f164016f0612d83ee29e835741ed05c67362b251c3da164ac69f719ad3cacc4`,
`codex-cli / gpt-6.1-sol / medium`. No se cambió la definición del juez.

- `campaign-verdicts.jsonl`: exportación real del examen completo en la campaña.
- `campaign-measured-result.json`: medición de esa exportación contra el oro v2.
- `campaign-status.json`: estado público posterior de los nueve jueces.
- `campaign-before.sha256` y `campaign-after.sha256`: los **1.917 archivos
  previos idénticos**. Sólo se agregaron el libro de examen de este juez y sus
  registros TAKEN/APPROVED; ningún libro del curso ni checkpoint fue editado.
- La UI HTML del puerto 7800 se recargó y releyó: «9 jueces: 9 aprobados»,
  Calidad de distractores seleccionado, «16 de 16», grupos 9/9 y 7/7.
  No se reinició ni se recompiló el servidor.

Respaldo recuperable anterior:
`/private/tmp/content-audit-choice-quality-official.wjwj0G/campaign-before.tgz`,
SHA-256 `a4f1cdf9fe66396a5194e52aff6db5ad3c8552d170f03a1793347fd2fa49e784`.
El registro histórico v1 de la raíz conserva
`855e5b54cea0c2a5030dfe7cdb99fb7319f4b4d8a56b5ab2d30ddfbc3c95d1ea`;
el checkpoint de Corrección 1 conserva
`a230c57959b2a3fa9136e4bff7c87fefafe9c39476a554f8b2d9529c7de2bbb4`.
Las salidas aisladas de ensayo se mantienen identificadas como ensayo, no
como el registro posterior de la campaña.

Esta aprobación no dispara consultas ni autoriza evaluación, corrección o
publicación del curso. El HTML advierte que el historial no identifica el
runtime actual; para este juez se comprobó el runtime efectivo mediante el
CLI, no inferido sólo a partir del historial.

## Pruebas ejecutadas

12 tests offline de `scripts/exams/test_*.py` aprobados, incluidos 7 nuevos
controles de recuperación/medición, más el ensayo real de 16 casos mediante
el CLI oficial con presupuesto cero. No se cambió ninguna definición de juez
ni se recompiló o reinició el tablero HTML para registrar datos.
