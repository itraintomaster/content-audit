---
patch: ARCH-REVBYP
requirement: 2026-04-17.01_refiner-revision-bypass
generated: 2026-04-18T00:00:00Z
---

# Tech Spec: Fase de revision (bypass skeleton)

## Introducir el modulo `revision-domain`
La fase de revision es una cuarta etapa del pipeline (`analyze -> refiner plan -> refiner next -> revision`) que consume `CorrectionContext` (refiner-domain) y escribe sobre elementos del curso (course-domain), pero no pertenece a ninguno de los dos. Vive como modulo de dominio propio para mantener el acoplamiento explicito en `dependsOn` y evitar que `refiner-domain` crezca con responsabilidades de ejecucion. Depende de `audit-domain` solo por los tipos comunes (`AuditTarget`, `AuditReportStore`, `DiagnosisKind` via refiner).

```architecture
modules:
  - name: "revision-domain"
    _change: "add"
    description: "Domain module for the revision phase of the refinement pipeline. Consumes refiner-domain (task and CorrectionContext) and course-domain (course entities and the CourseRepository port owned by the caller). Exposes the Reviser/RevisionValidator/RevisionArtifactStore/CourseElementLocator ports plus a RevisionEngineFactory seam that assembles a configured RevisionEngine. The bypass baseline (IdentityReviser + AutoApproveValidator + DefaultCourseElementLocator + DispatchingReviser + DefaultRevisionEngine) lives behind the factory; external modules only see the factory class and the carrier records."
    dependsOn: ["audit-domain", "refiner-domain", "course-domain"]
```

## Declarar el carrier `RevisionProposal` y el snapshot del elemento
R001 describe los 12 campos que identifican univocamente una propuesta. `elementBefore`/`elementAfter` se modelan como `CourseElementSnapshot` en vez de exponer `QuizTemplateEntity` directamente: el snapshot centraliza el direccionamiento (`nodeTarget + nodeId`) junto con la carga, de modo que extender el alcance a otros `NodeKind` en el futuro significa agregar un campo al record, no cambiar cada consumidor. En el base case solo `quiz` esta poblado (scope limit: solo QUIZ es objetivo realista).

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "CourseElementSnapshot"
        _change: "add"
        type: "record"
        fields:
          - { name: "nodeTarget", type: "AuditTarget", description: "QUIZ/KNOWLEDGE/TOPIC/MILESTONE/COURSE" }
          - { name: "nodeId", type: "String", description: "Identifier of the targeted node" }
          - { name: "quiz", type: "QuizTemplateEntity", description: "Full quiz snapshot when nodeTarget=QUIZ; null otherwise" }
      - name: "RevisionProposal"
        _change: "add"
        type: "record"
        fields:
          - { name: "proposalId", type: "String" }
          - { name: "taskId", type: "String" }
          - { name: "planId", type: "String" }
          - { name: "sourceAuditId", type: "String" }
          - { name: "diagnosisKind", type: "DiagnosisKind" }
          - { name: "nodeTarget", type: "AuditTarget" }
          - { name: "nodeId", type: "String" }
          - { name: "elementBefore", type: "CourseElementSnapshot" }
          - { name: "elementAfter", type: "CourseElementSnapshot" }
          - { name: "rationale", type: "String" }
          - { name: "reviserKind", type: "String" }
          - { name: "createdAt", type: "Instant" }
```

## Modelar el veredicto y el resultado del flujo como enums separados
`RevisionVerdict` responde solo a R006 (APPROVED/REJECTED del validator). `RevisionOutcomeKind` es distinto: captura el resultado de todo el flujo, incluyendo abortos pre-propuesta (`NO_REVISER`, `CONTEXT_UNAVAILABLE`, `ELEMENT_NOT_FOUND`) y fallas post-aprobacion (`APPROVED_APPLY_FAILED` de R014). Mantenerlos separados deja claro quien decide cada cosa: el validator produce el veredicto, el engine produce el outcome. `RevisionOutcome` envuelve ambos y adjunta el artefacto persistido (null solo para abortos pre-propuesta, por eso el campo `artifact` es nullable).

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionVerdict"
        _change: "add"
        type: "enum"
        fields:
          - { name: "APPROVED" }
          - { name: "REJECTED" }
      - name: "RevisionOutcomeKind"
        _change: "add"
        type: "enum"
        fields:
          - { name: "APPROVED_APPLIED" }
          - { name: "APPROVED_APPLY_FAILED" }
          - { name: "REJECTED" }
          - { name: "NO_REVISER" }
          - { name: "CONTEXT_UNAVAILABLE" }
          - { name: "ELEMENT_NOT_FOUND" }
      - name: "RevisionArtifact"
        _change: "add"
        type: "record"
        fields:
          - { name: "proposal", type: "RevisionProposal" }
          - { name: "verdict", type: "RevisionVerdict" }
          - { name: "rejectionReason", type: "String" }
          - { name: "outcome", type: "RevisionOutcomeKind" }
      - name: "RevisionOutcome"
        _change: "add"
        type: "record"
        fields:
          - { name: "kind", type: "RevisionOutcomeKind" }
          - { name: "artifact", type: "RevisionArtifact" }
          - { name: "errorMessage", type: "String" }
```

