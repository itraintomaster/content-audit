# Duplicados por par: cuántos candidatos y qué modelo decide

Medición del 1/10/2026 para FEAT-DUPAR v3 (el paso por pares). Nada de esto consulta a claude -p.

| Archivo | Qué es |
|---|---|
| `src/…/PairCensus.java` | Recorre la foto del 29/9 con las reglas de producción (`DefaultNearDuplicateRules`, `CorrectorMatcher`, `QuizStudentViewRenderer`) y pide a nomic-embed-text («clustering: » + el ejercicio terminado) los vectores de los ejercicios de algún par que el comodín no resuelve. Escribe ids y números, nunca un texto del curso. |
| `src/…/PairViews.java` | Lo que leería el juez (`DefaultSameExerciseQuestion.subjectOf`) para una lista de pares: sirvió para la calibración a mano. |
| `contar_candidatos.py` | Candidatos por umbral de coseno y por nivel, sobre la salida del censo. Resultado en `candidatos-por-umbral.txt`. |
| `calibracion-etiquetas.tsv` | 165 pares al azar por debajo de 0,90 y fuera de la zona de la muestra ciega, con mi lectura: S el mismo ejercicio, D dudoso, N otro. |
| `evaluar_llama3.py`, `llama3-resultados.json` | llama3:8b con la mecánica de sí o no de Naturalidad sobre el corpus inventado del juez: dice «sí» a todo. Los resultados son del corpus del 1/10; después se reemplazaron tres pares (`s05`, `s12`, `d01`) porque la guardia del examen los encontró en ejercicios del examen del 077. |

Cómo correr el censo (desde la raíz, con el proyecto compilado):

```
mvn -o -q compile -Dexec.skip=true
javac -proc:none -d /tmp/census -cp "<target/classes de cada módulo>:<dependencias>" analysis/10-duplicados-por-par/src/com/learney/contentaudit/auditdomain/nearduplicates/*.java
java -cp "/tmp/census:<el mismo classpath>" com.learney.contentaudit.auditdomain.nearduplicates.PairCensus <foto>/db/english-course analysis/recursos-compartidos pares.tsv vectores.json
python3 analysis/10-duplicados-por-par/contar_candidatos.py pares.tsv
```

El censo reproduce lo que midió el paquete B: 24.813 pares que piden lo mismo, 25 del comodín, 45 copias de la versión
reprobada y 696 candidatos sin Ollama.
