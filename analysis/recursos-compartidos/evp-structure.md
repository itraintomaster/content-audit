# Estructura de evp.json

## Ruta Original
`src/main/resources/vocabulary/evp.json`

## Fuente
English Vocabulary Profile: https://www.englishprofile.org/wordlists/evp

## Estructura JSON

```json
{
    "source": "https://www.englishprofile.org/wordlists/evp",
    "levels": {
        "A1": 784,
        "A2": 1594,
        "B1": 2937,
        "B2": 4164,
        "C1": 2410,
        "C2": 3807
    },
    "words": [
        {
            "word": "cattle",
            "guideWord": "",
            "level": "B1",
            "partOfSpeech": "",
            "topic": "animals",
            "undefined": "Details"
        },
        {
            "word": "clothes",
            "guideWord": "",
            "level": "A1",
            "partOfSpeech": "",
            "topic": "clothes",
            "undefined": "Details"
        }
    ]
}
```

## Campos por Entrada

| Campo | Tipo | Descripcion | Notas |
|-------|------|-------------|-------|
| `word` | String | La palabra en ingles | Puede ser una palabra o una frase |
| `guideWord` | String | Palabra guia (acepccion) | A veces vacio |
| `level` | String | Nivel CEFR (A1-C2) | |
| `partOfSpeech` | String | Parte de la oracion | **Frecuentemente vacio** |
| `topic` | String | Categoria tematica | Ej: "animals", "clothes", "food" |
| `undefined` | String | Siempre "Details" | Campo sin uso aparente |

<!-- Cuidado aqui: El campo partOfSpeech esta frecuentemente vacio en el EVP original. El enriched_vocabulary_catalog.json corrige esto usando SpaCy para asignar POS tags. Si se migra usando solo evp.json, las comparaciones por POS no funcionaran correctamente. -->

## Ejemplos de Topics

animals, body, clothes, colour, communication, computers, drink, education, employment, entertainment, environment, family, food, health, hobbies, homes, language, money, people, personal, places, relationships, science, shopping, society, sport, technology, time, tourism, transport, weather, work
