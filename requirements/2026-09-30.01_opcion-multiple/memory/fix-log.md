# Fix Log — FEAT-OPMUL

2026-09-30 — developer — Maven con JAVA_HOME=~/.sdkman/candidates/java/25.0.2-open: el mvn de Homebrew corre en Java 24 y el sentinel-agents de ~/.m2 esta compilado para Java 25.
  why: con Java 24, revision-infrastructure corta con "failed to discover tests" y el build no llega a audit-cli.
2026-09-30 — developer — Nunca `mvn install` desde una rama: pisa los jars de com/learney/contentaudit en ~/.m2, que usan los builds de un solo modulo de las otras ramas. Para un modulo con sus dependencias, `-pl X -am`.
  why: paso por error un minuto; se restauraron desde un archive de main.
2026-09-30 — test-writer — Los XML de surefire de clases borradas quedan en target/: contar los tests por el log de Maven.
2026-09-30 — developer — copyOf pasa por el constructor completo a proposito: si el modelo suma un campo, deja de compilar en vez de perderlo en la copia.
  why: generate no toca FormEntities ni QuizTemplateEntities (no son implementaciones declaradas), asi que nadie rellena la llamada con null.
