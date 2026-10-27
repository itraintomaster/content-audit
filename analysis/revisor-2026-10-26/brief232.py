# Arma el brief 232: las 1.725 marcas del revisor (cb2111fe) sobre la C2 después del tramo grave, leídas una por una
# (lectura_c2.py), con cada caso entero. Gratis: lee marcas-c2.json (leer_marcas.py) y la tanda, no consulta.
# Uso: brief232.py   (después de leer_marcas.py)
import json
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import WORK  # noqa: E402
from brief229 import e, lado, pill  # noqa: E402
from brief230 import dijo  # noqa: E402
from lectura_c2 import (CAUSAS, MAL, DUDOSO, NOTAS, HEREDADO_MAL_SEPARADOS, MUESTRA_TRAMO,  # noqa: E402
                        BIEN_QUE_NO)

AQUI = Path(__file__).parent
RAIZ = AQUI.parent.parent
CABEZA = RAIZ / 'docs/briefs/231-tramo-grave-y-revisor.html'
SALIDA = RAIZ / 'docs/briefs/232-las-marcas-de-la-c2.html'
CORRIDA = 'ui-2026-10-26T22-45-04.197082Z-batch-recorrect'

VEREDICTO = {'real': ('p-bad', 'real'), 'leve': ('p-warn', 'leve'), 'falsa': ('p-ok', 'sin problema'),
             'decidir': ('p-info', 'decidís vos'), 'bien': ('p-ok', 'bien')}
TABLA = ('<div class="tw casos"><table><thead><tr><th>Caso</th><th>Producción</th><th>Ahora</th><th>El revisor</th>'
         '<th>Yo</th></tr></thead><tbody>\n{}\n</tbody></table></div>')


def yo(veredicto, causa, nota):
    clase, rotulo = VEREDICTO[veredicto]
    detalle = CAUSAS.get(causa, '') + (f'. {nota}' if nota else '')
    return f'<span class="pill {clase}">{rotulo}</span><small>{e(detalle)}</small>'


def fila(f, veredicto, causa):
    return _fila(f, veredicto, causa).replace('…', '⋯').replace('...', '⋯')


def _fila(f, veredicto, causa):
    """El caso entero. Los puntos suspensivos del curso («Is he…?», «(o...)») van como «⋯»: son texto del curso, no
    un recorte, y así no se confunden con uno."""
    tramo = '<small><b>tramo grave</b></small>' if f['grave'] else ''
    return (f'<tr><td>{e(f["id"])}<small>{e(f["tema"])}</small>{tramo}</td>'
            f'<td>{lado(f["produccion"])}</td><td>{lado(f["correccion"], f["produccion"])}</td>'
            f'<td>{dijo(f["marca"], f["razon"], f["problemas"])}</td>'
            f'<td>{yo(veredicto, causa, NOTAS.get(f["id"]))}</td></tr>')


def grupo(titulo, bajada, filas, abierto=False):
    return (f'<details class="grupo"{" open" if abierto else ""}><summary><b>{e(titulo)} · {len(filas)}</b> '
            f'<span>{e(bajada)}</span></summary>\n{TABLA.format(chr(10).join(filas))}\n</details>')


def tramo_grave():
    """Cómo terminó cada ejercicio del tramo grave, según las decisiones de la mesa que guardó la corrida."""
    tanda = json.loads((WORK / f'.content-audit/batches/{(AQUI.parent / "c2-mejoras-2026-10-25/c2-id.txt").read_text().strip()}.json').read_text())
    corrida = next(r for r in tanda['selectiveRuns'] if r.get('id') == CORRIDA)
    tareas = set(corrida['taskIds'])
    por = {}
    for d in tanda['panelDecisions']:
        if d.get('taskIds') and set(d['taskIds']) <= tareas:
            for q in d['quizIds']:
                por.setdefault(q, []).append(d['outcome'])
    fin = Counter('ACCEPTED' if 'ACCEPTED' in o else o[-1] for o in (por.get(q, ['NINGUNA']) for q in corrida['quizIds']))
    return corrida, fin


