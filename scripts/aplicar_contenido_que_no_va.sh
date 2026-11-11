#!/usr/bin/env bash
# Lleva a producción los reemplazos del brief 284 (contenido que no va en el curso).
# José, 11/11/2026: «Los reemplazos, recordá que también tenés que generar el audio de nuevo».
#
#   bash scripts/aplicar_contenido_que_no_va.sh
#
# Hace, en orden, y frena ante el primer error:
#   1. Respalda producción entera (mongodump) en mongo-buckups/2026-11-11-contenido-que-no-va.
#   2. Respalda los 14 audios de hoy y la teoría b2.15 de S3, en la carpeta de corridas.
#   3. Corre el parche en seco contra producción y te pregunta si sigue.
#   4. Aplica los 14 ejercicios de A2, B1 y B2 (y sus copias sin resolver) y verifica.
#   5. Sube los 14 audios nuevos (ya generados y verificados) y los verifica contra S3.
#   6. Sube la teoría b2.15 con el ejemplo nuevo (rama claude/contenido-que-no-va-teoria de ittm).
#
# Se corre desde la raíz de content-audit. El de A1 (1e82) NO va acá: va después de publicar la
# C2, con --grupo despues-de-la-c2 y los audios de audios-despues-de-la-c2.
#
# Para volver atrás:
#   python3 scripts/contenido_que_no_va.py --revertir
#   for f in "$W"/audios-antes/*.mp3; do aws s3 cp "$f" "s3://learney/quizzes-audios/audios_mp3/$(basename "$f")" --content-type audio/mp3; done
#   aws s3 cp "$W/teoria-antes.json" "s3://learney/theory/$TEORIA.json" --content-type application/json
set -euo pipefail

W="$HOME/projects/learney/content-audit-corridas/2026-11-11-contenido-que-no-va"
RESPALDO="$HOME/projects/learney/mongo-buckups/2026-11-11-contenido-que-no-va"
CONFIG="$HOME/projects/learney/ittm/be/application/serverless/aws-lambda-handler/cmd/config.yaml"
ITTM="$HOME/projects/learney/ittm"
TEORIA="b2.15.Verbs_and_Different_Prepositions"
RAMA_TEORIA="claude/contenido-que-no-va-teoria"
S3_AUDIOS="s3://learney/quizzes-audios/audios_mp3"

cd "$(dirname "$0")/.."
[ -f "$W/audios-ahora/regen_manifest.json" ] || { echo "falta $W/audios-ahora/regen_manifest.json"; exit 1; }
[ "$(ls "$W/audios-ahora/regen/mp3" | wc -l | tr -d ' ')" = 14 ] || { echo "no están los 14 mp3 nuevos"; exit 1; }
git -C "$ITTM" cat-file -e "$RAMA_TEORIA:content/theory/$TEORIA.json" || { echo "falta la rama $RAMA_TEORIA en ittm"; exit 1; }

PROD_URI=$(grep -m1 'defaultMongoUri' "$CONFIG" | sed -E 's/.*&defaultMongoUri //')
HOST=$(printf '%s' "$PROD_URI" | sed -E 's#.*@([^/?]+).*#\1#')
echo "== destino: $HOST / learney"
nslookup -type=SRV "_mongodb._tcp.$HOST" >/dev/null || { echo "el cluster no resuelve: config zombi"; exit 1; }

echo "== 1. respaldo de producción en $RESPALDO"
mongodump --uri "$PROD_URI" --db learney --out "$RESPALDO" --quiet
ls -la "$RESPALDO/learney/quizTemplates.bson"

echo "== 2. respaldo de los 14 audios y de la teoría"
mkdir -p "$W/audios-antes"
for k in $(python3 -c "import json;print(' '.join(r['key'] for r in json.load(open('$W/audios-ahora/regen_manifest.json'))))"); do
  aws s3 cp "$S3_AUDIOS/$k.mp3" "$W/audios-antes/$k.mp3" --only-show-errors
done
echo "   $(ls "$W/audios-antes" | wc -l | tr -d ' ') audios respaldados"
aws s3 cp "s3://learney/theory/$TEORIA.json" "$W/teoria-antes.json" --only-show-errors
git -C "$ITTM" show "$RAMA_TEORIA~1:content/theory/$TEORIA.json" | cmp -s - "$W/teoria-antes.json" \
  && echo "   la teoría publicada es igual a la del repo antes del cambio" \
  || { echo "   OJO: la teoría publicada en S3 no es la del repo; no la piso. Revisalo y subila a mano."; SALTEAR_TEORIA=1; }

echo "== 3. en seco contra producción"
python3 scripts/contenido_que_no_va.py
read -r -p "¿Aplico los 14 ejercicios y subo los audios? (s/n) " sigue
[ "$sigue" = "s" ] || { echo "no se aplicó nada"; exit 0; }

echo "== 4. aplicar"
python3 scripts/contenido_que_no_va.py --aplicar

echo "== 5. audios nuevos"
python3 scripts/regenerate_quiz_audios.py --workdir "$W/audios-ahora" upload
python3 scripts/regenerate_quiz_audios.py --workdir "$W/audios-ahora" verify

echo "== 6. teoría"
if [ -z "${SALTEAR_TEORIA:-}" ]; then
  git -C "$ITTM" show "$RAMA_TEORIA:content/theory/$TEORIA.json" \
    | aws s3 cp - "s3://learney/theory/$TEORIA.json" --content-type application/json
  if aws s3 cp "s3://learney/theory/$TEORIA.json" - | grep -q "The children threw snowballs at each other."; then
    echo "   teoría subida y verificada"
  else
    echo "   OJO: la teoría en S3 no tiene el ejemplo nuevo"; exit 1
  fi
fi
echo "== listo"
