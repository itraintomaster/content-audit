---
modulo: audit-infrastructure
para: guarda en disco los informes de auditoría, los planes de refinamiento y lo que deja la revisión
depende: [audit-domain, refiner-domain, revision-domain]
---

La ficha sale de sentinel.yaml: el módulo de Maven, para qué está y de quién depende. Todavía
no declara puerta, así que se puede entrar por cualquiera de sus clases; Plomada avisa cuando
un módulo empieza a usar a otro que esta lista no nombra.
