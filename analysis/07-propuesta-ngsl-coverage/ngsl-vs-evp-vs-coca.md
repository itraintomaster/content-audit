# NGSL vs EVP vs COCA — Comparación de Fuentes

## Resumen

| Aspecto | EVP | COCA | NGSL |
|---------|-----|------|------|
| **Qué es** | Catálogo de vocabulario por nivel CEFR | Corpus de frecuencia de palabras | Lista de vocabulario esencial para L2 |
| **Quién lo hizo** | Cambridge University Press | Brigham Young University | Browne & Culligan (académicos) |
| **Tamaño del corpus** | Basado en Cambridge Learner Corpus | 1 billón de palabras | 273 millones (subset Cambridge Corpus) |
| **Palabras** | 15,696 (A1-C2) | Top 20,000 lemmas | 2,809 lemmas |
| **Orientación** | ¿Qué enseñar en cada nivel? | ¿Qué tan frecuente es una palabra? | ¿Qué palabras son esenciales para L2? |
| **CEFR-aligned** | Sí (nativo) | No (es un corpus general) | Parcialmente (97.2% mapeable a CEFR via EVP) |
| **Costo** | Libre para uso no comercial | Datos de frecuencia gratis | Open source, gratis |
| **Actualización** | Continua | Continua | 2023 (v1.2) |

---

## Qué Pregunta Responde Cada Uno

```
EVP:  "¿Qué palabras debe conocer un estudiante de nivel B1?"
      -> Lista curricular: estas son las palabras del nivel

COCA: "¿Cuánto se usa 'cat' en el inglés real?"
      -> Ranking: 'cat' es la palabra #2145 más frecuente

NGSL: "Si un estudiante conoce estas 2,809 palabras, ¿cuánto entiende?"
      -> Cobertura: entiende el 92% del inglés que va a encontrar
```

---

## Cómo se Complementan en el Sistema

```
                    ¿Qué enseñar?          ¿Están bien distribuidas?     ¿Cuánto cubre?
                         |                          |                         |
                         v                          v                         v
                        EVP                       COCA                      NGSL
                         |                          |                         |
                         v                          v                         v
                  LemmaAbsence              CocaBuckets              NGSLCoverage
                  (¿falta algo             (¿la frecuencia           (¿el curso prepara
                   del currículo?)           es la correcta?)          para el mundo real?)
```

Cada fuente ataca una dimensión diferente:

1. **EVP** → Completitud curricular (micro: ¿tiene las palabras del nivel?)
2. **COCA** → Distribución de dificultad (micro: ¿las palabras son de la frecuencia correcta para el nivel?)
3. **NGSL** → Utilidad práctica (macro: ¿el curso prepara al estudiante para entender inglés real?)

---

## Solapamiento entre Fuentes

### EVP ∩ NGSL
- De las 2,809 palabras NGSL, **2,723 (97.2%)** están en el EVP
- Las 86 palabras NGSL que NO están en el EVP son generalmente palabras funcionales o de muy alta frecuencia que el EVP no cataloga por nivel
- **Implicación:** El mapeo NGSL -> CEFR ya existe de facto a través del EVP

### COCA ∩ NGSL
- Todas las palabras NGSL tienen ranking COCA (la NGSL se construyó usando corpus analysis)
- La correlación entre ranking NGSL y ranking COCA es alta pero no perfecta — la NGSL usa criterios adicionales de "utilidad para L2" que van más allá de la frecuencia bruta

### EVP ∩ COCA
- Ya integradas en `enriched_vocabulary_catalog.json`
- El catálogo enriquecido tiene CEFR level (EVP) + frequencyRank (COCA) para cada palabra

---

## Ejemplo Concreto

La palabra **"between"**:

| Fuente | Dato |
|--------|------|
| EVP | Nivel A2, preposición |
| COCA | Ranking #85 (muy frecuente) |
| NGSL | Ranking ~#45 (esencial) |

**Con los analyzers actuales:**
- CocaBuckets: la clasificaría en "top1k" (bien para A2)
- LemmaAbsence: verificaría que aparece en A2
- SentenceLength: irrelevante para esta métrica

**Con NGSLCoverage:**
- Si "between" NO aparece en el curso -> hueco crítico (top 500 NGSL)
- Impacta directamente la cobertura acumulativa
- Se reporta como gap en el vocabulario esencial

<!-- Sugerencia: El valor real del NGSL analyzer es que prioriza por IMPORTANCIA PARA EL ESTUDIANTE, no por nivel curricular ni por frecuencia bruta. "between" (#45 NGSL) es más importante de cubrir que una palabra COCA #85 que no está en NGSL. -->

---

## Recomendación de Uso Conjunto

| Analyzer | Fuente primaria | Rol | Audiencia |
|----------|----------------|-----|-----------|
| LemmaAbsence | EVP | Control curricular | Diseñadores |
| CocaBuckets | COCA | Control de dificultad | Diseñadores |
| **NGSLCoverage** | NGSL | Medición de utilidad | Diseñadores + negocio |
| LemmaRecurrence | COCA + SpaCy | Control de espaciado | Diseñadores |
| SentenceLength | SpaCy | Control de complejidad | Diseñadores |

**Los tres primeros forman un triángulo de calidad:**
- EVP asegura que **no falta nada del currículo**
- COCA asegura que **la dificultad progresa bien**
- NGSL asegura que **el resultado es útil en la práctica**
