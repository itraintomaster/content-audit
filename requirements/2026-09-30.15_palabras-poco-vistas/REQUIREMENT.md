---
feature:
  id: FEAT-POCOV
  code: F-POCOV
  name: Palabras poco vistas, con prioridad por banda de frecuencia
  priority: high
---

# Palabras poco vistas, con prioridad por banda de frecuencia

## TL;DR

**Qué**: `lemma-count` sigue midiendo igual, y ahora dice qué palabra falta repetir, en qué orden y dónde está: cada
palabra poco vista es un hallazgo de su nivel, con su banda de frecuencia y los temas que la usan.

**Por qué**: Cuenta en el 73,9 % (67,0 % en el curso) pero sólo deja un número: 1.535 lemas en menos de 4 oraciones, y
nadie sabe si «education» (una oración en B1) importa más que «goalkeeper». Cambiar su umbral o que pida tareas es de José.

## Recorrido de uso (estado actual)

Cómo quien corrige intenta hoy saber qué palabras del nivel conviene repetir:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: `lemma-count` deja por nivel
   su puntaje y la lista de lemas con menos de N oraciones ([F-LCOUNT-R017](#F-LCOUNT-R017)).
2. Pide sus estadísticas → `StatsAnalyzerCommand.showStats("lemma-count", coursePath)`. ✓ Disponible.
3. Decide cuáles repetir primero y dónde. ⚠️ La lista no tiene orden ni banda, no dice en qué temas está cada palabra, y
   no se convierte en nada: el plan no tiene tareas de `lemma-count` ([F-HALL-R004](#F-HALL-R004), ligadura vacía).

**Punto de fricción**: paso 3. La señal existe, pero no llega al semáforo ni a la corrección.

**Cambio mínimo necesario**: que cada palabra poco vista sea un hallazgo que sólo ordena, con su banda y sus temas, en
el orden en que conviene repetirlas. El umbral, el puntaje y el plan de hoy no cambian.

## Reglas de Negocio

Grupos: **A**, lo que no cambia (R001–R002); **B**, lo que agrega (R003–R006). N es el umbral de `lemma-count`, hoy 4.
Base: análisis 2026-09-30T11-54-02, reproducido exacto con un prototipo (A1 93,9 %, A2 71,1 %, B1 52,5 %, B2 34,0 %).

### Grupo A — Lo que no cambia

<a id="F-POCOV-R001"></a>
### Rule[F-POCOV-R001] - El umbral y el puntaje son los de hoy
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una palabra es poco vista cuando aparece en menos de N oraciones del curso, con el mismo N para todas las bandas; el
> puntaje por palabra, por nivel y del curso se calcula como hoy, y da 67,0 % en el curso y el mismo 73,9 % de vocabulario.

Mantiene [F-LCOUNT-R001](#F-LCOUNT-R001) a [F-LCOUNT-R019](#F-LCOUNT-R019) y [F-HALL-R009](#F-HALL-R009). Umbral por banda:
[DOUBT-UMBRALES-POR-BANDA](#DOUBT-UMBRALES-POR-BANDA).

<a id="F-POCOV-R002"></a>
### Rule[F-POCOV-R002] - Sin tareas, como hoy
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `lemma-count` no produce tareas: el plan del mismo análisis sigue teniendo 4.036 tareas, ninguna de `lemma-count`, y
> sus hallazgos sólo ordenan: su ligadura al plan sigue vacía, como en el diseño de FEAT-HALL.

Mantiene [F-RCLA-R001](#F-RCLA-R001) y [F-HALL-R013](#F-HALL-R013); [F-HALL-R004](#F-HALL-R004) rige sólo en la familia
de errores. Que pida tareas: [DOUBT-TAREAS](#DOUBT-TAREAS).

### Grupo B — Lo que agrega

<a id="F-POCOV-R003"></a>
### Rule[F-POCOV-R003] - Cada palabra poco vista es un hallazgo de su nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En cada nivel con puntaje menor que 1, cada palabra poco vista asignada a ese nivel deja un hallazgo
> ([F-HALL-R001](#F-HALL-R001)): gravedad *baja*, resolución *sólo ordena*, costo *instantáneo*. La evidencia da el
> [lema](glossary:Lema), su categoría, en cuántas oraciones está contra N y su banda. En el curso, cada nivel con puntaje
> menor que 1 deja un hallazgo que resume cuántas palabras de su nivel son poco vistas.

<details><summary>Detalle</summary>

Son 1.535 hallazgos de nivel: A1 62 de 515, A2 418 de 810, B1 688 de 885, B2 366 de 384 y C1 1 de 3. Ejemplos:
«education (B1): 1 oración de 4; banda de las 1.000 más comunes»; «national (A2): 1 de 4; 1.000 más comunes». La gravedad
es la que decidió José el 30/9 para el resto del vocabulario. La identidad es lema y categoría ([F-HALL-R014](#F-HALL-R014)).
Los lemas sin nivel ([F-LCOUNT-R011](#F-LCOUNT-R011), 473) no dejan hallazgo, como hoy no dejan puntaje.

</details>

<a id="F-POCOV-R004"></a>
### Rule[F-POCOV-R004] - La banda de frecuencia ordena qué repetir primero
**Severity**: critical | **Validation**: ASSUMPTION

> Los hallazgos y la lista de cada nivel van ordenados: 1. por banda COCA, primero las 1.000 más comunes, después hasta
> 2.000, hasta 3.000, más allá y sin rango; 2. dentro de la banda, primero las de menos oraciones; 3. después, COCA
> ascendente. El orden no cambia ningún puntaje.

<details><summary>Detalle: cuántas hay en cada banda</summary>

| Nivel | 1.000 más comunes | hasta 2.000 | hasta 3.000 | más allá | sin rango |
|---|---:|---:|---:|---:|---:|
| A1 | 31 | 8 | 6 | 16 | 1 |
| A2 | 71 | 78 | 59 | 188 | 22 |
| B1 | 119 | 158 | 119 | 284 | 8 |
| B2 | 38 | 59 | 56 | 211 | 2 |

Las primeras de B1 son feeling, political, research, human, education, sense y effect, todas en una sola oración; las de
B2, community, policy, nation y source. Son las bandas de FEAT-COCA, las mismas que ordenan las sugeridas y el verde del
semáforo ([F-SLEM-R014](#F-SLEM-R014), [F-SEMAF-R003](#F-SEMAF-R003)).

</details>

<a id="F-POCOV-R005"></a>
### Rule[F-POCOV-R005] - Cada palabra dice en qué temas está
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cada hallazgo nombra los temas cuyos ejercicios usan la palabra, y en cada tema se puede leer qué palabras poco vistas
> de su nivel usa. Una palabra que ningún ejercicio usa no es poco vista: es ausente, de `lemma-absence`.

De las 418 poco vistas de A2, 207 están en un solo tema; «Cuantificadores: con o sin $of$» usa 27 de su nivel. Lo leen
el semáforo ([F-SEMAF-R002](#F-SEMAF-R002)) y la corrección. Mantiene [F-LCOUNT-R003](#F-LCOUNT-R003).

<a id="F-POCOV-R006"></a>
### Rule[F-POCOV-R006] - La ficha la describe con su nombre de siempre
**Severity**: major | **Validation**: AUTO_VALIDATED

> `lemma-count` figura en `get analyzers` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Cada palabra del nivel
> aparece en suficientes oraciones?»; lee las oraciones terminadas de todo el curso; familia vocabulario; evalúa nivel y
> curso; meta: cada palabra del nivel en N oraciones o más; resolución sólo ordena; costo instantáneo; muestra el N usado.

## Contexto

La propuesta la llama «Palabras poco vistas»: «cuenta en el puntaje, pero no genera tareas; propongo umbrales por banda».
El problema del curso es repetir poco: 1.535 lemas en menos de 4 oraciones, y en B2, 366 de 384. Con N igual para todas,
«education» pesa lo mismo que «goalkeeper» (las dos B1, en una oración); por eso la banda ordena ya, y moverla al umbral queda para José.

Dos cosas anotadas, que este requirement no cambia: 120 de las 1.535 son la misma palabra con otra categoría (need y
visit como sustantivos, [F-LCOUNT-R002](#F-LCOUNT-R002)); y el curso promedia cinco niveles, C1 incluido con 3 lemas
([F-LCOUNT-R013](#F-LCOUNT-R013)): sin C1 daría 62,9 % y el vocabulario 73,3 %.

**El examen.** No tiene casos en el 077: es vocabulario, no error. Se prueba con los mismos números: `analyze` antes y
después da 67,0 % y 73,9 %, 1.535 hallazgos y 4.036 tareas. **Costo**: segundos, en la pasada de hoy.

## Alcance

- **Adentro**: R001 a R006.
- **Afuera**: cambiar N, el puntaje o las tareas sin decisión de José (dudas); la sobreexposición (FEAT-LCOUNT la dejó
  afuera); el semáforo (FEAT-SEMAF) y cómo la corrección usa la lista (paquete E).

## User Journeys

### Journey[F-POCOV-J001] - Ver qué palabras del nivel repetir primero
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-POCOV-J001
    name: Ver qué palabras del nivel repetir primero
    flow:
      - id: analizar
        action: "El operador corre analyze con lemma-count sobre el curso"
        gate: [F-POCOV-R001]
        outcomes:
          - when: "El nivel tiene palabras en menos de N oraciones"
            then: hallazgos
          - when: "Todas las palabras del nivel están en N oraciones o más"
            then: nivel_completo
      - id: hallazgos
        action: "El nivel trae un hallazgo por palabra poco vista, con su banda y sus temas, ordenados por banda y oraciones"
        gate: [F-POCOV-R003, F-POCOV-R004, F-POCOV-R005]
        then: sin_cambios
      - id: nivel_completo
        action: "El nivel queda con puntaje 1 y sin hallazgos de lemma-count"
        then: sin_cambios
      - id: sin_cambios
        action: "El puntaje del curso sigue en 67,0 %, el vocabulario en 73,9 % y el plan sin tareas de lemma-count"
        gate: [F-POCOV-R002]
        result: success
```

## Open Questions

<a id="DOUBT-UMBRALES-POR-BANDA"></a>
### Doubt[DOUBT-UMBRALES-POR-BANDA] - ¿N depende de la banda de frecuencia de la palabra?
- [ ] A: no, N = 4 para todas, como hoy; la banda sólo ordena (R004).
- [ ] B (recomendada): 8 para las 1.000 más comunes, 6 hasta 2.000, 4 hasta 3.000 y 3 más allá, en puntaje y hallazgos.
- [ ] C: 10, 8, 6 y 4.

**Answer**: Pendiente. Recomiendo B: exige más a las palabras que más se usan y menos a las raras. Mueve el 73,9 %, así
que necesita tu OK explícito:

| Opción | A1 | A2 | B1 | B2 | `lemma-count` curso | Poco vistas | Vocabulario |
|---|---:|---:|---:|---:|---:|---:|---:|
| A, hoy | 93,9 | 71,1 | 52,5 | 34,0 | 67,0 | 1.535 | 73,9 |
| B | 90,7 | 67,8 | 49,4 | 36,5 | 63,9 | 1.608 | 73,5 |
| C | 87,7 | 59,1 | 40,1 | 28,0 | 57,6 | 1.826 | 72,6 |

Los valores de B son una propuesta mía, no una medición pedagógica.

<a id="DOUBT-TAREAS"></a>
### Doubt[DOUBT-TAREAS] - ¿lemma-count pide tareas?
- [ ] A (recomendada): no; su lista ordenada alimenta el verde del semáforo, y la corrección la usa cuando igual tiene que
  cambiar una palabra.
- [ ] B: una tarea por nivel, para una tanda de «reciclar» que sume las primeras de la lista en ejercicios que ya se corrigen.
- [ ] C: una tarea por palabra de las 2.000 más comunes: 562 tareas (A1 39, A2 149, B1 277, B2 97).

**Answer**: Pendiente. Recomiendo A: no se suman palabras por sumar, y ninguna tarea de C la cierra un solo ejercicio. B
cambia el plan de 4.036 tareas y necesita que el paquete E defina la tanda.

## References

- **FEAT-LCOUNT** — Conteo, umbral, asignación de nivel, puntaje y lista de sub-expuestos: R001, R003, R005.
- **FEAT-HALL** — Hallazgo, gravedad, ficha, plan y lo que no cambia: R001, R002, R003, R006.
- **FEAT-SEMAF** — Usa el orden y los temas de cada palabra: R004, R005.
