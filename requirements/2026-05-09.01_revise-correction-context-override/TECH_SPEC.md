---
patch: ARCH-REVCTX-001
requirement: 2026-05-09.01_revise-correction-context-override
generated: 2026-05-09T15:00:00Z
---

# Tech Spec: Override del correctionContext en revise

## Declarar Jackson como dependencia formal de revision-domain

El parser del override convierte JSON crudo en un CorrectionContext ya validado. Hasta hoy Jackson viajaba transitivamente desde Spring Boot solo en audit-infrastructure; con FEAT-REVCTX el parsing entra al dominio de revision como capacidad de primera clase. Fijamos 2.17.2, alineada al BOM que rige los stores filesystem.

```architecture
dependencies:
  - alias: "jackson-databind"
    artifact: "com.fasterxml.jackson.core:jackson-databind:2.17.2"
    scope: "compile"
    _change: "modify"
    provides:
      - type: "ObjectMapper"
        package: "com.fasterxml.jackson.databind"
      - type: "JsonNode"
        package: "com.fasterxml.jackson.databind"
      - type: "JsonProcessingException"
        package: "com.fasterxml.jackson.core"
modules:
  - name: "revision-domain"
    _change: "modify"
    uses: ["jackson-databind"]
```

## Agregar supports(DiagnosisKind) a CorrectionContextResolver

R004 exige rechazar override cuando el DiagnosisKind no tiene contrato. La unica fuente de verdad de "este kind tiene contexto" es el conjunto de resolvers registrados en DispatchingCorrectionContextResolver. Exponer supports en el port elimina la duplicacion: el engine pregunta una vez antes de invocar al parser.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    interfaces:
      - name: "CorrectionContextResolver"
        _change: "modify"
        exposes:
          - signature: "supports(DiagnosisKind kind): boolean"
            _change: "add"
```

## Trazabilidad del override en RevisionArtifact

R002 #3 exige distinguir si el contexto consumido provino del derivador o del override. La opcion A de DOUBT-OVERRIDE-AUDITABILITY fija maxima trazabilidad: snapshot crudo del JSON mas un enum binario. Va en RevisionArtifact (no en RevisionProposal) porque el origen del contexto es metadata del flujo, no del producto de la estrategia.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "CorrectionContextSource"
        type: "enum"
        fields:
          - name: "DERIVED"
          - name: "OVERRIDE"
      - name: "RevisionArtifact"
        _change: "modify"
        fields:
          - name: "contextSource"
            type: "CorrectionContextSource"
            _change: "add"
          - name: "contextOverridePayload"
            type: "String"
            _change: "add"
```

## Encapsular el override en un carrier propio

Pasar el CorrectionContext parseado y el JSON crudo como dos parametros separados a traves del engine genera puntos de re-acoplo. CorrectionContextOverride los liga: cuando el engine ve este carrier, el contexto ya pasaron validacion estructural y sanity check de coherencia (nodeId + diagnosisKind), y el rawPayload es el snapshot canonico para auditoria.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "CorrectionContextOverride"
        type: "record"
        fields:
          - name: "context"
            type: "CorrectionContext"
          - name: "rawPayload"
            type: "String"
```

## Modelar el rechazo del override como excepcion tipada

R003 y la decision sobre coherencia exigen que ciertos errores de input aborten la invocacion antes de invocar a la estrategia, sin persistir artefacto ni tocar el curso. OverrideRejectedException es la senal canonica del rechazo: la lanza el parser, la atrapa el engine y el outcome resultante es OVERRIDE_INVALID.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "OverrideRejectedException"
        type: "exception"
        message: "correctionContext override rejected: %s"
        fields:
          - name: "reason"
            type: "String"
```

## Outcomes terminales OVERRIDE_INVALID y OVERRIDE_NOT_APPLICABLE

R003 y R004 producen modos de fallo que el CLI necesita distinguir. Promoverlos a constantes del enum los pone en pie de igualdad con STRATEGY_FAILED y NO_ACTIVE_STRATEGY y deja claro en el contrato que ninguno persiste artefacto ni modifica curso ni tarea.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionOutcomeKind"
        _change: "modify"
        fields:
          - name: "OVERRIDE_INVALID"
            _change: "add"
          - name: "OVERRIDE_NOT_APPLICABLE"
            _change: "add"
