# Arma el brief 233: la Corrección 2 de A1 contra producción, ejercicio por ejercicio (cuántos quedaron sin problema,
# cuántos mejoraron, cuántos no se publican) y las consignas que el corrector reescribió. Gratis: lee pasada-c2.json
# (pasada_c2.py) y marcas-c2.json (leer_marcas.py).
# Uso: brief233.py   (después de pasada_c2.py)
import json
import re
import sys
from collections import Counter
from pathlib import Path

from brief229 import e, lado, campos, CORTOS  # noqa: E402

AQUI = Path(__file__).parent
RAIZ = AQUI.parent.parent
CABEZA = RAIZ / 'docs/briefs/231-tramo-grave-y-revisor.html'
SALIDA = RAIZ / 'docs/briefs/233-la-pasada-de-la-c2.html'
GRAVE = {'BLOCKING', 'HIGH'}
NIVEL = {'CERO': 0, 'LOW': 1, 'MEDIUM': 2, 'HIGH': 3, 'BLOCKING': 3}
BIEN = {'bien', 'falsa'}
# Qué permiso alcanza para reescribir la consigna, en el orden en que se cuenta cada consigna una sola vez.
PERMISOS = [({'near-duplicates', 'exact-duplicates'}, 'Ejercicio repetido', 'lo rehace entero'),
            ({'answer-giveaway'}, '«No regala»', 'saca lo que regala'),
            ({'multiple-choice-quality'}, 'Calidad de las opciones', 'opciones y consigna'),
            ({'answer-completeness'}, 'Sacar «Usa short forms»', 'la regla de las formas'),
            ({'spanish-register'}, 'Tú o vos', 'pasar a tú')]
CATEGORIAS = [('listo', 'Sin problema', 'jueces y revisor de acuerdo', 'g'),
              ('cero-algo', 'Sin problema para los jueces', 'el revisor ve algo', 's'),
              ('de-grave', 'Eran graves, ahora no', 'quedan medios o leves', 'g'),
              ('bajo', 'Bajaron algo', 'siguen con problemas', 's'),
              ('igual', 'Igual o peor', 'para los jueces', 's'),
              ('grave', 'Siguen graves', '', 'b'),
              ('real', 'Problema real', 'no se publica así', 'b'),
              ('decidir', 'Decidís vos', 'Contable o incontable', 'i')]


def mil(n):
    return f'{n:,}'.replace(',', '.')


def categoria(x):
    if x['revisor'] == 'real':
        return 'real'
    if x['revisor'] == 'decidir':
        return 'decidir'
    if x['ahora'] == 'CERO':
        return 'listo' if x['revisor'] in BIEN else 'cero-algo'
    if x['ahora'] in GRAVE:
        return 'grave'
    if x['antes'] in GRAVE:
        return 'de-grave'
    if NIVEL[x['ahora']] < NIVEL[x['antes']] or x['nAhora'] < x['nAntes']:
        return 'bajo'
    return 'igual'


def gravedad(nivel):
    return {'CERO': 'cero', 'LOW': 'leve', 'MEDIUM': 'medio'}.get(nivel, 'grave')


def barra(conteo, total):
    partes = []
    for clave, clase in (('cero', 's0'), ('leve', 's1'), ('medio', 's2'), ('grave', 's3')):
        n = conteo[clave]
        ancho = 100 * n / total
        texto = mil(n) if ancho >= 7 else ''
        partes.append(f'<span class="{clase}" style="width:{ancho:.1f}%">{texto}</span>')
    return ''.join(partes)


def fila_hb(titulo, bajada, n, total, clase):
    chico = f'<small>{e(bajada)}</small>' if bajada else ''
    return (f'      <div class="r"><div class="t">{e(titulo)}{chico}</div><div class="tr"><div class="f {clase}" '
            f'style="width:{round(100 * n / total)}%"></div></div><div class="n">{mil(n)}</div></div>')


