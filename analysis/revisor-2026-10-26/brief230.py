# Arma el brief 230: el revisor de F-MARCA-R010 (versión a67dac84) contra el de brief 229 (6f899ccf), en las 100
# marcas de la C2 que leí una por una y en la prueba ancha de 51. Cada caso que cambió va entero: los dos lados, lo que
# dijo cada versión y lo que leo yo, con el porqué.
# Uso: brief230.py   (después de remarcar.py, marcar.py v2 0 y comparar100.py)
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402

from brief229 import e, lado, pill, LUGAR, MIA, BIEN_DUDOSOS
from marcar import marca

AQUI = Path(__file__).parent
RAIZ = AQUI.parent.parent
CABEZA = RAIZ / 'docs/briefs/229-las-100-marcas.html'
SALIDA = RAIZ / 'docs/briefs/230-el-revisor-recalibrado.html'

# Mi lectura de lo que la versión de hoy dice distinto de brief 229, después de leer cada caso entero. Dos cambian
# lo que leí entonces: 25b0 es heredado (la negativa en un tema de preguntas, como 25ae) y 1edc se me había pasado.
# El generador se niega a armar el brief si la versión de hoy dice algo nuevo de un caso que no está acá.
NUEVAS = {
    '1edc': ('heredado', 'De acuerdo, y se me había pasado: «No veo ningún hermano» no se dice; lo natural es «I don\'t '
                         'have any brothers». Ya estaba en producción.'),
    '25b0': ('heredado', 'De acuerdo, cambio mi lectura: la negativa en un tema de preguntas yes / no ya estaba, como en '
                         '25ae. Lo de la miniteoría, que yo marcaba, era leve.'),
    '1e7c': ('bien', 'Exceso de celo: «at the house» en una oración suelta es normal. Lo provoqué yo: la instrucción 4 '
                     'nombra «un “the” que presupone algo».'),
    '1eef': ('bien', 'Exceso de celo, por la misma instrucción: «¿Ella hace el trabajo?» se entiende sola.'),
    '1efa': ('dudoso', 'Leve, y se le pasa: la miniteoría aplica la regla a este ejemplo. Ahora queda igual que sus diez '
                       'hermanos.'),
    '1eee': ('heredado', 'Leve, y se le pasa: «¿Comes el almuerzo todos los días?» ya estaba; lo natural es «¿Almorzás '
                         'todos los días?».'),
}

# La prueba ancha (51): mi lectura de cada caso donde el revisor de hoy no coincide con la mía de veredictos-ancha.json.
# Las métricas usan la lectura original; acá digo dónde, releyendo, le doy la razón.
ANCHA = {
    '25f2': ('bien', 'Exceso de celo: «Are the shirts white?» se entiende sola. Lo provocó la instrucción 4, la del «the».'),
    '223b': ('bien', 'Igual: «Were the lessons very hard?» se entiende sola.'),
    '26c7': ('bien', 'Discutible y leve: con «andar en bicicleta», «ride a bike» sería más natural. Ya estaba.'),
    '26d2': ('bien', 'Falsa alarma: compara con el tipo de oración, que el alumno no ve.'),
    '2572': ('bien', 'Falsa alarma por el tipo de oración. Y el cambio mejora: ahora el alumno decide dónde va el adverbio.'),
    '2580': ('dudoso', 'Le doy la razón en la miniteoría: perdió la regla y quedó la fórmula de esta respuesta, como 1eae. '
                       'En lo del tipo de oración, no.'),
    '222d': ('dudoso', 'Leve, de la misma clase que 2580. No es parejo: en 1efa no lo marcó.'),
    '257f': ('heredado', 'Tiene razón y se me había pasado: el corrector rechaza «Sometimes they go to the park.», que '
                         'es correcta. Ya estaba.'),
    '1fb0': ('dudoso', 'Se le pasa: «No conducimos un coche» es literal; se dice «No tenemos auto» o «No manejamos».'),
    '253c': ('dudoso', 'Se le pasa: agregó «to eat» al inglés para que encaje la traducción, en vez de arreglar la '
                       'traducción.'),
}

# Los grupos del brief salen de mi lectura final contra lo que marca hoy el revisor.
GRUPOS = [
    ('falsa', 'Marca sin razón', 'Lo doy por bueno y la versión de hoy lo marca.', True),
    ('pasa', 'Se le pasan', 'Veo algo y la versión de hoy lo da por bueno: todos leves.', True),
    ('nuevo', 'Lo que dejó la corrección', 'Defectos nuevos: siguen marcados, y ninguno como heredado.', True),
    ('heredado', 'Lo que ya estaba en producción', 'Siguen marcados, ahora aparte, como «heredado».', False),
    ('apagada', 'Falsas alarmas que se apagaron', 'La versión de hoy los da por buenos, como yo.', False),
]
MARCAN = ('mal', 'dudoso', 'heredado')


