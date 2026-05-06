# Fix log

2026-05-06 - qa-tester - sentinel patch apply silenció el placement de las 8 journeys (testModule/testPackage). handwrittenTests sí aterrizaron correctamente; las 18 reglas tienen sus 44 tests. Solo los journey gates F-CDIFF-J001..J008 quedaron sin testModule/testPackage en sentinel.yaml, y por eso sentinel generate no produjo las clases FCdiffJ00*JourneyTest.java.
  why: el bug `2026-04-20-01-silent-drop-journey-test-placement.md` (resolved 2026-04-20) reporta que el merge de apply era correcto y el bug real estaba en validate. Con la fix vigente, un re-propose journeys-only cuenta correctamente "8 modifications" — antes hubiera dicho "0". El fix es: re-proponer un patch focalizado SOLO sobre journeys placement y volver a aplicar. El usuario re-aplica, sentinel generate produce los 8 stubs, y el flujo sigue.

2026-05-06 - qa-tester - Patch journeys-only re-propuesto en architectural_patch.yaml (8 modifications, 0 conflicts). Pendiente: que el usuario corra `sentinel patch apply` y `sentinel generate` para que aterricen los stubs `FCdiffJ001JourneyTest.java`..`FCdiffJ008JourneyTest.java` (3 en audit-cli/.../commands, 5 en revision-domain/.../engine).
