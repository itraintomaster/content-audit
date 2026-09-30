# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-NATUR / F-NATUR y nombre `naturalness`: nuevos.

2026-09-30 — analyst — Método propuesto: preguntarle al modelo local (llama3 8B en Ollama, ya instalado) si un nativo diría
  la oración y usar ln(P(no)/P(sí)) del primer token (R002) [ASSUMPTION, DOUBT-NATUR-METODO], en vez del SLOR de la propuesta.
  why: medido sobre A1–A2 (4.813): la cola del 5 % por nivel trae 51 de los 186 antinatural+sentido de escribir (27,4 %,
  5,5 veces el azar; 47,9 % de la cola en algún oro contra 23,2 % de base). SLOR con el mismo modelo, leído de a un token
  con 20 alternativas porque Ollama no da la probabilidad del texto: 14 de 186 (7,5 %), casi el azar.

2026-09-30 — analyst — El cociente y no P(no): P(no) pasa de 0,999 en más de 300 ejercicios por nivel y el borde del 5 % se
  vuelve un empate. Umbrales del 5 % en la base: 10,0 (A1) y 10,2 (A2).

2026-09-30 — analyst — Sólo ordena (como aprobó José en el 089) y umbral fijo por nivel, no «el 5 % de cada corrida»
  (DOUBT-NATUR-MARCA, DOUBT-NATUR-UMBRAL).
  why: con 48 % de la cola en algún oro, como error inflaría «algún error»; con el 5 % por corrida, arreglar una oración
  rara marcaría otra que no cambió (F-HALL-R014).

2026-09-30 — analyst — Examen (R006): al menos el triple del azar en la cola del 5 % (28 de 186 en escribir). La pregunta
  sí/no lo aprueba; SLOR con lo instalado no. Sin verbo o de una palabra no alcanza (76 de 4.813 en A1–A2).
