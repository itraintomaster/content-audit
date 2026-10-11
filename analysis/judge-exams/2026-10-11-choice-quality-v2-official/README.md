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

## Pruebas

12 tests offline de `scripts/exams/test_*.py` aprobados, incluidos 7 nuevos
controles de recuperación/medición, más el ensayo real de 16 casos mediante
el CLI oficial con presupuesto cero. No se cambió ninguna definición de juez
ni se recompiló o reinició el tablero HTML para registrar datos.
