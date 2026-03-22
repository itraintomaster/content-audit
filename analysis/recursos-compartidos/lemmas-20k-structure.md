# Estructura de lemmas_20k_words.txt

## Ruta Original
`src/main/resources/vocabulary/lemmas_20k_words.txt`

## Fuente
COCA (Corpus of Contemporary American English): www.english-corpora.org/coca
Datos de frecuencia: www.wordfrequency.info

## Formato
Tab-separated values (TSV) con cabecera.

## Estructura

```
lemRank	lemma	PoS	lemFreq	wordFreq	word
1	the	a	50033612	50033323	the
1	the	a	50033612	287	ze
2	be	v	32394756	10093608	is
2	be	v	32394756	6848519	was
2	be	v	32394756	6303682	's
```

## Columnas

| Columna | Tipo | Descripcion |
|---------|------|-------------|
| `lemRank` | Integer | Ranking del lema (1 = mas frecuente) |
| `lemma` | String | Forma base (lema) |
| `PoS` | Char | Parte de la oracion (a=articulo, v=verbo, c=conjuncion, etc.) |
| `lemFreq` | Integer | Frecuencia total del lema en el corpus |
| `wordFreq` | Integer | Frecuencia de esta forma especifica |
| `word` | String | Forma especifica de la palabra |

## Codigos POS

| Codigo | Significado |
|--------|-------------|
| a | Article/Determiner |
| v | Verb |
| c | Conjunction |
| i | Preposition |
| t | To (infinitive marker) |
| n | Noun |
| j | Adjective |
| r | Adverb |
| p | Pronoun |
| m | Number |
| e | Existential |
| x | Not/Negative |

## Notas

- Cada lema tiene multiples filas (una por forma)
- Ejemplo: "be" (rank 2) tiene formas: is, was, 's, be, are, were, 're, been, 'm, being, am, s
- El archivo tiene ~20,000 lemas unicos con sus formas
- Las primeras 8 lineas son comentarios/instrucciones y deben ignorarse al parsear

<!-- Sugerencia: Este archivo se usa como referencia para construir el enriched_vocabulary_catalog.json. Para la migracion, probablemente solo necesites el enriched catalog ya generado, no los datos brutos. -->