## Extraer `Reviser` como puerto pluggable por `DiagnosisKind`
R003 exige un patron de dispatch analogo al de `CorrectionContextResolver` / `DispatchingCorrectionContextResolver` del refiner. `Reviser` es el puerto individual: `handles(DiagnosisKind)` habilita el registry-por-clave (Strategy Registry by Key) y `reviserKind()` identifica quien produjo la propuesta, dato que persiste en el artefacto (R010). `RevisionValidator` y `RevisionValidatorResult` se separan porque R006 exige mas que un boolean: el veredicto rechazado tiene que explicar su motivo en el artefacto.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "Reviser"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "propose(RefinementTask task, CorrectionContext context, CourseElementSnapshot before): RevisionProposal"
          - signature: "handles(DiagnosisKind kind): boolean"
          - signature: "reviserKind(): String"
      - name: "RevisionValidator"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "validate(RevisionProposal proposal): RevisionValidatorResult"
      - name: "RevisionValidatorResult"
        _change: "add"
        exposes:
          - signature: "verdict(): RevisionVerdict"
          - signature: "rejectionReason(): Optional<String>"
```

## Aislar el direccionamiento de elementos en `CourseElementLocator`
Aplicar la propuesta al curso (R011) exige dos operaciones diferentes: leer el elemento actual (`snapshot`) y devolver un curso nuevo con el elemento reemplazado (`replace`). Separarlas del `RevisionEngine` permite que el engine permanezca agnostico al tipo de nodo: hoy `DefaultCourseElementLocator` maneja QUIZ, manana otro locator podria manejar MILESTONE sin tocar la orquestacion. El contrato devuelve `Optional<CourseElementSnapshot>` para que `ELEMENT_NOT_FOUND` (R005) sea un valor del dominio y no una excepcion.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "CourseElementLocator"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "snapshot(CourseEntity course, AuditTarget target, String nodeId): Optional<CourseElementSnapshot>"
          - signature: "replace(CourseEntity course, CourseElementSnapshot replacement): CourseEntity"
```

## Puerto `RevisionArtifactStore` para persistencia del artefacto
R008/R009 fijan la ruta (`.content-audit/revisions/<planId>/<proposalId>`) y R014 fija el orden (persistir el artefacto **antes** de escribir el curso). El puerto vive en `revision-domain`; su implementacion filesystem (`FileSystemRevisionArtifactStore`) vive en `audit-infrastructure` donde ya conviven los otros adapters filesystem (`FileSystemAuditReportStore`, `FileSystemRefinementPlanStore`). `listByPlan` esta expuesto porque DOUBT-MULTIPLE-REVISIONS autoriza multiples propuestas por tarea y un lector puede necesitar listarlas.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "RevisionArtifactStore"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "save(RevisionArtifact artifact): String"
          - signature: "load(String planId, String proposalId): Optional<RevisionArtifact>"
          - signature: "listByPlan(String planId): List<RevisionArtifact>"
  - name: "audit-infrastructure"
    _change: "modify"
    dependsOn: ["audit-domain", "refiner-domain", "revision-domain"]
    implementations:
      - name: "FileSystemRevisionArtifactStore"
        _change: "add"
        implements: ["RevisionArtifactStore"]
        visibility: "public"
        types: ["Repository"]
