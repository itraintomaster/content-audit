<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: evaluation-ledger-domain (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

FEAT-EVCOST. Capacidad general de resultados de evaluacion costosa reutilizables entre analisis. No sabe nada de auditoria, de cursos ni de LLMs: define la identidad de un resultado —evaluador + huella del contenido, y nada mas (F-EVCOST-R001)—, el registro append-only clave->payload opaco, y la coordinacion reanudable de una corrida (consultar lo registrado -> si falta, evaluar dentro del presupuesto -> registrar apenas se obtiene). La version del evaluador NO integra la identidad: es procedencia obligatoria que se registra junto a cada resultado y viaja con el en cada consulta, de modo que ningun cambio de version invalida, borra ni deja pendiente nada (F-EVCOST-R006, F-EVCOST-R008). Cada consumidor aporta tres cosas y nada mas: como construye el contenido (EvaluationSubject.content), como su evaluador se pronuncia (Evaluator -> EvaluationOutcome) y como interpreta su payload. La huella NO la calcula el consumidor: la deriva la sesion del propio mapa de contenido entregado al evaluador, de modo que F-EVCOST-R002 ("exactamente lo que el evaluador recibe, ni mas ni menos") queda garantizada por construccion y no por disciplina.


## Models

### EvaluationKey (`record`)

| Field | Type |
|-------|------|
| evaluatorId | `String` |
| contentFingerprint | `String` |

### EvaluationSubject (`record`)

| Field | Type |
|-------|------|
| subjectRef | `String` |
| content | `Map<String,String>` |

### EvaluationRecord (`record`)

| Field | Type |
|-------|------|
| key | `EvaluationKey` |
| payload | `String` |
| subjectRef | `String` |
| recordedAt | `Instant` |
| evaluatorVersion | `String` |

### EvaluationEmitted (`record`)

| Field | Type |
|-------|------|
| payload | `String` |

### EvaluationNotEmitted (`record`)

| Field | Type |
|-------|------|
| kind | `EvaluationFailureKind` |
| reason | `String` |

### EvaluationFailureKind (`enum`)

| Field | Type |
|-------|------|
| OUTPUT_UNUSABLE | `null` |
| EVALUATOR_UNAVAILABLE | `null` |

### EvaluationResolutionKind (`enum`)

| Field | Type |
|-------|------|
| REUSED | `null` |
| EVALUATED | `null` |
| PENDING | `null` |
| FAILED | `null` |

### EvaluationResolution (`record`)

| Field | Type |
|-------|------|
| kind | `EvaluationResolutionKind` |
| payload | `String` |
| evaluatorVersion | `String` |

### EvaluationCoverage (`record`)

| Field | Type |
|-------|------|
| reached | `int` |
| withResult | `int` |
| evaluatedInRun | `int` |
| reused | `int` |
| pending | `int` |
| failed | `int` |

### EvaluationBudget (`record`)

| Field | Type |
|-------|------|
| maxNewEvaluations | `int` |

## Interfaces

### EvaluationOutcome (port)

### EvaluationLedger (port)

Methods:

- `append(EvaluationRecord record): void`
- `findLatest(EvaluationKey key): Optional<EvaluationRecord>`
- `history(EvaluationKey key): List<EvaluationRecord>`

### ContentFingerprinter (port)

Methods:

- `fingerprint(Map<String,String> content): String`

### Evaluator (port)

Methods:

- `evaluatorId(): String`
- `evaluate(EvaluationSubject subject): EvaluationOutcome`
- `evaluatorVersion(): Optional<String>`

### EvaluationSession (port)

Methods:

- `resolve(EvaluationSubject subject): EvaluationResolution`
- `resolveForced(EvaluationSubject subject): EvaluationResolution`
- `coverage(): EvaluationCoverage`
- `consultedEvaluatorVersion(): Optional<String>`
- `resolvePreferringNew(EvaluationSubject subject): EvaluationResolution`
- `resolveWithoutConsulting(EvaluationSubject subject): EvaluationResolution`

### EvaluationSessionFactory (factory)

Methods:

- `open(EvaluationBudget budget,Evaluator evaluator): EvaluationSession`

