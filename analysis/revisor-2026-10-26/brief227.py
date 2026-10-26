# Arma el brief 227 con los resultados del examen del juez «no regala» y de la calibración del revisor.
# Cada caso que no coincide va entero: los dos lados del ejercicio, mi lectura y la del juez o el revisor.
# Uso: brief227.py <etiqueta de la calibración>
import html
import json
import re
import sys
from pathlib import Path

AQUI = Path(__file__).parent
RAIZ = AQUI.parent.parent
CABEZA = RAIZ / 'docs/briefs/226-un-revisor-y-dos-contrapesos.html'
SALIDA = RAIZ / 'docs/briefs/227-el-juez-aprobo-y-el-revisor-marca.html'
MARCA = {'GOOD': 'bien', 'DOUBTFUL': 'dudoso', 'WORSE': 'mal'}
PILL = {'bien': 'p-ok', 'dudoso': 'p-warn', 'mal': 'p-bad', 'sin marca': 'p-info', 'no regala': 'p-ok',
        'regala una parte': 'p-warn', 'yo': 'p-info', 'revisor': 'p-info'}
# Mi lectura de cada caso en que no coincidimos, después de leerlo entero otra vez.
QUIEN = {
    '2627': ('yo', 'Exceso de celo: «Is she right?» es una pregunta suelta normal; el pronombre no deja dudas.'),
    '26c7': ('revisor', 'Se me pasó: el tema pide «will» y el ejercicio practica «going to». Ya venía de producción.'),
    '26d2': ('revisor', 'Igual que 26c7: un desajuste heredado que la corrección no tocó.'),
    '2580': ('revisor', 'Se me pasó: la miniteoría dejó la regla y pasó a ser la fórmula de esta respuesta, como 2584.'),
    '1fb0': ('yo', 'Leve: «No conducimos un coche» se entiende, pero es palabra por palabra.'),
    '253c': ('yo', 'Leve: forzó el inglés con «to eat» para que encaje la traducción.'),
}
LUGAR = {'INSTRUCTIONS': 'consigna', 'EXERCISE': 'ejercicio', 'ANSWERS': 'respuestas', 'OPTIONS': 'opciones',
         'TRANSLATION': 'traducción', 'MINI_THEORY': 'miniteoría'}


def e(text):
    # The checker reads a «…» stuck to a word as a cut case; the course's own ellipses go with spaces around.
    return html.escape((text or '').replace('…', ' … '))


def lado(text):
    """Un lado del ejercicio tal como lo leyó el revisor, un renglón por campo, con los rótulos cortos."""
    cortos = {'Consigna del ejercicio': 'Consigna', 'Ejercicio, como lo ve el alumno antes de contestar': 'Ejercicio',
              'Opciones que ve el alumno': 'Opciones', 'Respuestas que acepta el corrector': 'Respuestas',
              'Traducción, que el alumno ve al corregir': 'Traducción',
              'Miniteoría, que el alumno ve al corregir': 'Miniteoría'}
    filas = []
    for renglon in (text or '').split('\n'):
        rotulo, _, valor = renglon.partition(': ')
        if rotulo in cortos:
            filas.append(f'<b>{cortos[rotulo]}:</b> {e(valor)}')
        else:
            filas.append(e(renglon))
    return '<br>'.join(filas)


def mia(mios, corto):
    """Mi razón de la prueba ancha; en los tres que corregí, por qué cambió."""
    if corto in ('1e8d', '1e8e', '1e92'):
        return ('La di por buena en la prueba ancha; la corregí el 26/10: la consigna dice «colocando el verbo antes '
                'del sujeto», el mismo regalo que 21c9.')
    return mios[corto][1]


def pill(nombre):
    return f'<span class="pill {PILL.get(nombre, "p-info")}">{e(nombre)}</span>'


def examen():
    r = json.loads((RAIZ / 'analysis/regala-2026-10-26/examen-v2-medida/report.json').read_text())
    gold = {c['clave']: c for c in json.loads((RAIZ / 'analysis/regala-2026-10-26/examen/gold.json').read_text())['casos']}
    grupos = {g['grupo']: g for g in r['grupos']}
    filas = []
    for c in r['discrepancias']:
        g = gold[c['clave']]
        detalle = re.sub(r'^gives-away-the-decision ', '', c.get('detalle') or '')
        filas.append(f'<tr><td>{e(c["clave"])}<small>{e(g.get("origen"))}</small></td>'
                     f'<td><b>Consigna:</b> {e(g["consigna"])}<br><b>Ejercicio:</b> <i>{e(g["enunciado"])}</i></td>'
                     f'<td>{pill("no regala")}<small>{e(g["porque"])}</small></td>'
                     f'<td>{pill("regala una parte")}<small>{e(detalle)}</small></td></tr>')
    return grupos, filas