def grupo(f, yo):
    if yo == 'bien':
        return 'falsa' if f['ahora'] in MARCAN else 'apagada' if f['antes'] in MARCAN else None
    if f['ahora'] not in MARCAN:
        return 'pasa'
    return 'heredado' if yo == 'heredado' else 'nuevo'


def marca_pill(nombre):
    return pill(nombre) if nombre != 'heredado' else '<span class="pill p-info">heredado</span>'


def dijo(marca, razon, problemas):
    lista = ''.join(f'<small>· {e(LUGAR.get(p["where"], p["where"]))} «{e(p["fragment"])}»: {e(p["what"])}'
                    f'{" <b>(ya estaba)</b>" if p.get("inherited") else ""}</small>' for p in problemas)
    return f'{marca_pill(marca)}<small>{e(razon)}</small>{lista}'


def lectura(f):
    if f['id'] in ANCHA and f.get('ancha'):
        return ANCHA[f['id']]
    if f['id'] in NUEVAS:
        return NUEVAS[f['id']]
    if f['id'] in MIA:
        grupo, mia, texto = MIA[f['id']]
        return ('heredado' if grupo == 'hereda' else mia), texto
    if f['id'] in BIEN_DUDOSOS:
        return BIEN_DUDOSOS[f['id']]
    return 'bien', 'De acuerdo.'


def fila(f, v1):
    yo, texto = lectura(f)
    return (f'<tr><td>{e(f["id"])}<small>{e(f["tema"])}</small></td>'
            f'<td>{lado(f["produccion"])}</td><td>{lado(f["correccion"], f["produccion"])}</td>'
            f'<td>{dijo(v1["marca"], v1["razon"], v1["problemas"])}</td>'
            f'<td>{dijo(f["ahora"], f["razon"], f["problemas"])}</td>'
            f'<td>{marca_pill(yo)}<small>{e(texto)}</small></td></tr>')


TABLA = ('<div class="tw casos"><table><thead><tr><th>Caso</th><th>Producción</th><th>Ahora</th>'
         '<th>Revisor de antes</th><th>Revisor de hoy</th><th>Yo</th></tr></thead><tbody>\n{}\n</tbody></table></div>')