def renglones(texto, otro=None):
    mios, suyos = campos(texto), campos(otro) if otro is not None else None
    out = []
    for rotulo, corto in CORTOS.items():
        if rotulo in mios:
            valor = f'<b>{corto}:</b> {e(mios[rotulo])}'
            cambio = suyos is not None and suyos.get(rotulo) != mios[rotulo]
            out.append(f'<div class="campo">{f"<span class=cambio>{valor}</span>" if cambio else valor}</div>')
    return ''.join(out)


def caso(f, rotulo_hoy, res_antes, res_hoy):
    return (f'    <div class="ad peor">\n'
            f'      <div class="lado antes"><span class="rot">Producción · {e(f["id"])} · {e(f["tema"])}</span>'
            f'{renglones(f["produccion"])}<span class="res">{e(res_antes)}</span></div>\n'
            f'      <div class="lado despues"><span class="rot">{e(rotulo_hoy)}</span>'
            f'{renglones(f["correccion"], f["produccion"])}<span class="res">{e(res_hoy)}</span></div>\n    </div>')


def permiso(c):
    return next((nombre for analizadores, nombre, _ in PERMISOS if analizadores & set(c['permiso'])), 'ninguno')


if __name__ == '__main__':
    pasada = json.loads((AQUI / 'pasada-c2.json').read_text())
    filas = {f['id']: f for f in json.loads((AQUI / 'marcas-c2.json').read_text())['filas']}
    todos = pasada['ejercicios']
    cambiados = [x for x in todos if x['cambiado']]
    quietos = [x for x in todos if not x['cambiado']]
    antes = Counter(gravedad(x['antes']) for x in todos)
    ahora = Counter(gravedad(x['ahora']) for x in todos)
    cats = Counter(categoria(x) for x in cambiados)
    reescritas = [c for c in pasada['consignas'] if c['tipo'] == 'reescrita']
    forms = [c for c in pasada['consignas'] if c['tipo'] == 'forms']
    if any(not c['permiso'] for c in reescritas):
        raise SystemExit('Hay consignas reescritas sin una tarea que lo permita: revisar pasada_c2.py')
    reales = {x['quizId'] for x in cambiados if x['revisor'] == 'real'}
    # Lo que los jueces ven peor que producción: más grave, o igual de grave con más problemas.
    peores = {x['quizId'] for x in cambiados if NIVEL[x['ahora']] > NIVEL[x['antes']]
              or (NIVEL[x['ahora']] == NIVEL[x['antes']] and x['nAhora'] > x['nAntes'])}
    fuera = reales | {c['quizId'] for c in reescritas} | peores
    total = len(todos)

    filas_cambios = '\n'.join(fila_hb(t, b, cats[k], len(cambiados), clase) for k, t, b, clase in CATEGORIAS)
    por_permiso = Counter(permiso(c) for c in reescritas)
    reales_por_permiso = Counter(permiso(c) for c in reescritas if c['revisor'] in ('real', 'decidir'))
    filas_permiso = '\n'.join(
        fila_hb(nombre, f'{bajada} · {reales_por_permiso[nombre]} con problema real o para decidir', por_permiso[nombre],
                len(reescritas), 'b')
        for _, nombre, bajada in PERMISOS if por_permiso[nombre])

    LECTURA = {'bien': ('p-ok', 'bien'), 'falsa': ('p-ok', 'sin problema'), 'leve': ('p-warn', 'leve'),
               'heredado': ('p-idle', 'ya estaba'), 'decidir': ('p-info', 'decidís vos'), 'real': ('p-bad', 'real')}
    tabla = ['<div class="tw casos"><table><thead><tr><th>Caso</th><th>Consigna en producción</th><th>Consigna hoy</th>'
             '<th>Lo permitió</th><th>Yo</th></tr></thead><tbody>']
    for c in sorted(reescritas, key=lambda c: (c['tema'], c['id'])):
        clase, rotulo = LECTURA[c['revisor']]
        tareas = ', '.join(c['tareas'])
        tabla.append(f'<tr><td>{e(c["id"])}<small>{e(c["tema"])}</small><small>{e(c["tanda"] or "")}</small></td>'
                     f'<td>{e(c["antes"])}</td><td><span class="cambio">{e(c["ahora"])}</span></td>'
                     f'<td>{e(permiso(c))}<small>{e(tareas)}</small></td>'
                     f'<td><span class="pill {clase}">{rotulo}</span></td></tr>')
    tabla.append('</tbody></table></div>')

    deriva = Counter((NIVEL[x['ahora']] > NIVEL[x['antes']]) - (NIVEL[x['ahora']] < NIVEL[x['antes']]) for x in quietos)
    valores = {
        'TOTAL': mil(total), 'CAMBIADOS': mil(len(cambiados)), 'SIN_CAMBIAR': mil(len(quietos)),
        'GRAVES_ANTES': mil(antes['grave']), 'GRAVES_AHORA': mil(ahora['grave']),
        'CERO_ANTES': mil(antes['cero']), 'CERO_AHORA': mil(ahora['cero']),
        'LEVE_ANTES': mil(antes['leve']), 'LEVE_AHORA': mil(ahora['leve']),
        'MEDIO_ANTES': mil(antes['medio']), 'MEDIO_AHORA': mil(ahora['medio']),
        'PROBLEMAS_ANTES': mil(sum(x['nAntes'] for x in todos)), 'PROBLEMAS_AHORA': mil(sum(x['nAhora'] for x in todos)),
        'BARRA_ANTES': barra(antes, total), 'BARRA_AHORA': barra(ahora, total),
        'DERIVA_MEJOR': str(deriva[-1]), 'DERIVA_PEOR': str(deriva[1]),
        'LISTOS': mil(cats['listo']), 'A_CERO': mil(sum(x['ahora'] == 'CERO' for x in cambiados)),
        'OBJETA': str(sum(x['ahora'] == 'CERO' for x in cambiados) - cats['listo']),
        'DE_GRAVE': mil(sum(x['antes'] in GRAVE and x['ahora'] not in GRAVE for x in cambiados)),
        'FILAS_CAMBIOS': filas_cambios, 'FILAS_PERMISO': filas_permiso,
        'REALES': str(len(reales)), 'REESCRITAS': str(len(reescritas)), 'FORMS': str(len(forms)),
        'REESCRITAS_BIEN': str(sum(c['revisor'] in BIEN for c in reescritas)),
        'FUERA': str(len(fuera)), 'PUBLICAR': mil(len(cambiados) - len(fuera)),
        'GRAVES_NO_TOCADOS': str(sum(x['ahora'] in GRAVE for x in quietos)),
        'GRAVES_CAMBIADOS': str(sum(x['ahora'] in GRAVE for x in cambiados)),
        'TABLA_CONSIGNAS': '\n'.join(tabla), 'VERSION': str(pasada['contentVersion']),
        'CASO_CONTABLE': caso(filas['230b'], 'Hoy · rehecho por repetido', 'Practica clasificar el sustantivo.',
                              'Practica «there are», otro tema.'),
        'CASO_BE': caso(filas['1e8d'], 'Hoy · por la tarea de opciones', 'Se acierta copiando la pista.',
                        'La consigna dice el orden: media respuesta.'),
    }
    plantilla = (AQUI / 'brief233-plantilla.html').read_text()
    cabeza = CABEZA.read_text().split('</head>')[0].replace('<title>Tramo grave en marcha</title>',
                                                              '<title>La pasada de la C2</title>')
    cuerpo = plantilla
    for clave, valor in valores.items():
        cuerpo = cuerpo.replace('{{' + clave + '}}', valor)
    # Los puntos suspensivos del curso («Is he…?») son texto, no un recorte.
    cuerpo = cuerpo.replace('…', '⋯')
    SALIDA.write_text(cabeza + cuerpo)
    print(SALIDA, {k: v for k, v in valores.items() if not k.startswith(('BARRA', 'FILAS', 'TABLA', 'CASO'))},
          dict(cats), dict(por_permiso), 'sin llenar:', re.findall(r'\{\{[A-Z_0-9]+\}\}', cuerpo))
