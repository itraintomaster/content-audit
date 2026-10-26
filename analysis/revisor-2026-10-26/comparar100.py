# Compara las 100 marcas del revisor de F-MARCA-R010 (versión a67dac84) con mi lectura de brief 229, que leí una por
# una con la versión anterior (6f899ccf). Leer es gratis: `batch mark` sin --budget no consulta.
# Mi lectura es una referencia, no una verdad certificada: lo que el revisor nuevo marca distinto se vuelve a leer.
# Uso: comparar100.py [etiqueta]   (después de remarcar.py; etiqueta v2 si no se dice)
import json
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402
from brief229 import MIA, BIEN_DUDOSOS  # noqa: E402
from marcar import marca  # noqa: E402

AQUI = Path(__file__).parent
CLI = AQUI.parent.parent / 'audit-cli.sh'


def esperado(corto):
    """Lo que leo yo: «heredado» para lo que ya estaba, la marca para lo que dejó la corrección, «bien» si no."""
    if corto in MIA:
        grupo, mia, _ = MIA[corto]
        return grupo, 'heredado' if grupo == 'hereda' else mia
    if corto in BIEN_DUDOSOS:
        return 'leve', BIEN_DUDOSOS[corto][0]
    return 'bien', 'bien'


def clase(grupo, ahora):
    marca_algo = ahora in ('dudoso', 'heredado', 'mal')
    if grupo == 'corrige':
        return 'nuevo bien visto' if ahora in ('dudoso', 'mal') else 'nuevo como heredado' if ahora == 'heredado' else 'nuevo perdido'
    if grupo == 'hereda':
        return 'heredado aparte' if ahora == 'heredado' else 'heredado sin separar' if marca_algo else 'heredado perdido'
    if grupo == 'falsa':
        return 'falsa alarma sigue' if marca_algo else 'falsa alarma apagada'
    if grupo == 'leve':
        return 'leve marcado' if marca_algo else 'leve sin marcar'
    return 'alarma nueva' if marca_algo else 'bien igual'


if __name__ == '__main__':
    d = json.loads((AQUI / 'primeras100.json').read_text())
    args = ['bash', str(CLI), '--workdir', str(WORK), 'batch', 'mark', (OUT / 'c2-id.txt').read_text().strip(), '-f', 'json']
    for f in d['filas']:
        args += ['--quiz', f['quizId']]
    p = subprocess.run(args, cwd=REPO, env=ENV, capture_output=True, text=True, timeout=900)
    if p.returncode:
        raise SystemExit(p.stderr[-2000:])
    r = json.loads(p.stdout)
    filas_v2 = {row['quizId']: row for row in r['rows']}
    filas, cuenta = [], {}
    for f in d['filas']:
        row = filas_v2[f['quizId']]
        grupo, yo = esperado(f['id'])
        ahora = marca(row.get('verdict')) if row['state'] == 'MARKED' else 'sin marca'
        c = clase(grupo, ahora)
        cuenta[c] = cuenta.get(c, 0) + 1
        filas.append({'id': f['id'], 'quizId': f['quizId'], 'tema': f['tema'], 'grupo': grupo, 'yo': yo,
                      'antes': f['marca'], 'ahora': ahora, 'clase': c, 'estado': row['state'],
                      'razon': row['verdict']['reason'] if row.get('verdict') else row.get('detail'),
                      'problemas': row['verdict']['problems'] if row.get('verdict') else [],
                      'produccion': f['antes'], 'correccion': f['despues']})
    salida = {'version': r['reviewerVersion'], 'motor': r['runtime'], 'alDia': r['upToDate'], 'faltan': r['pending'],
              'leyenda': {'antes': 'marca de la versión 6f899ccf (brief 229)', 'ahora': 'marca de la versión de hoy',
                          'yo': 'mi lectura de brief 229 (heredado = ya estaba en producción)'},
              'cuenta': dict(sorted(cuenta.items())), 'filas': filas}
    etiqueta = sys.argv[1] if len(sys.argv) > 1 else 'v2'
    (AQUI / f'comparacion-100-{etiqueta}.json').write_text(json.dumps(salida, ensure_ascii=False, indent=1))
    print(json.dumps({k: salida[k] for k in ('version', 'alDia', 'faltan', 'cuenta')}, ensure_ascii=False))
    for f in filas:
        if f['clase'] not in ('bien igual', 'falsa alarma apagada', 'nuevo bien visto', 'heredado aparte'):
            print(f"  {f['id']} {f['clase']}: yo={f['yo']} antes={f['antes']} ahora={f['ahora']} · {f['razon']}")
