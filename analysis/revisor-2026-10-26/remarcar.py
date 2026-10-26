# Vuelve a marcar con el revisor de F-MARCA-R010 (26/10/2026) los 151 ejercicios de la C2 que ya tenían marca de la
# versión 6f899ccf: los 51 de la prueba ancha (calibración) y las primeras 100 de brief 229. Una sola corrida, así el
# registro de marcas no tiene dos procesos escribiendo a la vez; después marcar.py v2 0 compara gratis.
# Uso: remarcar.py <tope>   (tope 0: sólo muestra cuántas faltan, no consulta)
import json
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402
from marcar import referencia  # noqa: E402

AQUI = Path(__file__).parent
CLI = AQUI.parent.parent / 'audit-cli.sh'


def ids():
    cien = [f['quizId'] for f in json.loads((AQUI / 'primeras100.json').read_text())['filas']]
    return list(referencia()) + cien


if __name__ == '__main__':
    tope = int(sys.argv[1])
    quiz = ids()
    args = ['bash', str(CLI), '--workdir', str(WORK), 'batch', 'mark', (OUT / 'c2-id.txt').read_text().strip(),
            '-f', 'json', '--concurrency', '4']
    for q in quiz:
        args += ['--quiz', q]
    if tope:
        args += ['--budget', str(tope)]
    inicio = time.monotonic()
    p = subprocess.run(args, cwd=REPO, env=ENV, capture_output=True, text=True, timeout=14400)
    (AQUI / f'remarcar-{tope}.stderr.log').write_text(p.stderr)
    if p.returncode:
        raise SystemExit(p.stderr[-2000:])
    r = json.loads(p.stdout)
    estados = {}
    for row in r['rows']:
        estados[row['state']] = estados.get(row['state'], 0) + 1
    print(json.dumps({'pedidos': len(quiz), 'distintos': len(set(quiz)), 'cambiados': r['changed'], 'alDia': r['upToDate'],
                      'faltan': r['pending'], 'consultas': r['spent'], 'version': r['reviewerVersion'][:8],
                      'motor': r['runtime'], 'estados': estados, 'segundos': round(time.monotonic() - inicio),
                      'noDisponible': r.get('unavailableReason')}, ensure_ascii=False))
