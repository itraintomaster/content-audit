# Fix Log

2026-04-18 — architect — fix: declarar implementaciones SOLO dentro del package, no al root del módulo.
  why: `patch propose` valida el patch en sí pero no detecta duplicados cross-scope; el conflicto aparece recién en `sentinel generate`. La prevención es estructural — un componente vive en exactamente un scope.
