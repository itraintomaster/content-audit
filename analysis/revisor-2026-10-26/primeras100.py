# Lee lo que marcó el revisor en la corrida de 100 que José pidió desde Correcciones el 26/10/2026 (14:04).
# Leer es gratis: `batch mark` sin --budget no consulta. Las marcas nuevas son las del ledger después de las 51
# de la calibración; cada una sale con los dos lados enteros, para leerla como la leyó el revisor.
# Uso: primeras100.py <desde, ISO UTC>   (p. ej. 2026-10-26T17:04:00Z)
import json
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402

AQUI = Path(__file__).parent
LEDGER = WORK / '.content-audit/evaluations/change-reviewer.jsonl'
MARCA = {'GOOD': 'bien', 'DOUBTFUL': 'dudoso', 'WORSE': 'mal'}


def nuevas(desde):
    """Los quizId que el revisor marcó desde [desde], con la hora de cada marca."""
    hora = {}
    for renglon in LEDGER.read_text().splitlines():
        r = json.loads(renglon)
        if r['recordedAt'] >= desde:
            hora[r['subjectRef']] = r['recordedAt']
    return hora


def leer():
    c2 = (OUT / 'c2-id.txt').read_text().strip()
    p = subprocess.run(['bash', str(REPO / 'audit-cli.sh'), '--workdir', str(WORK), 'batch', 'mark', c2, '-f', 'json'],
                       cwd=REPO, env=ENV, capture_output=True, text=True, timeout=900)
    (AQUI / 'primeras100.stderr.log').write_text(p.stderr)
    if p.returncode:
        raise SystemExit(p.stderr[-2000:])
    return json.loads(p.stdout)


if __name__ == '__main__':
    hora = nuevas(sys.argv[1])
    r = leer()
    filas = []
    for orden, row in enumerate(r['rows']):
        if row['quizId'] not in hora:
            continue
        v = row.get('verdict') or {}
        filas.append({'id': row['quizId'][-4:], 'quizId': row['quizId'], 'orden': orden, 'tema': row['topic'],
                      'estado': row['state'], 'marca': MARCA.get(v.get('mark'), 'sin marca'),
                      'razon': v.get('reason') or row.get('detail'), 'problemas': v.get('problems') or [],
                      'hora': hora[row['quizId']], 'antes': row['before'], 'despues': row['after']})
    cuenta = {}
    for f in filas:
        cuenta[f['marca']] = cuenta.get(f['marca'], 0) + 1
    salida = {'tanda': r['batchId'], 'version': r['reviewerVersion'], 'motor': r['runtime'],
              'cambiados': r['changed'], 'alDia': r['upToDate'], 'faltan': r['pending'],
              'nuevas': len(filas), 'cuenta': cuenta, 'filas': filas}
    (AQUI / 'primeras100.json').write_text(json.dumps(salida, ensure_ascii=False, indent=1))
    print(json.dumps({k: v for k, v in salida.items() if k != 'filas'}, ensure_ascii=False))
