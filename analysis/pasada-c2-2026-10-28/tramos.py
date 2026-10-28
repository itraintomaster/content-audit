# Parte la selección en dos tramos: el primero se lee a mano antes de lanzar el segundo.
# Uso: tramos.py <seleccion.json> <cuantos en el primero>
# El primer tramo empieza por los que trabaron las pruebas (221d, 1fe1, 24c2) y por los de «otro tema» que marca Calidad
# A1, que ahora pueden tocar la tarjeta y la consigna del ejercicio; el resto se completa en el orden de la selección.
import json, sys
from pathlib import Path

AQUI = Path(__file__).parent
PRIMERO = ['221d', '1fe1', '24c2', '2615', '2618', '261c', '275e', '2761', '2626', '274e', '21a4', '2ce0']
seleccion = json.loads((AQUI / sys.argv[1]).read_text())
cuantos = int(sys.argv[2])
primero = [q for corto in PRIMERO for q in seleccion if q.endswith(corto)]
resto = [q for q in seleccion if q not in primero]
tramo1 = (primero + resto)[:cuantos]
tramo2 = [q for q in seleccion if q not in tramo1]
(AQUI / 'tramo-1.json').write_text(json.dumps(tramo1))
(AQUI / 'tramo-2.json').write_text(json.dumps(tramo2))
print(len(tramo1), 'en el primero (', len(primero), 'elegidos );', len(tramo2), 'en el segundo')
