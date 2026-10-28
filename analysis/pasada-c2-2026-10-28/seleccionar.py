# Elige los ejercicios de la pasada a partir de la inspección gratis del corrector (recorrect estimate sin ejercicios).
# Uso: seleccionar.py <inspeccion.json> <salida.json>
# Entra un ejercicio sin aprobar, que el corrector puede tomar, con un hallazgo medio o grave que no sea:
# - «de vos» (spanish-register): se pasa a tú aparte;
# - la oración de producción (data-coherence con marca «produccion»): la arregla publicar (F-LEVE-R004);
# - una palabra de otro nivel sola (lemma-absence): el selector no la toma sola (F-TANDA-R001);
# - una respuesta ofrecida de Una sola o un desacuerdo de traducción: no cuentan como error en el tablero.
# Con la inspección del 28/10 (contenido v57) esto da los mismos 520 de seleccion-520.json.
# Además entran los de «otro tema» del brief 232 en los que Calidad A1 ya marca el caso equivocado, aunque sea leve:
# sin esa tarea el corrector no sabe que tiene que volver al tema.
import json, sys

OTRO_TEMA_CON_TAREA = ['67fab6d59930102295341fe1', '67fab6d599301022953421a4', '67fab6d599301022953421a6',
                       '67fab6d59930102295342c8a', '67fab6d599301022953424b7', '67fab6d59930102295342ce0']


def cuenta(f):
    if f['severity'] == 'LOW':
        return False
    marker = (f.get('identity') or {}).get('marker') or ''
    return not (f['analyzer'] == 'spanish-register'
                or (f['analyzer'] == 'data-coherence' and 'produccion' in marker)
                or f['analyzer'] == 'lemma-absence'
                or (f['analyzer'] == 'single-answer' and f['rule'] == 'offered-alternative')
                or (f['analyzer'] == 'translation-sense' and f['rule'] == 'judges-disagree'))


ins = json.load(open(sys.argv[1]))
elegidos = []
for x in ins['quizzes']:
    if x.get('approved') or not x.get('correctable'):
        continue
    otro_tema = x['quizId'] in OTRO_TEMA_CON_TAREA and any(f['analyzer'] == 'a1-didactic' for f in x['findings'])
    if otro_tema or any(cuenta(f) for f in x['findings']):
        elegidos.append(x['quizId'])
json.dump(elegidos, open(sys.argv[2], 'w'))
print(len(elegidos), 'ejercicios; contenido', ins.get('contentVersion'))
