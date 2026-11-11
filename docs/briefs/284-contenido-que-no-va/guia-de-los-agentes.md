# Guía: buscar contenido polémico en ejercicios de un curso de inglés

Learney es una app para que hispanohablantes adultos aprendan inglés (niveles A1 a B2). El
contenido lo generó un modelo en 2025 y nadie lo miró con este criterio. Buscamos oraciones,
traducciones, opciones de respuesta o explicaciones que NO deberían estar en una app educativa
apta para todo público (piensa en la clasificación 4+/12+ de una tienda de apps).

## Formato de cada renglón

`id | nivel | tema | EN: oración en inglés (la respuesta correcta va entre [corchetes]) | OPCIONES MAL: distractores (sólo opción múltiple) | ES: traducción`
En teorías y tarjetas: `theory:<id> | texto…` o `card:<id> | texto…`.
El id es lo que va antes del primer `|` (ej. `1f63`, `C2-1f63`, `card:6814…`).

## Qué marcar

ALTA (hay que cambiarlo ya):
- violencia contra personas: agresiones, asesinatos, apuñalar, disparar, armas contra alguien,
  secuestro, tortura; violencia doméstica o de pareja; amenazas de muerte
- abuso de cualquier tipo; contenido sexual o insinuaciones sexuales
- suicidio, autolesión, deseo de morir
- terrorismo, bombas, matanzas
- insultos o desprecio hacia un grupo (raza, nacionalidad, religión, género, orientación,
  discapacidad), racismo, esclavitud presentada con liviandad
- consumo de drogas ilegales; crueldad explícita con animales

MEDIA (conviene cambiarlo):
- algo chocante o de mal gusto sin necesidad: muertes raras o de personas cercanas usadas como
  ejemplo, cadáveres, sangre, accidentes graves, alarmismo («todos vamos a morir»)
- peleas o golpes entre personas, pegarle a alguien o a un animal, castigos físicos
- delitos (robar, estafar, mentirle a la policía) dichos en primera persona o como algo normal
- borracheras, beber en exceso, fumar presentado como algo bueno, apuestas
- estereotipos de género, de nacionalidad o de aspecto; burlarse del peso, la fealdad o la
  inteligencia de alguien; humillaciones
- política partidaria, líderes políticos reales, religión de forma polémica, guerras actuales

BAJA (sólo si suena raro para una app educativa): temas adultos tratados con normalidad pero
de forma extraña o innecesaria.

## Qué NO marcar

Menciones cotidianas y neutras: una copa de vino en la cena, un hecho histórico neutro
(«The war ended in 1945»), patear una pelota, lastimarse la rodilla jugando, un cuchillo de
cocina, la policía haciendo su trabajo, «I hate Mondays», alguien enfermo que va al médico, un
abuelo que murió dicho con naturalidad, un ladrón en una noticia contada en tercera persona.
Tampoco marques errores de inglés o de traducción: no es lo que buscamos.

Ejemplos inventados (no están en el curso) para calibrar:
- «He hit his son because he was late.» → ALTA (violencia contra un niño)
- «She was so fat that nobody wanted to sit next to her.» → MEDIA (burla por el peso)
- «I drank ten beers and drove home.» → MEDIA (borrachera + conducir)
- «My grandmother died last year.» → no marcar
- «They stole my bike yesterday.» → no marcar (víctima, tercera persona)

Mirá los cuatro campos: la oración en inglés, la traducción, los distractores y el texto de
teorías y tarjetas. A veces lo grave está sólo en la traducción o en una opción incorrecta.

## Cómo trabajar

1. Leé el archivo ENTERO, en tramos de 250 renglones con Read (offset/limit), sin saltear
   ninguno, hasta el último renglón. No uses grep ni búsquedas por palabra: la idea es leer.
2. Por cada renglón que corresponda, anotá un hallazgo.
3. Al terminar, escribí con Write un archivo JSONL (un objeto JSON por renglón) en la ruta que te
   indico, con estos campos:
   {"id": "...", "nivel": "...", "tema": "...", "texto": "<el renglón tal cual, desde EN:>",
    "categoria": "violencia|muerte|sexual|suicidio|discriminacion|drogas-alcohol|delito|animales|chocante|politica-religion|otro",
    "gravedad": "ALTA|MEDIA|BAJA", "por_que": "<una frase en castellano>"}
   Si no encontrás nada, escribí el archivo vacío igual.
4. Respondé en dos líneas: cuántos renglones leíste (el número del último renglón) y cuántos
   hallazgos hay por gravedad.
