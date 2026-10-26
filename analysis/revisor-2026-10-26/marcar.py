# Corre el revisor de cambios (FEAT-MARCA) sobre la C2 de A1 y lo compara con mi lectura a mano de la prueba ancha,
# 26/10/2026. Mis veredictos son una referencia, no una verdad certificada.
# Uso: marcar.py <etiqueta> <tope> [<quizId corto>...]   (tope 0: sólo muestra lo que hay, no consulta)
# El CLI sale del checkout que contiene este script (el worktree mientras ca main no tenga el revisor).
import json, subprocess, sys, time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402

AQUI = Path(__file__).parent
CLI = AQUI.parent.parent / 'audit-cli.sh'
VEREDICTOS = json.loads((OUT / 'veredictos-ancha.json').read_text())['veredictos']
# Corrección mía del 26/10: 1e8d, 1e8e y 1e92 tienen la misma consigna que regala de 21c9 y los había dado por buenos.
CORREGIDOS = {'1e8d': 'dudoso', '1e8e': 'dudoso', '1e92': 'dudoso'}
MARCA = {'GOOD': 'bien', 'DOUBTFUL': 'dudoso', 'WORSE': 'mal'}
MARCAN = ('dudoso', 'heredado', 'mal')


def marca(verdict):
    """Como el tablero (F-MARCA-R010): un dudoso cuyos problemas ya estaban todos en producción es «heredado»."""
    if not verdict:
        return 'sin marca'
    problemas = verdict['problems']
    if verdict['mark'] == 'DOUBTFUL' and problemas and all(x.get('inherited') for x in problemas):
        return 'heredado'
    return MARCA[verdict['mark']]


def referencia():
    ref = {k: CORREGIDOS.get(k, m) for k, (m, _) in VEREDICTOS.items() if m in ('bien', 'dudoso', 'mal')}
    ids = json.loads((OUT / 'revision-ancha.json').read_text())['ejercicios']
    return {e['quizId']: ref[e['quizId'][-4:]] for e in ids if e['quizId'][-4:] in ref}


def correr(etiqueta, tope, cortos):
    c2 = (OUT / 'c2-id.txt').read_text().strip()
    ref = referencia()
    quiz = [q for q in ref if not cortos or q[-4:] in cortos]
    args = ['bash', str(CLI), '--workdir', str(WORK), 'batch', 'mark', c2, '-f', 'json', '--concurrency', '3']
    for q in quiz:
        args += ['--quiz', q]
    if int(tope):
        args += ['--budget', str(tope)]
    inicio = time.monotonic()
    p = subprocess.run(args, cwd=REPO, env=ENV, capture_output=True, text=True, timeout=7200)
    (AQUI / f'marcas-{etiqueta}.stderr.log').write_text(p.stderr)
    if p.returncode:
        raise SystemExit(p.stderr[-2000:])
    r = json.loads(p.stdout)
    (AQUI / f'marcas-{etiqueta}.json').write_text(json.dumps(r, ensure_ascii=False, indent=1))
    filas = []
    for row in r['rows']:
        mia = ref[row['quizId']]
        suya = marca(row.get('verdict'))
        filas.append({'id': row['quizId'][-4:], 'mia': mia, 'revisor': suya, 'estado': row['state'],
                      'coincide': mia == suya or (mia != 'bien' and suya in MARCAN),
                      'razon': row['verdict']['reason'] if row.get('verdict') else row.get('detail'),
                      'problemas': row['verdict']['problems'] if row.get('verdict') else []})
    marcados_mios = [f for f in filas if f['mia'] != 'bien']
    bien_mios = [f for f in filas if f['mia'] == 'bien']
    resumen = {'consultas': r['spent'], 'segundos': round(time.monotonic() - inicio), 'version': r['reviewerVersion'],
               'motor': r['runtime'], 'ejercicios': len(filas),
               'veLoQueYoMarque': sum(f['revisor'] in MARCAN for f in marcados_mios),
               'yoMarque': len(marcados_mios),
               'marcaLoQueYoDiPorBueno': sum(f['revisor'] in MARCAN for f in bien_mios),
               'heredados': sum(f['revisor'] == 'heredado' for f in filas),
               'yoDiPorBueno': len(bien_mios), 'sinMarca': sum(f['revisor'] == 'sin marca' for f in filas)}
    salida = {'leyenda': {'mia': 'mi lectura a mano (referencia)', 'revisor': 'la marca del revisor',
                          'coincide': 'los dos dicen bien, o los dos marcan algo (dudoso, heredado o mal)'},
              'resumen': resumen, 'discrepancias': [f for f in filas if not f['coincide']], 'filas': filas}
    (AQUI / f'comparacion-{etiqueta}.json').write_text(json.dumps(salida, ensure_ascii=False, indent=1))
    print(json.dumps(resumen, ensure_ascii=False))
    for f in salida['discrepancias']:
        print(f"  {f['id']} yo={f['mia']} revisor={f['revisor']}: {f['razon']}")


if __name__ == '__main__':
    correr(sys.argv[1], sys.argv[2], sys.argv[3:])
