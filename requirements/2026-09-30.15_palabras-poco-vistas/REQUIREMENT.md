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
nadie sabe si «education» (una oración en B1) importa más que «goalkeeper». José eligió umbrales por banda (8 · 6 · 4 · 3).

## Recorrido de uso (estado actual)

Cómo quien corrige intenta hoy saber qué palabras del nivel conviene repetir:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: `lemma-count` deja por nivel
   su puntaje y la lista de lemas con menos de N oraciones ([F-LCOUNT-R017](#F-LCOUNT-R017)).
2. Pide sus estadísticas → `StatsAnalyzerCommand.showStats("lemma-count", coursePath)`. ✓ Disponible.
3. Decide cuáles repetir primero y dónde. ⚠️ La lista no tiene orden ni banda, no dice en qué temas está cada palabra, y
   no se convierte en nada: el plan no tiene tareas de `lemma-count` ([F-HALL-R004](#F-HALL-R004), ligadura vacía).

**Punto de fricción**: paso 3. La señal existe, pero no llega al semáforo ni a la corrección.

**Cambio mínimo necesario**: que cada palabra poco vista sea un hallazgo que sólo ordena, con su banda y sus temas, en
el orden en que conviene repetirlas, y que el umbral dependa de la banda de la palabra. El plan de hoy no cambia.

## Reglas de Negocio

Grupos: **A**, el umbral y lo que no cambia (R001–R002); **B**, lo que agrega (R003–R007). N es el umbral de una palabra
en `lemma-count`: hoy 4 para todas. Base: análisis 2026-09-30T11-54-02, reproducido exacto con un prototipo (A1 93,9 %,
A2 71,1 %, B1 52,5 %, B2 34,0 %, C1 83,3 %; curso 0,6698448).

### Grupo A — El umbral y lo que no cambia

<a id="F-POCOV-R001"></a>
### Rule[F-POCOV-R001] - El umbral de cada palabra es el de su banda, y «4 para todas» da lo de hoy
**Severity**: critical | **Validation**: ASSUMPTION

> Una palabra es poco vista cuando aparece en menos oraciones del curso que el umbral N de su banda de frecuencia, y su
> puntaje, el de su nivel y el del curso se calculan como hoy con ese N. Invariantes:
> 1. con 4 para todas las bandas, todo da exactamente lo de hoy: `lemma-count` 67,0 % en el curso, 73,9 % de vocabulario,
>    niveles 96,5 · 94,2 · 79,5 · 63,5 y 1.535 palabras poco vistas;
> 2. con los umbrales recomendados, 8 para las 1.000 más comunes, 6 hasta 2.000, 4 hasta 3.000 y 3 más allá o sin puesto,
>    da 66,5 % en el curso, 73,86 % de vocabulario, niveles 96,0 · 93,6 · 78,9 · 63,9 y 1.616 palabras poco vistas;
> 3. la banda de una palabra sale de su puesto COCA en el catálogo EVP, el mismo que muestran las palabras sugeridas.

<details><summary>Detalle</summary>

Mantiene [F-LCOUNT-R001](#F-LCOUNT-R001) a [F-LCOUNT-R019](#F-LCOUNT-R019), con N por banda donde decían «N», y
[F-HALL-R009](#F-HALL-R009). José eligió las bandas el 30/9 ([DOUBT-UMBRALES-POR-BANDA](#DOUBT-UMBRALES-POR-BANDA)) y
las confirma antes de construir: los umbrales son configuración, y volver a 4 para todas es cambiar un solo valor.

| Configuración | A1 | A2 | B1 | B2 | C1 | `lemma-count` curso | Poco vistas | Vocabulario |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 4 para todas (hoy) | 93,9 | 71,1 | 52,5 | 34,0 | 83,3 | 67,0 | 1.535 | 73,92 |
| 8 · 6 · 4 · 3, puesto del catálogo | 90,8 | 67,8 | 49,0 | 36,1 | 88,9 | 66,5 | 1.616 | 73,86 |
| 8 · 6 · 4 · 3, puesto de la primera oración | 90,7 | 67,8 | 49,4 | 36,5 | 75,0 | 63,9 | 1.608 | 73,48 |

El 73,5 que vio José salió de la tercera fila. La diferencia con la segunda es casi toda C1, que tiene 3 palabras y pesa
un quinto del curso: una es «hop», que en realidad es «hoped» mal leído por el lematizador; su primera oración le da el
puesto de «hope» (413, banda de las 1.000) y pide 8, mientras que el catálogo le da el de «hop» (5.603) y pide 3.
Ver [DOUBT-PUESTO-DE-LA-BANDA](#DOUBT-PUESTO-DE-LA-BANDA) y [DOUBT-LEMATIZADOR](#DOUBT-LEMATIZADOR).

</details>

<a id="F-POCOV-R002"></a>
### Rule[F-POCOV-R002] - Sin tareas, como hoy
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `lemma-count` no produce tareas: el plan del mismo análisis sigue teniendo 4.036 tareas, ninguna de `lemma-count`, y
> sus hallazgos sólo ordenan: su ligadura al plan sigue vacía, como en el diseño de FEAT-HALL.

Mantiene [F-RCLA-R001](#F-RCLA-R001) y [F-HALL-R013](#F-HALL-R013); [F-HALL-R004](#F-HALL-R004) rige sólo en la familia
de errores. José decidió el 30/9 que no pida tareas ([DOUBT-TAREAS](#DOUBT-TAREAS)): sus palabras llegan a la corrección
como palabras sugeridas ([F-POCOV-R007](#F-POCOV-R007)).

### Grupo B — Lo que agrega

<a id="F-POCOV-R003"></a>
### Rule[F-POCOV-R003] - Cada palabra poco vista es un hallazgo de su nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En cada nivel con puntaje menor que 1, cada palabra poco vista asignada a ese nivel deja un hallazgo
> ([F-HALL-R001](#F-HALL-R001)): gravedad *baja*, resolución *sólo ordena*, costo *instantáneo*. La evidencia da el
> [lema](glossary:Lema), su categoría, en cuántas oraciones está contra N y su banda. En el curso, cada nivel con puntaje
> menor que 1 deja un hallazgo que resume cuántas palabras de su nivel son poco vistas.

<details><summary>Detalle</summary>

Con 4 para todas son 1.535 palabras poco vistas: A1 62 de 515, A2 418 de 810, B1 688 de 885, B2 366 de 384 y C1 1 de 3;
con las bandas recomendadas, 89, 461, 707, 358 y 1 (1.616). Ejemplos: «education (B1): 1 oración de 4; banda de las
1.000 más comunes»; con bandas, «1 oración de 8». La gravedad es la que decidió José el 30/9 para el resto del
vocabulario. La identidad es lema y categoría ([F-HALL-R014](#F-HALL-R014)). Los lemas sin nivel
([F-LCOUNT-R011](#F-LCOUNT-R011), 473) no dejan hallazgo, como hoy no dejan puntaje. C1 no es un nivel del curso: no tiene
ejercicios propios, así que su palabra poco vista (hoy «hop») se nombra en el hallazgo del curso sobre C1 y no deja uno
propio. Con 4 para todas quedan 1.534 hallazgos de nivel y 5 de curso.

</details>

<a id="F-POCOV-R004"></a>
### Rule[F-POCOV-R004] - La banda de frecuencia ordena qué repetir primero
**Severity**: critical | **Validation**: ASSUMPTION

> Cada nivel publica sus palabras en este orden: 1. por banda COCA, primero las 1.000 más comunes, después hasta 2.000,
> hasta 3.000, más allá y sin puesto; 2. dentro de la banda, primero las de menos oraciones; 3. después, COCA ascendente.
> Cada hallazgo lleva su banda y sus oraciones, así que se ordena igual. El orden no cambia ningún puntaje.

<details><summary>Detalle: cuántas hay en cada banda</summary>

Con 4 para todas y el puesto del catálogo:

| Nivel | 1.000 más comunes | hasta 2.000 | hasta 3.000 | más allá | sin puesto |
|---|---:|---:|---:|---:|---:|
| A1 | 31 | 8 | 6 | 15 | 2 |
| A2 | 71 | 75 | 60 | 190 | 22 |
| B1 | 131 | 152 | 117 | 280 | 8 |
| B2 | 41 | 64 | 53 | 206 | 2 |

Las primeras de B1 son feeling, political, research, human, education, sense y effect, todas en una sola oración; las de
B2, community, policy, nation y source. Son las bandas de FEAT-COCA, las mismas que ordenan las sugeridas y el verde del
semáforo ([F-SLEM-R014](#F-SLEM-R014), [F-SEMAF-R003](#F-SEMAF-R003)). La primera versión de esta tabla tomaba el puesto
de la primera oración (A1 31/8/6/16/1, B1 119/158/119/284/8): ver [DOUBT-PUESTO-DE-LA-BANDA](#DOUBT-PUESTO-DE-LA-BANDA).

El orden de los hallazgos dentro de un nivel lo fija el contrato ([F-HALL-R014](#F-HALL-R014): analizador, regla y
palabra); por eso la prioridad viaja en la tabla del nivel y en la evidencia de cada hallazgo. La lista de sub-expuestos
de [F-LCOUNT-R017](#F-LCOUNT-R017) conserva su orden por oraciones.

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
> curso; meta: cada palabra del nivel en tantas oraciones como pide su banda; resolución sólo ordena; costo instantáneo;
> muestra el umbral de cada banda que usó.

<a id="F-POCOV-R007"></a>
### Rule[F-POCOV-R007] - Las palabras sugeridas usan el umbral de la banda de cada palabra
**Severity**: critical | **Validation**: ASSUMPTION

> En las palabras sugeridas de la corrección ([F-SLEM-R014](#F-SLEM-R014)) y en las palabras escasas que la corrección no
> debe quitar ([F-RCLA-R003b](#F-RCLA-R003b)), el N de cada palabra es el de su banda en `lemma-count`: *poco vista* es
> de 1 a N−1 oraciones y *escasa* de N a 2N−1, y el umbral que se informa con cada palabra es ese N. Invariantes: 1. una
> palabra es poco vista para las sugeridas si y sólo si es un hallazgo de `lemma-count` en su nivel; 2. con 4 para todas,
> las listas son idénticas a las de hoy.

<details><summary>Detalle: el efecto medido</summary>

Decidido por el arquitecto el 30/9, a pedido de José («sé que son sugeridas para hacerlas aparecer»). Mantener 4 en las
sugeridas contradiría al hallazgo: «number» (A1, 5 oraciones de 8) sería poco vista para `lemma-count` y expuesta para
las sugeridas. Con las bandas recomendadas, el grupo de poco vistas de las sugeridas cambia así:

| Nivel | Hoy | Con bandas | Entran | Salen |
|---|---:|---:|---:|---:|
| A1 | 62 | 89 | 33 (number, word, eye, face, white, wife) | 6 (mobile, cow, picnic, cd) |
| A2 | 418 | 461 | 87 (study, area, change, information) | 44 (brush, passport, vocabulary) |
| B1 | 688 | 707 | 52 (president, hand, issue, experience) | 33 (pollution, relative, jewelry) |
| B2 | 366 | 358 | 1 (gather) | 9 (dawn, gardener, elegant) |

Entran palabras comunes vistas 4 a 7 veces y salen palabras raras vistas 3. En la lista de 10 que viaja con cada tarea de
A2 entran real, describe y clearly (6 oraciones de 8); en B1 y B2 no cambia. Mantiene [F-SLEM-R013](#F-SLEM-R013): el
umbral informado y la marca de poco vista siguen coincidiendo.

</details>

## Contexto

La propuesta la llama «Palabras poco vistas»: «cuenta en el puntaje, pero no genera tareas; propongo umbrales por banda».
El problema del curso es repetir poco: 1.535 lemas en menos de 4 oraciones, y en B2, 366 de 384. Con N igual para todas,
«education» pesa lo mismo que «goalkeeper» (las dos B1, en una oración); por eso José eligió el 30/9 que el umbral dependa
de la banda, y la banda ordena qué repetir primero.

Tres cosas anotadas, que este requirement no cambia: 120 de las 1.535 son la misma palabra con otra categoría (need y
visit como sustantivos, [F-LCOUNT-R002](#F-LCOUNT-R002)); el curso promedia cinco niveles, C1 incluido con 3 lemas
([F-LCOUNT-R013](#F-LCOUNT-R013)): sin C1 daría 62,9 % y el vocabulario 73,3 %; y el lematizador compartido lee mal 17
formas en 69 palabras de 68 ejercicios («hated» → hat, «hoped» → hop, «drunk», «swum»), lo que deja en C1 una palabra falsa
([DOUBT-LEMATIZADOR](#DOUBT-LEMATIZADOR)).

**El examen.** No tiene casos en el 077: es vocabulario, no error. Se prueba con los números de
[F-POCOV-R001](#F-POCOV-R001): con 4 para todas, `analyze` antes y después da 67,0 % y 73,9 %, 1.535 poco vistas y 4.036
tareas; con las bandas, los de la segunda fila de su tabla. **Costo**: segundos, en la pasada de hoy.

## Alcance

- **Adentro**: R001 a R007, con los umbrales por banda como configuración.
- **Afuera**: que `lemma-count` pida tareas (José dijo que no); la sobreexposición (FEAT-LCOUNT la dejó afuera); el
  semáforo (FEAT-SEMAF) y cómo la corrección usa la lista (paquete E); arreglar el lematizador compartido.

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
        action: "El plan no tiene tareas de lemma-count y, con 4 para todas, el curso sigue en 67,0 % y el vocabulario en 73,9 %"
        gate: [F-POCOV-R001, F-POCOV-R002]
        result: success
```

## Open Questions

<a id="DOUBT-UMBRALES-POR-BANDA"></a>
### Doubt[DOUBT-UMBRALES-POR-BANDA] - ¿N depende de la banda de frecuencia de la palabra?
**Status**: RESOLVED (2026-09-30)

- [ ] A: no, N = 4 para todas, como hoy; la banda sólo ordena (R004).
- [x] B (recomendada): 8 para las 1.000 más comunes, 6 hasta 2.000, 4 hasta 3.000 y 3 más allá, en puntaje y hallazgos.
- [ ] C: 10, 8, 6 y 4.

**Answer**: **B** (José, 30/9): «bandas 8 · 6 · 4 · 3». Mueve el número del curso, así que José lo confirma antes de
construir; por eso los umbrales son configuración y volver a 4 para todas es cambiar un valor
([F-POCOV-R001](#F-POCOV-R001)). La tabla que José vio, con el puesto de la primera oración, es la de abajo; con el
puesto del catálogo la fila B da 73,86 % de vocabulario, no 73,5 ([DOUBT-PUESTO-DE-LA-BANDA](#DOUBT-PUESTO-DE-LA-BANDA)).

| Opción | A1 | A2 | B1 | B2 | `lemma-count` curso | Poco vistas | Vocabulario |
|---|---:|---:|---:|---:|---:|---:|---:|
| A, hoy | 93,9 | 71,1 | 52,5 | 34,0 | 67,0 | 1.535 | 73,9 |
| B | 90,7 | 67,8 | 49,4 | 36,5 | 63,9 | 1.608 | 73,5 |
| C | 87,7 | 59,1 | 40,1 | 28,0 | 57,6 | 1.826 | 72,6 |

Los valores de B son una propuesta mía, no una medición pedagógica.

<a id="DOUBT-TAREAS"></a>
### Doubt[DOUBT-TAREAS] - ¿lemma-count pide tareas?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): no; su lista ordenada alimenta el verde del semáforo, y la corrección la usa cuando igual tiene que
  cambiar una palabra.
- [ ] B: una tarea por nivel, para una tanda de «reciclar» que sume las primeras de la lista en ejercicios que ya se corrigen.
- [ ] C: una tarea por palabra de las 2.000 más comunes: 562 tareas (A1 39, A2 149, B1 277, B2 97).

**Answer**: **A** (José, 30/9): no piden tareas. Sus palabras ya llegan a la corrección como sugeridas: el orden de seis
grupos las pone primero cuando otra tarea reescribe una oración (F-SLEM), con el umbral de su banda
([F-POCOV-R007](#F-POCOV-R007)).

<a id="DOUBT-PUESTO-DE-LA-BANDA"></a>
### Doubt[DOUBT-PUESTO-DE-LA-BANDA] - ¿De dónde sale el puesto de frecuencia que decide la banda de una palabra?
- [ ] A (recomendada): del catálogo EVP, para el lema y su categoría: el mismo puesto que muestran las sugeridas y el
  semáforo. Con 8 · 6 · 4 · 3, el vocabulario del curso da 73,86 % (se sigue leyendo 73,9).
- [ ] B: de la primera oración del curso donde aparece la palabra, como el prototipo que dio 73,5 %.

**Answer**: Pendiente. Recomiendo A. B depende del orden de los ejercicios y de la forma escrita: «hop», que en realidad
es «hoped», toma el puesto de «hope» y pide 8 oraciones; sólo eso pesa 2,8 puntos en `lemma-count` del curso (66,5
contra 63,9), y los otros niveles compensan 0,1. Los niveles casi no cambian entre A y B (a lo sumo 0,4).

<a id="DOUBT-LEMATIZADOR"></a>
### Doubt[DOUBT-LEMATIZADOR] - ¿Cuándo se arregla el lematizador compartido?
- [ ] A (recomendada): en la ronda 2, como un cambio propio con su antes y después, porque mueve el 73,9.
- [ ] B: ya, sólo en las 17 formas medidas.
- [ ] C: no se arregla.

**Answer**: Pendiente. Recomiendo A. Lee mal 17 formas en 69 palabras de 68 ejercicios. En este paquete: `lemma-count`
tiene en C1 una palabra falsa («hop», de «hoped» y «hoping») que baja el curso de 70,3 % a 67,0 % y el vocabulario de
74,4 % a 73,9 %; la recurrencia queda en 0,08; `off-level-words` tiene 1 falso de 170; el semáforo, 6 rojas de 959.

## References

- **FEAT-LCOUNT** — Conteo, umbral, asignación de nivel, puntaje y lista de sub-expuestos: R001, R003, R004, R005.
- **FEAT-HALL** — Hallazgo, gravedad, ficha, plan, orden de los hallazgos y lo que no cambia: R001, R002, R003, R004, R006.
- **FEAT-SEMAF** — Usa el orden, el umbral y los temas de cada palabra: R004, R005.
- **FEAT-SLEM, FEAT-RCLA** — Palabras sugeridas y palabras escasas, con el umbral de cada palabra: R007.
