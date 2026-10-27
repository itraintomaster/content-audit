# Lee gratis las marcas del revisor de la C2 entera después del tramo grave (26-27/10/2026): `batch mark` sin
# --budget no consulta. Junta cada ejercicio cambiado con sus dos lados y su marca, dice cuáles tocó el tramo grave y
# saca una muestra fija de los «bien» para leer a mano junto con lo marcado.
# Uso: leer_marcas.py [muestra]   (20 «bien» si no se dice)
import json
import random
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402
from marcar import marca  # noqa: E402

AQUI = Path(__file__).parent


def leer():
    c2 = (OUT / 'c2-id.txt').read_text().strip()
    p = subprocess.run(['bash', str(REPO / 'audit-cli.sh'), '--workdir', str(WORK), 'batch', 'mark', c2, '-f', 'json'],
                       cwd=REPO, env=ENV, capture_output=True, text=True, timeout=1800)
    (AQUI / 'leer-marcas.stderr.log').write_text(p.stderr)
    if p.returncode:
        raise SystemExit(p.stderr[-2000:])
    return json.loads(p.stdout)


if __name__ == '__main__':
    tamano = int(sys.argv[1]) if len(sys.argv) > 1 else 20
    grave = set(json.loads((OUT / 'estimacion-tramo-grave.json').read_text())['quizIds'])
    r = leer()
    filas = []
    for row in r['rows']:
        v = row.get('verdict') or {}
        filas.append({'id': row['quizId'][-4:], 'quizId': row['quizId'], 'tema': row['topic'], 'estado': row['state'],
                      'marca': marca(v) if row['state'] == 'MARKED' else 'sin marca', 'grave': row['quizId'] in grave,
                      'razon': v.get('reason') or row.get('detail'), 'problemas': v.get('problems') or [],
                      'produccion': row['before'], 'correccion': row['after']})
    bien = sorted(f['quizId'] for f in filas if f['marca'] == 'bien')
    muestra = set(random.Random(20261027).sample(bien, min(tamano, len(bien))))
    for f in filas:
        f['muestra'] = f['quizId'] in muestra
    cuenta, cuenta_grave = {}, {}
    for f in filas:
        cuenta[f['marca']] = cuenta.get(f['marca'], 0) + 1
        if f['grave']:
            cuenta_grave[f['marca']] = cuenta_grave.get(f['marca'], 0) + 1
    salida = {'tanda': r['batchId'], 'version': r['reviewerVersion'], 'motor': r['runtime'], 'cambiados': r['changed'],
              'alDia': r['upToDate'], 'faltan': r['pending'], 'cuenta': dict(sorted(cuenta.items())),
              'cuentaTramoGrave': dict(sorted(cuenta_grave.items())), 'filas': filas}
    (AQUI / 'marcas-c2.json').write_text(json.dumps(salida, ensure_ascii=False, indent=1))
    print(json.dumps({k: v for k, v in salida.items() if k != 'filas'}, ensure_ascii=False))
