<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: evaluation-ledger-domain (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

FEAT-EVCOST. Capacidad general de resultados de evaluacion costosa reutilizables entre analisis. No sabe nada de auditoria, de cursos ni de LLMs: define la identidad de un resultado (evaluador + version + huella del contenido, F-EVCOST-R001), el registro append-only clave->payload opaco, y la coordinacion reanudable de una corrida (consultar vigente -> si falta, evaluar dentro del presupuesto -> registrar apenas se obtiene). Cada consumidor aporta tres cosas y nada mas: como construye el contenido (EvaluationSubject.content), como su evaluador se pronuncia (Evaluator -> EvaluationOutcome) y como interpreta su payload. La huella NO la calcula el consumidor: la deriva la sesion del propio mapa de contenido entregado al evaluador, de modo que F-EVCOST-R002 ("exactamente lo que el evaluador recibe, ni mas ni menos") queda garantizada por construccion y no por disciplina.


## Models

### EvaluationKey (`record`)

| Field | Type |
|-------|------|
| evaluatorId | `String` |
| evaluatorVersion | `String` |
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

- `find(EvaluationKey key): Optional<EvaluationRecord>`
- `append(EvaluationRecord record): void`
- `history(String evaluatorId,String contentFingerprint): List<EvaluationRecord>`

### ContentFingerprinter (port)

Methods:

- `fingerprint(Map<String,String> content): String`

### Evaluator (port)

Methods:

- `evaluatorId(): String`
- `evaluatorVersion(): String`
- `evaluate(EvaluationSubject subject): EvaluationOutcome`

### EvaluationSession (port)

Methods:

- `resolve(EvaluationSubject subject): EvaluationResolution`
- `resolveForced(EvaluationSubject subject): EvaluationResolution`
- `coverage(): EvaluationCoverage`
- `evaluatorVersion(): String`

### EvaluationSessionFactory (factory)

Methods:

- `open(EvaluationBudget budget,Evaluator evaluator): EvaluationSession`

