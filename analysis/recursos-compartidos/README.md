# Recursos Compartidos

## Descripcion

Estos son los archivos de datos y configuracion que son utilizados por multiples analyzers o que son fundamentales para el funcionamiento del sistema completo.

---

## Archivos de Datos de Vocabulario

### 1. evp.json (English Vocabulary Profile)

**Ruta original:** `src/main/resources/vocabulary/evp.json`
**Tamano:** ~3.4 MB
**Usado por:** LemmaByLevelAbsence, LemmaCountAnalyzer (a traves de VocabularyLookupService)

Catalogo del English Vocabulary Profile (Cambridge). Define que palabras se esperan en cada nivel CEFR.

**Estructura:** Ver [evp-structure.md](./evp-structure.md)

**Distribucion por nivel:**
| Nivel | Cantidad de palabras |
|-------|---------------------|
| A1 | 784 |
| A2 | 1,594 |
| B1 | 2,937 |
| B2 | 4,164 |
| C1 | 2,410 |
| C2 | 3,807 |
| **Total** | **15,696** |

<!-- Sugerencia: El sistema actual solo usa niveles A1-B2 (4 niveles). Los datos C1 y C2 estan disponibles pero no se procesan. Si se extiende el sistema a niveles avanzados, ya hay datos. -->

### 2. enriched_vocabulary_catalog.json

**Ruta original:** `src/main/resources/vocabulary/enriched_vocabulary_catalog.json`
**Tamano:** ~11 MB
**Usado por:** TODOS los analyzers (a traves de VocabularyLookupService)

Este es el archivo MAS CRITICO. Es una version enriquecida del EVP que combina datos de multiples fuentes:

**Estructura:** Ver [enriched-vocabulary-catalog-structure.md](./enriched-vocabulary-catalog-structure.md)

**Fuentes de datos:**
- English Vocabulary Profile (EVP) -> word, cefrLevel, topic
- COCA Corpus -> frequencyRank, lemmaFrequency, wordFrequency
- SpaCy en_core_web_sm -> lemma, spacyPosTag, spacyTag, isAlpha, isStop

<!-- Cuidado aqui: Este archivo fue generado el 2025-06-22. Si el catalogo EVP se actualiza o si se cambia el modelo de SpaCy, este archivo necesita regenerarse. El proceso de generacion tomo ~554 segundos. -->

### 3. lemmas_20k_words.txt

**Ruta original:** `src/main/resources/vocabulary/lemmas_20k_words.txt`
**Tamano:** ~1.48 MB
**Usado por:** Referencia para rankings de frecuencia COCA

Datos brutos del COCA (Corpus of Contemporary American English) con las top 20,000 lemas y sus formas.

**Estructura:** Ver [lemmas-20k-structure.md](./lemmas-20k-structure.md)

---

## Archivos de Configuracion Transversales

### 4. config.yaml (Configuracion Principal)

**Ruta:** `src/main/resources/pipeline/vocabulary-analyzer/config.yaml`
**Copia completa incluida en cada carpeta de analyzer como config-snippet.yaml**

Este archivo centraliza TODA la configuracion de targets y umbrales para todos los analyzers.

### 5. config.properties (Propiedades de Pipeline)

**Ruta:** `src/main/resources/pipeline/vocabulary-analyzer/config.properties`

Configuracion de infraestructura (MongoDB, estrategia de bandas de frecuencia).

```properties
mongo.container.name=pipeline-container
mongo.image.name=mongo:7.0
mongo.port=27017
mongo.db.name=learney
mongo.restore.path=/Users/josecullen/projects/learney/pipeline/02-no-sentence-marker
mongo.force.restart=true
analyzer.frequency.bands.strategy=quarters
analyzer.frequency.bands.bands=1000,2000,3000,4000
```

### 6. Properties de Analyzers Especificos

| Archivo | Ruta | Uso |
|---------|------|-----|
| critical-absent-lemmas-config.properties | vocabulary/ | Bandas de criticidad por frecuencia |
| global-lemma-distribution-config.properties | vocabulary/ | Configuracion de analisis Zipf |
| lemma-progression-config.properties | vocabulary/ | Rangos de nuevos lemas por nivel |
| level-specific-absent-lemmas-config.properties | vocabulary/ | Thresholds por nivel CEFR |

<!-- Sugerencia: Algunos de estos .properties parecen pertenecer a analyzers que NO estan actualmente activos en el orquestador (como el analisis de Zipf o la progresion de lemas). Verificar cuales se usan efectivamente. -->

---

## Componentes Transversales

### ContentWordFilter

Filtro que distingue palabras de contenido de palabras funcionales. Usado por LemmaRecurrence y LemmaByLevelAbsence.

Ver detalle en [content-word-filter.md](./content-word-filter.md)

### VocabularyLookupService

Servicio central que carga el enriched_vocabulary_catalog.json y proporciona lookups por:
- Palabra -> nivel CEFR
- Nivel -> lista de palabras esperadas
- Palabra -> ranking de frecuencia
- Palabra -> informacion completa (lemma, POS, frecuencia, etc.)

Ver detalle en [vocabulary-lookup-service.md](./vocabulary-lookup-service.md)

### SpaCy (Procesamiento NLP)

Todos los analyzers dependen del procesamiento previo de SpaCy que genera:
- Lematizacion (forma base de cada palabra)
- POS tagging (parte de la oracion)
- Conteo de tokens
- Ranking de frecuencia

Ver detalle en [spacy-processing.md](./spacy-processing.md)

---

## GenAI Implementer (Mejora Automatica)

Ademas del analisis, el sistema incluye un componente de **mejora automatica de oraciones** usando GenAI:

### fix-sentence-prompt.md

**Ruta:** `src/main/resources/vocabulary/implementer/fix-sentence-prompt.md`
**Copia:** Ver [fix-sentence-prompt.md](./fix-sentence-prompt.md)

Prompt utilizado para generar oraciones de reemplazo cuando un analyzer detecta problemas.

### Configuracion GenAI

```yaml
implementer:
  genai:
    provider: lm-studio      # o gemini
    endpoint:
      url: http://localhost:1234/v1/chat/completions
    model: openai/gpt-oss-120b
    max:
      tokens: 16384
    temperature: 0.3
    top:
      p: 0.95
    timeout:
      seconds: 90
```

<!-- Cuidado aqui: La configuracion actual usa LM Studio local con un modelo open-source. Para la migracion, sera necesario definir que provider de GenAI usar. -->
