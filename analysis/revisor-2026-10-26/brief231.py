# Arma el brief 231: el tramo grave lanzado (la estimación de hoy contra la del 225) y el revisor pulido (versión
# cb2111fe, sin el «the» ni el tipo de oración) contra el del brief 230 (a67dac84), con mi lectura final de cada caso.
# Uso: brief231.py   (después de remarcar.py, marcar.py v3 0 y comparar100.py v3)
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402
from brief229 import e, lado  # noqa: E402
from brief230 import ANCHA, MARCAN, dijo, grupo, lectura, marca_pill  # noqa: E402
from marcar import marca, CORREGIDOS  # noqa: E402

AQUI = Path(__file__).parent
RAIZ = AQUI.parent.parent
CABEZA = RAIZ / 'docs/briefs/230-el-revisor-recalibrado.html'
SALIDA = RAIZ / 'docs/briefs/231-tramo-grave-y-revisor.html'

# Mi lectura de lo que la versión de hoy dice distinto de la del brief 230; se llena leyendo cada caso entero.
NUEVAS = {
    '1ecd': ('heredado', 'De acuerdo, y se me había pasado: «I love a lot of vegetables» es «Me encantan muchas verduras»; '
                         '«Me gustan mucho» dice otra cosa. Leve, ya estaba.'),
    '1e9b': ('mal', 'Se le pasa, y la versión anterior lo marcaba: con «she» escrito, el alumno ya no ordena be y el '
                    'sujeto, que es lo que practica el tema.'),
    '1e9c': ('dudoso', 'Lo marca, más fuerte que yo: «Where is he at?» es coloquial; para mí, dudoso.'),
}

TABLA = ('<div class="tw casos"><table><thead><tr><th>Caso</th><th>Producción</th><th>Ahora</th>'
         '<th>Revisor del 230</th><th>Revisor de hoy</th><th>Yo</th></tr></thead><tbody>\n{}\n</tbody></table></div>')


# Mi lectura de la prueba ancha (veredictos-ancha.json, con lo que corregí el 26/10), y lo que releí en el 230.
_VEREDICTOS = json.loads((OUT / 'veredictos-ancha.json').read_text())['veredictos']
DE_LA_ANCHA = {k: (CORREGIDOS.get(k, m), t) for k, (m, t) in _VEREDICTOS.items()} | ANCHA


def mia(f):
    if f.get('ancha'):
        return NUEVAS.get(f['id']) or DE_LA_ANCHA[f['id']]
    return NUEVAS.get(f['id']) or lectura(f)


def fila(f, antes):
    yo, texto = mia(f)
    return (f'<tr><td>{e(f["id"])}<small>{e(f["tema"])}</small></td>'
            f'<td>{lado(f["produccion"])}</td><td>{lado(f["correccion"], f["produccion"])}</td>'
            f'<td>{dijo(antes["ahora"], antes["razon"], antes["problemas"])}</td>'
            f'<td>{dijo(f["ahora"], f["razon"], f["problemas"])}</td>'
            f'<td>{marca_pill(yo)}<small>{e(texto)}</small></td></tr>')


def de_la_ancha(etiqueta):
    """Los 51 de la prueba ancha como filas de comparar100: los dos lados, la marca y los problemas."""
    filas = {}
    for r in json.loads((AQUI / f'marcas-{etiqueta}.json').read_text())['rows']:
        k = r['quizId'][-4:]
        filas[k] = {'id': k, 'tema': r['topic'], 'produccion': r['before'], 'correccion': r['after'], 'ancha': True,
                    'antes': None, 'ahora': marca(r.get('verdict')), 'razon': (r.get('verdict') or {}).get('reason'),
                    'problemas': (r.get('verdict') or {}).get('problems') or []}
    return filas


def cuenta(filas, ref):
    """Cuántas marca, cuántas reales, cuántas sin razón y cuántas se le pasan, según mi lectura."""
    marcadas = [f for f in filas if f['ahora'] in MARCAN]
    falsas = [f for f in marcadas if ref(f) == 'bien']
    pasan = [f for f in filas if f['ahora'] not in MARCAN and ref(f) != 'bien']
    return {'marcadas': len(marcadas), 'reales': len(marcadas) - len(falsas), 'falsas': len(falsas),
            'pasan': len(pasan), 'heredados': sum(f['ahora'] == 'heredado' for f in filas)}


if __name__ == '__main__':
    v2 = {f['id']: f for f in json.loads((AQUI / 'comparacion-100-v2.json').read_text())['filas']}
    v3 = json.loads((AQUI / 'comparacion-100-v3.json').read_text())['filas']
    for f in v3:
        f['antes'] = v2[f['id']]['antes']
    ancha2, ancha3 = de_la_ancha('v2'), de_la_ancha('v3')
    # Lo que cambió entre las dos versiones tiene que estar leído.
    cambiaron = [f for f in v3 if f['ahora'] != v2[f['id']]['ahora']]
    cambiaron_ancha = [ancha3[k] for k in ancha3 if ancha3[k]['ahora'] != ancha2[k]['ahora']]
    # Un caso que pasa a marcar lo que doy por bueno, o a dar por bueno lo que marco, y no está leído.
    previo = lambda f: v2.get(f['id']) or ancha2[f['id']]
    faltan = [f['id'] for f in cambiaron + cambiaron_ancha if f['id'] not in NUEVAS
              and grupo(f, mia(f)[0]) in ('falsa', 'pasa') and grupo(previo(f), mia(previo(f))[0]) != grupo(f, mia(f)[0])]
    if faltan:
        raise SystemExit(f'Faltan leer: {faltan}')
    c100 = {'v2': cuenta(list(v2.values()), lambda f: mia(f)[0]), 'v3': cuenta(v3, lambda f: mia(f)[0])}
    c51 = {v: cuenta(list(a.values()), lambda f: mia(f)[0]) for v, a in (('v2', ancha2), ('v3', ancha3))}
    filas = [fila(f, v2[f['id']]) for f in cambiaron] + [fila(f, ancha2[f['id']]) for f in cambiaron_ancha]
    casos = (f'<details class="grupo" open><summary><b>Lo que cambió entre las dos versiones · {len(filas)}</b> '
             f'<span>Las 100 del brief 229 y los 51 de la prueba ancha.</span></summary>\n'
             f'{TABLA.format(chr(10).join(filas))}\n</details>')
    grave = json.loads((OUT / 'estimacion-tramo-grave.json').read_text())
    valores = {'CASOS': casos, 'GRAVE_EJERCICIOS': str(grave['selectedQuizzes']),
               'GRAVE_MINIMO': f'{grave["minimumQueries"]:,}'.replace(',', '.')}
    for v in ('v2', 'v3'):
        for k, n in c100[v].items():
            valores[f'C100_{k.upper()}_{v.upper()}'] = str(n)
        for k, n in c51[v].items():
            valores[f'C51_{k.upper()}_{v.upper()}'] = str(n)
    plantilla = (AQUI / 'brief231-plantilla.html').read_text()
    cabeza = CABEZA.read_text().split('</head>')[0].replace('<title>El revisor recalibrado</title>',
                                                              '<title>Tramo grave en marcha</title>')
    cuerpo = plantilla
    for clave, valor in valores.items():
        cuerpo = cuerpo.replace('{{' + clave + '}}', valor)
    SALIDA.write_text(cabeza + cuerpo)
    print(SALIDA, {k: v for k, v in valores.items() if k != 'CASOS'}, 'sin llenar:',
          re.findall(r'\{\{[A-Z_0-9]+\}\}', cuerpo))
