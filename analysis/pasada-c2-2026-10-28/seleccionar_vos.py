# Elige los ejercicios cuyo único problema es el vos que la tabla pasa a tú (F-MESA-R032), a partir de la inspección
# gratis del corrector (recorrect estimate sin ejercicios).
# Uso: seleccionar_vos.py <inspeccion.json> <salida.json>
# Entra un ejercicio sin aprobar, que el corrector puede tomar, con un hallazgo de vos por regla y ningún otro hallazgo
# medio o grave salvo una palabra de otro nivel (el selector no la toma sola) o la oración de producción (la arregla
# publicar). Los de vos que la tabla no deja de tú (resolución PANEL) quedan afuera: necesitan la mesa.
import collections
import json
import sys


def acompana(f):
    marker = (f.get('identity') or {}).get('marker') or ''
    return (f['severity'] == 'LOW' or f.get('resolution') == 'RANK_ONLY' or f['analyzer'] == 'lemma-absence'
            or (f['analyzer'] == 'data-coherence' and 'produccion' in marker))


ins = json.load(open(sys.argv[1]))
elegidos, afuera = [], collections.Counter()
for x in ins['quizzes']:
    vos = [f for f in x['findings'] if f['analyzer'] == 'spanish-register']
    if not vos:
        continue
    if x.get('approved'):
        afuera['aprobado'] += 1
    elif not x.get('correctable'):
        afuera['no corregible: ' + (x.get('reason') or '')[:60]] += 1
    elif any(f.get('resolution') != 'RULE' for f in vos):
        afuera['vos que la tabla no deja de tú'] += 1
    elif not all(f['analyzer'] == 'spanish-register' or acompana(f) for f in x['findings']):
        afuera['tiene otro problema'] += 1
    else:
        elegidos.append(x['quizId'])
json.dump(elegidos, open(sys.argv[2], 'w'))
print(len(elegidos), 'elegidos;', dict(afuera))
