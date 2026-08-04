<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: evaluation-ledger-infrastructure (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Adaptador filesystem del registro de evaluaciones costosas. Escribe un archivo por evaluador bajo .content-audit/evaluations/, una entrada por linea, en modo append: cada resultado queda disponible apenas se obtiene (F-EVCOST-R004) y una interrupcion abrupta a lo sumo deja una ultima linea incompleta, que la lectura descarta por no parsear —"se lee completa o no existe"—. El orden de append es la unica fuente del desempate que exige F-EVCOST-R006: con mas de una entrada para la misma clave (evaluador + huella), findLatest devuelve la ULTIMA escrita y history las devuelve todas, de la mas reciente a la mas antigua, cada una con la version que la produjo. Se desempata por posicion en el archivo y no por recordedAt a proposito: el append es monotono y es la misma propiedad sobre la que se apoya la seguridad ante dos corridas simultaneas, mientras que un reloj puede empatar o retroceder; recordedAt queda como dato informativo. Append-only tambien significa que las entradas de versiones anteriores del evaluador conviven como historia consultable y nunca se borran solas (F-EVCOST-R006), y que una re-evaluacion agrega sin destruir y pasa a ganar el desempate (F-EVCOST-R008). Vive aparte de los informes de auditoria a proposito: borrar informes no toca este registro (F-EVCOST-R007).


## Implementations

### FileSystemEvaluationLedger

**Implements:** EvaluationLedger

**Types:** Repository

**Dependencies (constructor injection):**

- `baseDir`: `Path`

**Tests that must pass:**

- should make every appended result readable by a ledger instance opened after the run was interrupted → FEAT-EVCOST/F-EVCOST-R004
- should ignore a truncated trailing entry and report its content as never registered → FEAT-EVCOST/F-EVCOST-R004
- should preserve every result when two runs append to the same evaluator ledger at the same time → FEAT-EVCOST/F-EVCOST-R004
- should keep the results of previous evaluator versions readable in history after a new version records its own → FEAT-EVCOST/F-EVCOST-R006
- should keep every registered result readable after the analysis reports under the same base directory are deleted → FEAT-EVCOST/F-EVCOST-R007
- should keep the earlier entry consultable in history when the same key is recorded again → FEAT-EVCOST/F-EVCOST-R008

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

### From evaluation-ledger-domain

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

### EvaluationSessionFactory (factory)

Methods:

- `open(EvaluationBudget budget,Evaluator evaluator): EvaluationSession`