if __name__ == '__main__':
    cien = json.loads((AQUI / 'comparacion-100-v2.json').read_text())
    v1 = {f['id']: f for f in json.loads((AQUI / 'primeras100.json').read_text())['filas']}
    filas = cien['filas']
    # Lo que la versión de hoy dice distinto de brief 229 tiene que estar leído.
    faltan = [f['id'] for f in filas if f['id'] not in NUEVAS and f['clase'] not in (
        'bien igual', 'falsa alarma apagada', 'nuevo bien visto', 'heredado aparte', 'leve sin marcar')]
    if faltan:
        raise SystemExit(f'Faltan leer: {faltan}')
    por_grupo = {}
    for f in filas:
        g = grupo(f, lectura(f)[0])
        if g:
            por_grupo.setdefault(g, []).append(f)
    bloques = []
    for g, titulo, bajada, abierto in GRUPOS:
        del_grupo = por_grupo.get(g, [])
        if not del_grupo:
            continue
        tabla = TABLA.format('\n'.join(fila(f, v1[f['id']]) for f in del_grupo))
        bloques.append(f'<details class="grupo"{" open" if abierto else ""}><summary><b>{e(titulo)} · {len(del_grupo)}'
                       f'</b> <span>{e(bajada)}</span></summary>\n{tabla}\n</details>')
    # La prueba ancha: los casos donde el revisor de hoy no coincide con mi lectura, enteros.
    c2 = json.loads((AQUI / 'comparacion-v2.json').read_text())
    filas_v1 = {r['quizId'][-4:]: r for r in json.loads((AQUI / 'marcas-v1.json').read_text())['rows']}
    filas_v2 = {r['quizId'][-4:]: r for r in json.loads((AQUI / 'marcas-v2.json').read_text())['rows']}
    sin_leer = [f['id'] for f in c2['discrepancias'] if f['id'] not in ANCHA]
    if sin_leer:
        raise SystemExit(f'Faltan leer de la prueba ancha: {sin_leer}')
    ancha_filas = []
    for d in c2['discrepancias']:
        r1, r2 = filas_v1[d['id']], filas_v2[d['id']]
        ancha_filas.append(fila(
            {'id': d['id'], 'tema': r2['topic'], 'produccion': r2['before'], 'correccion': r2['after'], 'ancha': True,
             'ahora': marca(r2['verdict']), 'razon': r2['verdict']['reason'], 'problemas': r2['verdict']['problems']},
            {'marca': marca(r1['verdict']), 'razon': r1['verdict']['reason'], 'problemas': r1['verdict']['problems']}))
    bloques.append(f'<details class="grupo"><summary><b>Prueba ancha: donde no coincidimos · {len(ancha_filas)}</b> '
                   f'<span>Los 51 de la calibración: lo que marca y yo no, y lo que se le pasa.</span></summary>\n'
                   f'{TABLA.format(chr(10).join(ancha_filas))}\n</details>')
    ancha = {v: json.loads((AQUI / f'comparacion-{v}.json').read_text())['resumen'] for v in ('v1', 'v2')}
    n = {g: len(por_grupo.get(g, [])) for g, *_ in GRUPOS}
    marcados = [f for f in filas if f['ahora'] in MARCAN]
    valores = {
        'MARCADOS_V1': str(sum(f['marca'] in MARCAN for f in v1.values())),
        'MARCADOS_V2': str(len(marcados)),
        'MAL_V1': str(sum(f['marca'] == 'mal' for f in v1.values())),
        'MAL_V2': str(sum(f['ahora'] == 'mal' for f in filas)),
        'DUDOSO_V2': str(sum(f['ahora'] == 'dudoso' for f in filas)),
        'HEREDADO_V2': str(sum(f['ahora'] == 'heredado' for f in filas)),
        'REALES_V2': str(len(marcados) - n['falsa']),
        'FALSAS_V1': str(sum(MIA[k][0] == 'falsa' for k in MIA)), 'FALSAS_V2': str(n['falsa']),
        'APAGADAS': str(n['apagada']), 'PASAN': str(n['pasa']), 'NUEVOS': str(n['nuevo']),
        'HEREDADOS': str(n['heredado']),
        'ANCHA_VE_V1': str(ancha['v1']['veLoQueYoMarque']), 'ANCHA_VE_V2': str(ancha['v2']['veLoQueYoMarque']),
        'ANCHA_YO': str(ancha['v2']['yoMarque']),
        'ANCHA_FALSA_V1': str(ancha['v1']['marcaLoQueYoDiPorBueno']),
        'ANCHA_FALSA_V2': str(ancha['v2']['marcaLoQueYoDiPorBueno']),
        'ANCHA_BIEN': str(ancha['v2']['yoDiPorBueno']), 'ANCHA_HEREDADOS': str(ancha['v2']['heredados']),
        'CASOS': '\n'.join(bloques),
    }
    base = len(marcados)
    for clave, valor in [('NUEVOS', n['nuevo']), ('HEREDADOS', n['heredado']), ('FALSAS', n['falsa']), ('PASAN', n['pasa'])]:
        valores[clave + '_PCT'] = f'{100 * valor / base:.1f}'
    valores['ANCHA_VE_PCT'] = f'{100 * ancha["v2"]["veLoQueYoMarque"] / ancha["v2"]["yoMarque"]:.1f}'
    valores['ANCHA_FALSA_PCT'] = f'{100 * ancha["v2"]["marcaLoQueYoDiPorBueno"] / ancha["v2"]["yoDiPorBueno"]:.1f}'
    # Lo que falta pedir en toda la tanda: leer es gratis, sin --budget no consulta.
    tanda = subprocess.run(['bash', str(RAIZ / 'audit-cli.sh'), '--workdir', str(WORK), 'batch', 'mark',
                            (OUT / 'c2-id.txt').read_text().strip(), '-f', 'json'],
                           cwd=REPO, env=ENV, capture_output=True, text=True, timeout=900)
    valores['FALTAN'] = f'{json.loads(tanda.stdout)["pending"]:,}'.replace(',', '.')
    plantilla = (AQUI / 'brief230-plantilla.html').read_text()
    cabeza = CABEZA.read_text().split('</head>')[0].replace('<title>Las cien marcas</title>',
                                                              '<title>El revisor recalibrado</title>')
    cuerpo = plantilla
    for clave, valor in valores.items():
        cuerpo = cuerpo.replace('{{' + clave + '}}', valor)
    SALIDA.write_text(cabeza + cuerpo)
    print(SALIDA, valores | {'CASOS': len(bloques)}, 'sin llenar:', re.findall(r'\{\{[A-Z_0-9]+\}\}', cuerpo))
