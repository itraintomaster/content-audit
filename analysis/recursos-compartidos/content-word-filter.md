# ContentWordFilter

## Proposito

Distingue **palabras de contenido** (content words) de **palabras funcionales** (function words). Usado por:
- `LemmaRecurrence` (para filtrar el GlobalWordPositionTracker)
- `LemmaByLevelAbsence` (para excluir function words del analisis)
- `CourseSentences` (al construir las posiciones globales)

## Logica de Filtrado

Una palabra se considera **content word** si pasa TODOS los siguientes filtros:

```
1. NO es una funcion word explicita
   (the, a, an, is, are, was, were, be, been, being, have, has, had,
    do, does, did, will, would, can, could, shall, should, may, might,
    must, and, but, or, so, if, when, while, because, although, etc.)

2. NO es un verbo auxiliar
   (be, have, do, y sus formas)

3. NO es una palabra extremadamente frecuente
   (frequencyRank != null Y frequencyRank < umbral)
   Nota: el umbral se configura en ContentWordFilterConfig

4. Manejo de proper nouns (nombres propios)
   Segun configuracion, pueden incluirse o excluirse
   POS tag: "PROPN"

5. Manejo de numeros
   Segun configuracion, pueden incluirse o excluirse
   POS tag: "NUM"

6. El POS tag es de contenido:
   Tags validos: NOUN, VERB, ADJ, ADV
   (se excluyen: ADP, AUX, CCONJ, DET, INTJ, PART, PRON, SCONJ, etc.)
```

## POS Tags (Universal Dependencies)

| Tag | Tipo | Es content word? |
|-----|------|-------------------|
| NOUN | Sustantivo | SI |
| VERB | Verbo | SI |
| ADJ | Adjetivo | SI |
| ADV | Adverbio | SI |
| PROPN | Nombre propio | Configurable |
| NUM | Numero | Configurable |
| ADP | Preposicion | NO |
| AUX | Auxiliar | NO |
| CCONJ | Conjuncion coordinante | NO |
| DET | Determinante | NO |
| INTJ | Interjection | NO |
| PART | Particula | NO |
| PRON | Pronombre | NO |
| SCONJ | Conjuncion subordinante | NO |
| PUNCT | Puntuacion | NO |
| SYM | Simbolo | NO |
| X | Otro | NO |

<!-- Cuidado aqui: La decision de excluir verbos auxiliares como "be" y "have" puede causar que estos no se analicen en el LemmaRecurrence. Sin embargo, "be" como verbo principal (ej: "I am happy") tambien se excluiria. SpaCy normalmente distingue "be" como AUX vs VERB, pero no siempre es perfecto. -->

## Lista de Function Words

```
Pronouns: I, you, he, she, it, we, they, me, him, her, us, them
Determiners: the, a, an, this, that, these, those, my, your, his, its, our, their
Prepositions: in, on, at, to, for, with, by, from, of, about, into, through, during, before, after
Conjunctions: and, but, or, so, if, when, while, because, although, unless, until, since
Auxiliaries: be, is, are, was, were, been, being, have, has, had, do, does, did
Modals: will, would, can, could, shall, should, may, might, must
```
