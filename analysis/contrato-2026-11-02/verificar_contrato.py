# Baja la vista de la C2 del 7800 (sólo GET): cambios, decisiones, análisis y opiniones, a <carpeta>/*-c2.json.
# La importación de tablero.py sale de la carpeta del script.
import json, sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tablero import pedir, sesion, C2
tok = sesion()
for nombre, ruta in [('vista', '/api/cambios'), ('decisiones', '/api/decisiones'), ('analisis', '/api/analisis'), ('opiniones', '/api/opiniones')]:
    st, x = pedir('GET', f'{ruta}?tanda={C2}', token=tok)
    assert st == 200, (nombre, st, str(x)[:300])
    json.dump(x, open(f'{sys.argv[1]}/{nombre}-c2.json', 'w'))
    print(nombre, sorted(x.keys()) if isinstance(x, dict) else type(x))