```

## Encapsular el engine en el package `engine` con un unico seam publico
El flujo J001 involucra una orquestracion con seis colaboradores (`RefinementPlanStore`, `AuditReportStore`, `CorrectionContextResolver`, `Reviser` dispatcher, `RevisionValidator`, `RevisionArtifactStore`, `CourseRepository`, `CourseElementLocator`). Exponerlos cada uno como constructor publico obligaria al composition root a conocer la receta completa del engine. La solucion es un Factory Seam: `RevisionEngineFactory` + `RevisionEngineConfig` al root del modulo (contrato publico); el package `engine` es `public` para que los otros modulos puedan ver `DefaultRevisionEngineFactory`, pero dentro del package solo esa clase es `public`. `DispatchingReviser`, `IdentityReviser`, `AutoApproveValidator`, `DefaultRevisionValidatorResult`, `DefaultCourseElementLocator` y `DefaultRevisionEngine` son package-private: nadie fuera del package puede construirlas.

Esto corrige el error del patch previo, que declaraba estas implementaciones simultaneamente al root del modulo y dentro del package `engine`, provocando `conflicts with root-level component of the same name` al generar. Aqui las impls viven solo dentro del package.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "RevisionEngine"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "revise(String planId, String taskId, Path coursePath): RevisionOutcome"
      - name: "RevisionEngineFactory"
        _change: "add"
        stereotype: "factory"
        exposes:
          - signature: "create(RevisionEngineConfig config): RevisionEngine"
    patterns:
      - type: "Factory"
        interface: "RevisionEngineFactory"
        implementations: ["DefaultRevisionEngineFactory"]
    packages:
      - name: "engine"
        _change: "add"
        visibility: "public"
        implementations:
          - name: "DefaultRevisionEngineFactory"
            _change: "add"
            visibility: "public"
            implements: ["RevisionEngineFactory"]
            types: ["Component"]
          - name: "IdentityReviser"
            _change: "add"
            implements: ["Reviser"]
            types: ["Component"]
          - name: "AutoApproveValidator"
            _change: "add"
            implements: ["RevisionValidator"]
            types: ["Component"]
          - name: "DefaultRevisionValidatorResult"
            _change: "add"
            implements: ["RevisionValidatorResult"]
          - name: "DispatchingReviser"
            _change: "add"
            implements: ["Reviser"]
            types: ["Component"]
            requiresInject:
              - { name: "byKind", type: "Map<DiagnosisKind,Reviser>" }
              - { name: "fallback", type: "IdentityReviser" }
          - name: "DefaultCourseElementLocator"
            _change: "add"
            implements: ["CourseElementLocator"]
            types: ["Component"]
          - name: "DefaultRevisionEngine"
            _change: "add"
            implements: ["RevisionEngine"]
            types: ["Component"]
            requiresInject:
              - { name: "refinementPlanStore", type: "RefinementPlanStore" }
              - { name: "auditReportStore", type: "AuditReportStore" }
              - { name: "contextResolver", type: "CorrectionContextResolver<CorrectionContext>" }
              - { name: "reviser", type: "Reviser" }
              - { name: "validator", type: "RevisionValidator" }
              - { name: "artifactStore", type: "RevisionArtifactStore" }
              - { name: "courseRepository", type: "CourseRepository" }
              - { name: "elementLocator", type: "CourseElementLocator" }
```

## Config record `RevisionEngineConfig` con defaults de bypass
La factoria recibe un unico `RevisionEngineConfig` en vez de ocho parametros posicionales: mas legible en el composition root y futuro-proof (agregar un puerto opcional no rompe llamadores existentes). Los campos obligatorios (`artifactStore`, `courseRepository`, `refinementPlanStore`, `auditReportStore`, `contextResolver`) son IO que el caller obligatoriamente posee. Los opcionales (`validator`, `elementLocator`) aceptan null y la factoria sustituye los bypass defaults (`AutoApproveValidator`, `DefaultCourseElementLocator`). `revisers` puede venir vacio: la factoria siempre agrega `IdentityReviser` como fallback (R004) envolviendo todo en un `DispatchingReviser`.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionEngineConfig"
        _change: "add"
        type: "record"
        fields:
          - { name: "revisers", type: "Map<DiagnosisKind,Reviser>" }
          - { name: "validator", type: "RevisionValidator" }
          - { name: "artifactStore", type: "RevisionArtifactStore" }
          - { name: "courseRepository", type: "CourseRepository" }
          - { name: "elementLocator", type: "CourseElementLocator" }
          - { name: "refinementPlanStore", type: "RefinementPlanStore" }
          - { name: "auditReportStore", type: "AuditReportStore" }
          - { name: "contextResolver", type: "CorrectionContextResolver<CorrectionContext>" }
```

## Agregar el entry point CLI `refiner revise`
DOUBT-CLI-SURFACE se resuelve en favor de incluir el comando en esta iteracion — sin punto de entrada no hay end-to-end observable. `RefinerReviseCommand` es una `sealed` port al root de `audit-cli` (mismo patron que `RefinerPlanCommand`, `RefinerNextCommand`, `RefinerListCommand`) y `RefinerReviseCmd` es el `@Component` picocli en el package `commands`, que inyecta `RevisionEngine` para delegar el flujo. `audit-cli` y `audit-application` suman `revision-domain` a `dependsOn` para ver el puerto y el factory.

```architecture
modules:
  - name: "audit-application"
    _change: "modify"
    dependsOn: ["audit-domain", "course-domain", "refiner-domain", "course-infrastructure", "nlp-infrastructure", "vocabulary-infrastructure", "audit-infrastructure", "revision-domain"]
  - name: "audit-cli"
    _change: "modify"
    dependsOn: ["audit-application", "audit-domain", "course-domain", "course-infrastructure", "nlp-infrastructure", "vocabulary-infrastructure", "audit-infrastructure", "refiner-domain", "revision-domain"]
    interfaces:
      - name: "RefinerReviseCommand"
        _change: "add"
        stereotype: "port"
        sealed: true
        exposes:
          - signature: "revise(String planId, String taskId): Integer"
    packages:
      - name: "commands"
        _change: "modify"
        implementations:
          - name: "RefinerReviseCmd"
            _change: "add"
            implements: ["RefinerReviseCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            types: ["Component"]
            requiresInject:
              - { name: "revisionEngine", type: "RevisionEngine" }
```
