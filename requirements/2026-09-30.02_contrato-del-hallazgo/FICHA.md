---
paquete: 0.3
nombre: Contrato del hallazgo
codigo: FEAT-HALL
requirement: requirements/2026-09-30.02_contrato-del-hallazgo
dudas: 3
---

# Contrato del hallazgo

**Pregunta:** ¿Qué está mal en cada contexto, dicho igual por los 19 analizadores y contado por content-audit?

## 1 · Qué resuelve
Que todo analizador informe lo que no pasa con una sola forma (el hallazgo), se describa solo en `get analyzers`
(su ficha) y que content-audit publique en cada tema, nivel y curso el puntaje de vocabulario de hoy y los errores
contados. Deja afuera los analizadores nuevos, cómo un hallazgo se vuelve tarea (E) y las pantallas (G).

## 2 · Qué decide

| Qué | Elijo | Descarto | Por qué |
|---|---|---|---|
| Dónde hay hallazgos | Sólo donde el analizador calcula su propio puntaje; el motor se los pide ahí | Que cada uno los escriba donde quiera | Un tema con promedio menor que 1 no tiene un problema propio |
| Puntaje de vocabulario | Promedio de los de vocabulario, sin los cuartos COCA ni jueces | Promediar todas las claves | Es el número de hoy: 73,9 %; el 73,4 % sumaba los cuartos |
| El juez en los contextos | Corre con los demás, antes de sumar | Después de sumar, como hoy | Hoy nunca llega al tema, al nivel ni al curso |
| Dónde viven los números | Calculados una vez en cada nodo, y en un resumen de 10 a 15 MB | Que la UI los arme leyendo 146 MB | El 92 % del informe es el curso repetido cuatro veces |
| Qué corre | Exactamente lo pedido; un nombre desconocido frena antes de empezar | Correr siempre los siete clásicos | Hoy `--analyzers` no los restringe |
| Cada corrida | Analizadores nuevos cada vez | Reusar los mismos | La vista consolidada corre dos veces y la segunda hereda la primera |

## 3 · Cómo se ve
- **En el ejercicio:** ✕ Largo de oración · «He isn't in the living-room.» (A1 · Be: preguntas yes/no). Largo: 9
  tokens; A1 va de 3 a 8. En el mismo tema, ✕ Palabras de otro nivel · «Where is my wallet?»: wallet es de A2.
- **En el tema, el nivel y el curso:** vocabulario A1 96,5 · A2 94,2 · B1 79,5 · B2 63,5 · curso 73,9, el mismo en
  el CLI, `get audits`, la vista consolidada y el tablero (hoy el tablero dice B2 51,0 y curso 73,4). Con el juez,
  cada contexto suma «algún error»: ejercicios con error, su porcentaje, por gravedad y con lo no juzgado declarado.
  La base del 29/9 corrió sin el juez, así que hoy esa fila sale vacía.
- **¿Necesita un dibujo propio?** No. Es lo que deja a los demás sin dibujo propio: la UI recibe la ficha de cada
  analizador, una fila por analizador en cada contexto, «algún error» y el puntaje de vocabulario.

## 4 · Cómo se acopla y qué reusa
- **Nuevo:** el hallazgo, la ficha, los números de cada contexto, un proveedor por analizador y el resumen chico.
- **Enganches de cada analizador futuro:** su bloque en sentinel.yaml, el lugar de su diagnóstico tipado (dos
  archivos por nivel), una línea en `Main` y, si produce tareas, su tipo de tarea. Nada más de lo compartido.
- **Reusa:** la suma de hoy, los diagnósticos tipados de los ocho, el libro de evaluaciones, el tope y la vista consolidada.
- **No toca:** cómo puntúa cada analizador, la huella del juez (13.000 veredictos pagados), el modelo del curso (0.2).

## 5 · Qué mantiene
- F-DLABS-R001 a R003: el diagnóstico tipado sigue; el hallazgo no tiene campos libres (ver duda 3).
- F-RCLA-R001: tarea sólo con puntaje menor que 1. El plan da las mismas 4.036 tareas (3.051 + 950 + 25 + 6 + 3 + 1).
- F-QINST-R004 (el tema promedia a sus juzgados, hoy incumplida), R007 (un modelo caído no corta) y R015 (un nombre, también para el tope).
- F-EVCOST-R001 a R004: no cambia ninguna huella; F-CLIRV-R016: el mismo mensaje al rechazar un nombre.
- F-CDIFF-R007 y R019 a R023: la vista consolidada compara hallazgos por su identidad; F-PIPRE-R004: el curso es el publicado.
- ⚠ Cambia a propósito: `get audits`, `get audit`, `analyze -f raw` y el tablero pasan de 73,4 a 73,9 (R010); la
  segunda corrida de la vista consolidada deja de heredar la primera (R014); y el ejercicio juzgado (duda 1).

## 6 · Cómo se prueba
- **Examen:** no usa el set del 077: su examen es la base del 29/9. Un `analyze` completo antes y después da el mismo
  73,9 %, los mismos niveles, los mismos diagnósticos y las mismas 4.036 tareas; y cada analizador solo trae sólo lo suyo.
- **Meta propuesta:** cero diferencias en vocabulario y plan; cero hallazgos en nodos que pasan o sólo promedian; «algún
  error» cierra sus tres sumas en cada nodo.
- **Costo:** segundos: una pasada más sobre 11.760 nodos y ninguna consulta nueva a modelos.

## 7 · Para José
- **¿El ejercicio que juzgó el juez deja de promediarlo?** A: sí, el juez se ve aparte, como error. B: el ejercicio
  guarda también su promedio viejo, como segundo número. Recomiendo A: es el único número de hoy que cambia, y en la
  base del 29/9 no hay ninguno juzgado.
- **¿Un hallazgo del tema, como la miniteoría, cuenta en cada ejercicio del tema?** A: sí. B: aparte, como temas con
  hallazgo. C: las dos. Recomiendo A: la miniteoría la ven todos sus ejercicios, y así contó el 077.
- **¿Sacamos el enganche del diagnóstico tipado?** Se puede con un «cajón tipado» por nivel, donde cada analizador deja
  su diagnóstico sin tocar archivos ajenos. Cuesta dos formas de guardar diagnósticos y nombres de clases en el JSON.
  Recomiendo que no por ahora: son dos archivos por analizador y el integrador los junta en un parche por ronda.

Las otras tres dudas del analista —gravedad de los ocho, palabras de otro nivel y `stats` de los jueces— no cambian el
diseño: cualquier opción es local. Van con su recomendación A, salvo que digas otra cosa.