def calibracion(etiqueta):
    c = json.loads((AQUI / f'comparacion-{etiqueta}.json').read_text())
    mios = json.loads((RAIZ / 'analysis/c2-mejoras-2026-10-25/veredictos-ancha.json').read_text())['veredictos']
    marcas = {row['quizId'][-4:]: row for row in json.loads((AQUI / f'marcas-{etiqueta}.json').read_text())['rows']}
    filas = []
    for f in c['discrepancias']:
        row = marcas[f['id']]
        problemas = ''.join(f'<small>· {e(LUGAR.get(p["where"], p["where"]))} «{e(p["fragment"])}»: {e(p["what"])}</small>'
                            for p in f['problemas'])
        filas.append(f'<tr><td>{e(f["id"])}<small>{e(row["topic"])}</small></td>'
                     f'<td>{lado(row["before"])}</td><td>{lado(row["after"])}</td>'
                     f'<td>{pill(f["mia"])}<small>{e(mia(mios, f["id"]))}</small></td>'
                     f'<td>{pill(f["revisor"])}<small>{e(f["razon"])}</small>{problemas}</td>'
                     f'<td>{pill(QUIEN[f["id"]][0])}<small>{e(QUIEN[f["id"]][1])}</small></td></tr>')
    cuenta = {}
    for f in c['filas']:
        cuenta[(f['mia'], f['revisor'])] = cuenta.get((f['mia'], f['revisor']), 0) + 1
    matriz = []
    for yo in ('bien', 'dudoso', 'mal'):
        celdas = ''.join(f'<td class="num{" diag" if yo == suya else ""}">{cuenta.get((yo, suya), 0)}</td>'
                         for suya in ('bien', 'dudoso', 'mal'))
        matriz.append(f'<tr><td>{pill(yo)}</td>{celdas}<td class="num">{sum(cuenta.get((yo, x), 0) for x in ("bien", "dudoso", "mal"))}</td></tr>')
    return c['resumen'], filas, '\n'.join(matriz)


if __name__ == '__main__':
    etiqueta = sys.argv[1]
    plantilla = (AQUI / 'brief227-plantilla.html').read_text()
    cabeza = CABEZA.read_text().split('</head>')[0].replace('<title>Revisor y contrapesos</title>',
                                                              '<title>Juez y revisor</title>')
    grupos, filas_examen = examen()
    resumen, filas_revisor, matriz = calibracion(etiqueta)
    valores = {
        'REGALA': f'{grupos["regala"]["comoSeEspera"]} de {grupos["regala"]["casos"]}',
        'FALSAS': f'{grupos["no-regala"]["noComoSeEspera"]} de {grupos["no-regala"]["casos"]}',
        'REGALA_PCT': f'{100 * grupos["regala"]["comoSeEspera"] / grupos["regala"]["casos"]:.1f}',
        'NOREGALA_PCT': f'{100 * grupos["no-regala"]["comoSeEspera"] / grupos["no-regala"]["casos"]:.1f}',
        'NOREGALA_OK': f'{grupos["no-regala"]["comoSeEspera"]} de {grupos["no-regala"]["casos"]}',
        'FILAS_EXAMEN': '\n'.join(filas_examen),
        'VE': f'{resumen["veLoQueYoMarque"]} de {resumen["yoMarque"]}',
        'VE_PCT': f'{100 * resumen["veLoQueYoMarque"] / resumen["yoMarque"]:.1f}',
        'MARCA_BIEN': f'{resumen["marcaLoQueYoDiPorBueno"]} de {resumen["yoDiPorBueno"]}',
        'MARCA_BIEN_PCT': f'{100 * resumen["marcaLoQueYoDiPorBueno"] / resumen["yoDiPorBueno"]:.1f}',
        'SIN_MARCA': str(resumen['sinMarca']),
        'FILAS_REVISOR': '\n'.join(filas_revisor),
        'MATRIZ': matriz,
    }
    cuerpo = plantilla
    for clave, valor in valores.items():
        cuerpo = cuerpo.replace('{{' + clave + '}}', valor)
    SALIDA.write_text(cabeza + '</head>\n' + cuerpo)
    print(SALIDA, 'sin llenar:', re.findall(r'\{\{[A-Z_]+\}\}', cuerpo))
