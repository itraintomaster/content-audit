# Arma el brief 229 con las 100 marcas que José pidió desde Correcciones el 26/10/2026 y mi lectura de cada una.
# Cada ejercicio marcado va entero: los dos lados, lo que dijo el revisor y lo que leo yo, con el porqué.
# Uso: brief229.py   (lee primeras100.json; primero correr primeras100.py)
import html
import json
import re
from pathlib import Path

AQUI = Path(__file__).parent
RAIZ = AQUI.parent.parent
CABEZA = RAIZ / 'docs/briefs/228-marcas-en-correcciones.html'
SALIDA = RAIZ / 'docs/briefs/229-las-100-marcas.html'
LUGAR = {'INSTRUCTIONS': 'consigna', 'EXERCISE': 'ejercicio', 'ANSWERS': 'respuestas', 'OPTIONS': 'opciones',
         'TRANSLATION': 'traducción', 'MINI_THEORY': 'miniteoría'}
PILL = {'bien': 'p-ok', 'dudoso': 'p-warn', 'mal': 'p-bad'}

# Mi lectura de cada ejercicio marcado, después de leerlo entero. El grupo dice de dónde sale lo que marcó.
_FORMA = ('Dudoso, no mal: el corrector acepta la forma larga desde el 30/9. Lo real es la miniteoría: que el sujeto '
          'esté escrito no obliga a contraer.')
_CONSIGNA_DEL_TEMA = ('Falsa alarma: lo compara con la consigna del tema, que el alumno no ve. Lo que ve dice «Elige» '
                      'y trae opciones.')
_IT = 'Exceso de celo: «It» sin decir qué es, en una oración suelta de A1, es normal.'
_WH = 'De acuerdo, heredado: una pregunta con wh- en un tema de preguntas yes / no. Sacar «short forms» está bien.'
MIA = {
    '1eeb': ('corrige', 'mal', 'De acuerdo: la pista trae «Do», y elegir entre do y does es lo que practica el tema.'),
    '25be': ('corrige', 'mal', 'De acuerdo: acepta y enseña «ain\'t» en A1. Sumar «are not» y «\'re not» sí estaba bien.'),
    '1e9b': ('corrige', 'mal', 'Peor de lo que dice: con «she» escrito ya no practica el orden wh- + be + sujeto, que es '
                               'el tema. Para mí, mal.'),
    '25ad': ('corrige', 'dudoso', _FORMA), '25b6': ('corrige', 'dudoso', _FORMA),
    '25b7': ('corrige', 'dudoso', _FORMA), '25b9': ('corrige', 'dudoso', _FORMA),
    '25ac': ('corrige', 'dudoso', 'De acuerdo: «colocando el verbo antes del sujeto» regala la inversión, como 21c9.'),
    '1e8b': ('corrige', 'dudoso', 'De acuerdo: «Con la pista (She), elegí Is she» le dice qué elegir.'),
    '1eae': ('corrige', 'dudoso', 'De acuerdo: la miniteoría pasó a ser la receta de esta pregunta.'),
    '25b0': ('corrige', 'dudoso', 'Leve: «en el hueco se puede escribir…» habla del ejercicio, no de la regla.'),
    '1efa': ('corrige', 'dudoso', 'Leve, y no es parejo: diez hermanos con la misma miniteoría quedaron «bien».'),
    '25ae': ('hereda', 'dudoso', 'Dudoso por herencia, no mal: el corrector acepta «is not». Queda una negativa en un '
                                 'tema de preguntas yes / no, que ya estaba.'),
    '25af': ('hereda', 'dudoso', _WH), '25b1': ('hereda', 'dudoso', _WH), '25b4': ('hereda', 'dudoso', _WH),
    '25b5': ('hereda', 'dudoso', _WH), '25ba': ('hereda', 'dudoso', _WH),
    '1e97': ('hereda', 'dudoso', 'De acuerdo, heredado y leve: «Am I brown?» suena raro.'),
    '1eee': ('hereda', 'dudoso', 'De acuerdo, heredado y leve: «¿Almorzás todos los días?» sería lo natural.'),
    '1e62': ('falsa', 'bien', _IT), '1e6b': ('falsa', 'bien', _IT), '1e8f': ('falsa', 'bien', _IT),
}
for corto in ('1e60', '1e63', '1e6d', '1e74', '1e75', '1e76', '1e77', '1e78', '1e79', '1e7a', '1e7b', '1e7c', '1e7d',
              '1e7e', '1e7f', '1e80', '1e82', '1e83', '1e84', '1e85', '1e86'):
    MIA[corto] = ('falsa', 'bien', _CONSIGNA_DEL_TEMA)
# Los «bien» del revisor que yo miraría.
BIEN_DUDOSOS = {
    '1ed6': ('dudoso', 'Leve: «luce sombreros» traducía mejor «sport»; «lleva puestos sombreros» es más torpe.'),
    '1e9c': ('dudoso', 'Discutible: acepta «is he at»; «Where is he at?» es coloquial.'),
}
GRUPOS = [
    ('corrige', 'Los dejó la corrección', 'Defectos nuevos: la corrección los metió. Rechazarlos o corregirlos.'),
    ('hereda', 'Ya estaban en producción', 'La corrección no los tocó; no los publicaría así, pero no son del cambio.'),
    ('falsa', 'Falsas alarmas', 'Lo que el revisor marca y el alumno no vive así.'),
]


def e(texto):
    # El verificador lee un «…» pegado a una palabra como un caso cortado; los del curso van con espacios.
    return html.escape((texto or '').replace('…', ' … '))


