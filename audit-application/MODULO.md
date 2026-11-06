---
modulo: audit-application
para: los casos de uso que juntan la auditoría, el refinamiento y la revisión
depende: [audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain, evaluation-ledger-domain]
---

La ficha sale de sentinel.yaml: el módulo de Maven, para qué está y de quién depende. Todavía
no declara puerta, así que se puede entrar por cualquiera de sus clases; Plomada avisa cuando
un módulo empieza a usar a otro que esta lista no nombra.