if __name__ == '__main__':
    marcas = json.loads((AQUI / 'marcas-c2.json').read_text())
    filas = {f['id']: f for f in marcas['filas']}
    leidas = {**MAL, **DUDOSO, **HEREDADO_MAL_SEPARADOS}
    marcadas = [k for k, f in filas.items() if f['marca'] in ('mal', 'dudoso')]
    faltan = [k for k in marcadas if k not in leidas]
    if faltan:
        raise SystemExit(f'Faltan leer: {faltan}')
    por_veredicto = Counter(v for v, _ in leidas.values())
    reales = Counter(c for v, c in leidas.values() if v == 'real')
    caso = Counter(v for v, c in leidas.values() if c == 'CASO')
    heredados = sum(f['marca'] == 'heredado' for f in filas.values())
    muestra = [k for k, f in filas.items() if f['muestra']] + list(MUESTRA_TRAMO)
    corrida, fin = tramo_grave()
    grave = Counter(leidas.get(k, ('bien', None))[0] if filas[k]['marca'] != 'heredado' or k in HEREDADO_MAL_SEPARADOS
                    else 'heredado' for k, f in filas.items() if f['grave'])

    def de(veredicto, causa=None):
        return [fila(filas[k], v, c) for k, (v, c) in sorted(leidas.items(), key=lambda x: filas[x[0]]['quizId'])
                if v == veredicto and (causa is None or c == causa)]

    familias = [c for c, _ in reales.most_common()]
    casos = [grupo(f'Real · {CAUSAS[c]}', 'no se publica así' if i == 0 else '', de('real', c),
                   abierto=(i == 0)) for i, c in enumerate(familias)]
    casos.append(grupo('Para que decidas vos · Contable o incontable', 'el tema pide clasificar',
                       de('decidir')))
    casos.append(grupo('Leves, sin la miniteoría del caso', 'no frenan',
                       [x for c in CAUSAS if c != 'CASO' for x in de('leve', c)]))
    casos.append(grupo('Leves · la miniteoría dicta la respuesta sin el porqué', 'una orden, sin porqué',
                       de('leve', 'CASO')))
    casos.append(grupo('Sin problema · la miniteoría explica el caso con su porqué', 'marcadas dudosas',
                       de('falsa', 'CASO')))
    casos.append(grupo('Sin problema · otras', 'lo decidido el 25/10',
                       [x for c in CAUSAS if c != 'CASO' for x in de('falsa', c)]))
    casos.append(grupo('Muestra de lo que da por bueno', '20 al azar, 12 del tramo grave',
                       [fila(filas[k], *BIEN_QUE_NO.get(k, ('bien', None))) for k in muestra]))

    valores = {
        'CASOS': '\n'.join(casos),
        'CAMBIADOS': f'{marcas["cambiados"]:,}'.replace(',', '.'),
        'BIEN': f'{sum(f["marca"] == "bien" for f in filas.values()):,}'.replace(',', '.'),
        'MARCADAS': str(len(marcadas) + heredados),
        'MAL': str(sum(filas[k]['marca'] == 'mal' for k in marcadas)),
        'DUDOSO': str(sum(filas[k]['marca'] == 'dudoso' for k in marcadas)),
        'HEREDADOS': str(heredados), 'HEREDADOS_BIEN': str(heredados - len(HEREDADO_MAL_SEPARADOS)),
        'REALES': str(por_veredicto['real']), 'DECIDIR': str(por_veredicto['decidir']),
        'LEVES': str(por_veredicto['leve']), 'FALSAS': str(por_veredicto['falsa']),
        'CASO_LEVE': str(caso['leve']), 'CASO_FALSA': str(caso['falsa']),
        'MUESTRA': str(len(muestra)), 'MUESTRA_BIEN': str(len(muestra) - len(BIEN_QUE_NO)),
        'GRAVE_EJERCICIOS': str(len(corrida['quizIds'])), 'GRAVE_ACEPTADOS': str(fin['ACCEPTED']),
        'GRAVE_SIN_GANADOR': str(fin['NO_WINNER']), 'GRAVE_SIN_INTENTAR': str(fin['NOT_ATTEMPTED']),
        'GRAVE_EMPATE': str(fin['TIE']), 'GRAVE_SIN_DECIDIR': str(fin['NINGUNA']),
        'GRAVE_CONSULTAS': f'{corrida["spent"]:,}'.replace(',', '.'),
        'GRAVE_MARCADOS': str(sum(f['grave'] for f in filas.values())),
        'GRAVE_REALES': str(grave['real']), 'GRAVE_HEREDADOS': str(grave['heredado']),
        'GRAVE_DESCARTE': str(sum(1 for k, (v, c) in leidas.items() if filas[k]['grave'] and c == 'DESCARTE')),
    }
    for c, n in reales.items():
        valores[f'R_{c}'] = str(n)
    total = por_veredicto['real']
    for c in ('PISTA', 'TEMA', 'FORMA', 'AINT', 'DESCARTE'):
        valores[f'P_{c}'] = str(round(100 * reales[c] / total))
    otras = total - sum(reales[c] for c in ('PISTA', 'TEMA', 'FORMA', 'AINT', 'DESCARTE'))
    valores['R_OTRAS'], valores['P_OTRAS'] = str(otras), str(round(100 * otras / total))
    marcadas_n = len(marcadas) + heredados
    for clave, n in (('REALES', total), ('DECIDIR', por_veredicto['decidir']), ('LEVES', por_veredicto['leve']),
                     ('HEREDADOS_BIEN', heredados - len(HEREDADO_MAL_SEPARADOS)), ('FALSAS', por_veredicto['falsa'])):
        valores[f'W_{clave}'] = str(round(100 * n / marcadas_n))
    plantilla = (AQUI / 'brief232-plantilla.html').read_text()
    cabeza = CABEZA.read_text().split('</head>')[0].replace('<title>Tramo grave en marcha</title>',
                                                              '<title>Las marcas de la C2</title>')
    cuerpo = plantilla
    for clave, valor in valores.items():
        cuerpo = cuerpo.replace('{{' + clave + '}}', valor)
    SALIDA.write_text(cabeza + cuerpo)
    print(SALIDA, {k: v for k, v in valores.items() if k != 'CASOS'}, 'sin llenar:',
          re.findall(r'\{\{[A-Z_0-9]+\}\}', cuerpo))