def campos(texto):
    out = {}
    for renglon in (texto or '').split('\n'):
        rotulo, _, valor = renglon.partition(': ')
        if valor:
            out[rotulo] = valor
    return out


CORTOS = {'Consigna del ejercicio': 'Consigna', 'Ejercicio, como lo ve el alumno antes de contestar': 'Ejercicio',
          'Opciones que ve el alumno': 'Opciones', 'Respuestas que acepta el corrector': 'Respuestas',
          'Traducción, que el alumno ve al corregir': 'Traducción', 'Miniteoría, que el alumno ve al corregir': 'Miniteoría'}


def lado(texto, otro=None):
    """Un lado del ejercicio, un renglón por campo; con [otro], lo que cambió va resaltado."""
    mios, suyos = campos(texto), campos(otro) if otro is not None else None
    filas = []
    for rotulo, corto in CORTOS.items():
        if rotulo not in mios:
            continue
        valor = f'<b>{corto}:</b> {e(mios[rotulo])}'
        filas.append(f'<span class="cambio">{valor}</span>' if suyos is not None and suyos.get(rotulo) != mios[rotulo]
                     else valor)
    return '<br>'.join(filas)


def pill(nombre):
    return f'<span class="pill {PILL.get(nombre, "p-idle")}">{e(nombre)}</span>'


def revisor(f):
    problemas = ''.join(f'<small>· {e(LUGAR.get(p["where"], p["where"]))} «{e(p["fragment"])}»: {e(p["what"])}</small>'
                        for p in f['problemas'])
    return f'{pill(f["marca"])}<small>{e(f["razon"])}</small>{problemas}'


def fila(f, mia, texto):
    return (f'<tr><td>{e(f["id"])}<small>{e(f["tema"])}</small></td>'
            f'<td>{lado(f["antes"])}</td><td>{lado(f["despues"], f["antes"])}</td>'
            f'<td>{revisor(f)}</td><td>{pill(mia)}<small>{e(texto)}</small></td></tr>')


TABLA = ('<div class="tw casos"><table><thead><tr><th>Caso</th><th>Producción</th><th>Ahora</th><th>El revisor</th>'
         '<th>Yo</th></tr></thead><tbody>\n{}\n</tbody></table></div>')


if __name__ == '__main__':
    d = json.loads((AQUI / 'primeras100.json').read_text())
    por_id = {f['id']: f for f in d['filas']}
    marcados = [f for f in d['filas'] if f['marca'] in ('mal', 'dudoso')]
    sin_lectura = [f['id'] for f in marcados if f['id'] not in MIA]
    if sin_lectura:
        raise SystemExit(f'Faltan leer: {sin_lectura}')
    cuenta = {g: sum(MIA[f['id']][0] == g for f in marcados) for g, _, _ in GRUPOS}
    bloques = []
    for g, titulo, bajada in GRUPOS:
        # Dentro de cada grupo, lo que yo leo como mal primero.
        del_grupo = sorted((f for f in marcados if MIA[f['id']][0] == g),
                           key=lambda f: ('mal', 'dudoso', 'bien').index(MIA[f['id']][1]))
        filas = [fila(f, MIA[f['id']][1], MIA[f['id']][2]) for f in del_grupo]
        abierto = ' open' if g == 'corrige' else ''
        bloques.append(f'<details class="grupo"{abierto}><summary><b>{e(titulo)} · {len(filas)}</b> '
                       f'<span>{e(bajada)}</span></summary>\n{TABLA.format(chr(10).join(filas))}\n</details>')
    bien = [fila(por_id[k], m, t) for k, (m, t) in BIEN_DUDOSOS.items()]
    bloques.append(f'<details class="grupo"><summary><b>Dos «bien» que miraría · {len(bien)}</b> '
                   f'<span>De los {d["cuenta"]["bien"]} que dio por buenos, comparto el resto.</span></summary>\n'
                   f'{TABLA.format(chr(10).join(bien))}\n</details>')
    reales = cuenta['corrige'] + cuenta['hereda']
    valores = {
        'MAL': str(d['cuenta']['mal']), 'DUDOSO': str(d['cuenta']['dudoso']), 'BIEN': str(d['cuenta']['bien']),
        'MARCADOS': str(len(marcados)), 'REALES': str(reales), 'FALSAS': str(cuenta['falsa']),
        'CORRIGE': str(cuenta['corrige']), 'HEREDA': str(cuenta['hereda']),
        'CORRIGE_PCT': f'{100 * cuenta["corrige"] / len(marcados):.1f}',
        'HEREDA_PCT': f'{100 * cuenta["hereda"] / len(marcados):.1f}',
        'FALSAS_PCT': f'{100 * cuenta["falsa"] / len(marcados):.1f}',
        'BIEN_OK': str(d['cuenta']['bien'] - len(BIEN_DUDOSOS)),
        'FALTAN': f'{d["faltan"]:,}'.replace(',', '.'),
        'CASOS': '\n'.join(bloques),
    }
    plantilla = (AQUI / 'brief229-plantilla.html').read_text()
    cabeza = CABEZA.read_text().split('</head>')[0].replace('<title>Marcas en Correcciones</title>',
                                                              '<title>Las cien marcas</title>')
    cuerpo = plantilla
    for clave, valor in valores.items():
        cuerpo = cuerpo.replace('{{' + clave + '}}', valor)
    SALIDA.write_text(cabeza + cuerpo)
    print(SALIDA, cuenta, 'sin llenar:', re.findall(r'\{\{[A-Z_]+\}\}', cuerpo))