```

## Crear el port CorrectionContextOverrideParser y ocultar el adapter

El parser deserializa JSON con Jackson, valida la estructura por DiagnosisKind y verifica la coherencia minima. Los tres viven juntos dentro del paquete interno contextoverride, expuestos tras un unico port en el modulo raiz. Public Port / Hidden Adapter: ningun modulo externo puede instanciar DefaultCorrectionContextOverrideParser directamente.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "CorrectionContextOverrideParser"
        stereotype: "port"
        exposes:
          - signature: "parse(String rawJson, DiagnosisKind expectedDiagnosisKind, String expectedNodeId): CorrectionContextOverride"
            throws: ["OverrideRejectedException"]
    packages:
      - name: "contextoverride"
        visibility: "internal"
        implementations:
          - name: "DefaultCorrectionContextOverrideParser"
            visibility: "public"
            implements: ["CorrectionContextOverrideParser"]
            requiresInject:
              - name: "objectMapper"
                type: "ObjectMapper"
              - name: "validators"
                type: "Map<DiagnosisKind,CorrectionContextStructuralValidator>"
```

## Separar la validacion estructural por DiagnosisKind

Cada DiagnosisKind con contexto tiene su contrato (FEAT-RCLA, FEAT-RCSL). Mezclar todas las validaciones en un unico parser crece linealmente con cada nuevo kind. Extraer CorrectionContextStructuralValidator registrado por kind permite agregar un validador nuevo sin tocar el parser ni el engine.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "contextoverride"
        interfaces:
          - name: "CorrectionContextStructuralValidator"
            stereotype: "port"
            exposes:
              - signature: "validateAndBuild(JsonNode payload, String expectedNodeId): CorrectionContext"
                throws: ["OverrideRejectedException"]
        implementations:
          - name: "LemmaAbsenceContextStructuralValidator"
            implements: ["CorrectionContextStructuralValidator"]
            requiresInject:
              - name: "objectMapper"
                type: "ObjectMapper"
          - name: "SentenceLengthContextStructuralValidator"
            implements: ["CorrectionContextStructuralValidator"]
            requiresInject:
              - name: "objectMapper"
                type: "ObjectMapper"
```

## Agregar el overload con override a RevisionEngine

R001 #1 exige que la firma sin override quede intacta. Modelar el override como parametro opcional con null confunde el contrato; dos firmas distintas hacen explicito que existe un camino con derivacion y otro con override. La nueva firma declara OverrideRejectedException como parte del contrato.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "RevisionEngine"
        _change: "modify"
        exposes:
          - signature: "revise(String planId, String taskId, Path coursePath, String overridePayload): RevisionOutcome"
            _change: "add"
            throws: ["OverrideRejectedException"]
```

## Wireado del parser en DefaultRevisionEngine y RevisionEngineConfig

El engine, segun el overload invocado, decide si llamar al parser y saltearse contextResolver.resolve o seguir el camino historico. RevisionEngineConfig sigue siendo el unico punto donde la composition root pasa colaboradores; el parser es nullable como el deriver y el validator, asi quien no usa la capacidad nueva no esta obligado a construirlo.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionEngineConfig"
        _change: "modify"
        fields:
          - name: "correctionContextOverrideParser"
            type: "CorrectionContextOverrideParser"
            _change: "add"
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "DefaultRevisionEngine"
            _change: "modify"
            requiresInject:
              - name: "correctionContextOverrideParser"
                type: "CorrectionContextOverrideParser"
                _change: "add"
```

## Extender ReviseCommand con los flags de override

Los dos flags mutuamente excluyentes viven en el contrato del ReviseCommand para que la diferencia entre el camino con override y sin override sea visible en la firma del verbo, no escondida en la implementacion picocli. Pasar los dos como String mantiene a audit-cli alejado de Jackson: el JSON crudo (inline o leido del archivo) se delega tal cual al engine.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    interfaces:
      - name: "ReviseCommand"
        _change: "modify"
        exposes:
          - signature: "revise(String taskId, String planId, String correctionContextJson, String correctionContextFilePath): Integer"
            _change: "add"
```
