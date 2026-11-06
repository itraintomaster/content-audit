---
modulo: audit-cli
para: la línea de comandos: arma todas las piezas y las conecta
depende: [audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure, evaluation-ledger-domain, evaluation-ledger-infrastructure, agent-runtime-infrastructure, quiz-instruction-infrastructure, local-language-infrastructure]
---

La ficha sale de sentinel.yaml: el módulo de Maven, para qué está y de quién depende. Todavía
no declara puerta, así que se puede entrar por cualquiera de sus clases; Plomada avisa cuando
un módulo empieza a usar a otro que esta lista no nombra.
