# La pasada de la C2 entera, ejercicio por ejercicio: cuántos problemas tenía en producción y cuántos tiene ahora, qué
# dijo el revisor (con mi lectura de lectura_c2.py) y qué consignas cambiaron y qué tarea lo permitió. Gratis: lee el
# análisis vigente de la tanda (batch analysis read, el mismo que muestra el tablero), las tandas y sus planes.
# Escribe pasada-c2.json, que lee brief233.py.
# Uso: pasada_c2.py   (después de leer_marcas.py)
import collections
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV  # noqa: E402
from lectura_c2 import MAL, DUDOSO, HEREDADO_MAL_SEPARADOS  # noqa: E402

AQUI = Path(__file__).parent
DATOS = WORK / '.content-audit'
C2 = (AQUI.parent / 'c2-mejoras-2026-10-25/c2-id.txt').read_text().strip()
C1 = 'tanda-A1-20261007T084434Z'
ORDEN = ['BLOCKING', 'HIGH', 'MEDIUM', 'LOW']
# Los jueces cuya tarea puede cambiar la consigna (editScope con INSTRUCTIONS o WHOLE_EXERCISE): answer-completeness
# sólo en su regla «form-request», que saca «Usa short forms».
PERMITEN_CONSIGNA = {'multiple-choice-quality', 'answer-giveaway', 'spanish-register', 'near-duplicates',
                     'exact-duplicates', 'answer-completeness'}
FORMS = r'usa (short|long) forms( si es posible)?\.? ?'


def analisis():
    q = subprocess.run(['bash', str(REPO / 'audit-cli.sh'), '--workdir', str(WORK), 'batch', 'analysis',
                        '--request-json', json.dumps({'action': 'read', 'batchId': C2}), '-f', 'json'],
                       cwd=REPO, env=ENV, capture_output=True, text=True, timeout=1200)
    if q.returncode:
        raise SystemExit(q.stderr[-2000:])
    return json.loads(q.stdout)


def ejercicios(nodo, out):
    if nodo['target'] == 'QUIZ':
        out[nodo['nodeId']] = nodo
    for hijo in nodo.get('children') or []:
        ejercicios(hijo, out)
    return out


def problemas(nodo):
    """Como los cuenta el tablero: los errores sin los anotados (withError) más los de vocabulario (puntaje < 1)."""
    numeros = nodo['numbers']
    con_error = {a['analyzer'] for a in (numeros.get('errors') or {}).get('analyzers', []) if a['withError'] > 0}
    vocabulario = {a['analyzer'] for a in numeros.get('analyzerScores', [])
                   if a['family'] == 'VOCABULARY' and a['score'] < 1}
    return [f for f in nodo['findings'] if f['analyzer'] in con_error | vocabulario]


def peor(hallazgos):
    gravedades = {f['severity'] for f in hallazgos}
    return next((g for g in ORDEN if g in gravedades), 'CERO')


def consigna(texto):
    return texto.split('\n')[0].replace('Consigna del ejercicio: ', '').strip()


def normal(texto):
    return re.sub(r'\s+', ' ', re.sub(r"[$'\"«»]", '', texto).lower()).strip(' .')


def recorrer(o):
    if isinstance(o, dict):
        if isinstance(o.get('id'), str) and 'origin' in o and 'nodeId' in o:
            yield o
        for v in o.values():
            yield from recorrer(v)
    elif isinstance(o, list):
        for v in o:
            yield from recorrer(v)


def decisiones():
    """Las decisiones aceptadas de la C1 y la C2 por (ejercicio, consigna aceptada), con sus tareas. Los ids de tarea
    se repiten entre planes: cada tanda se lee con su propio plan, y una tarea de otro ejercicio no cuenta."""
    out = {}
    for tanda in (C2, C1):
        lote = json.loads((DATOS / f'batches/{tanda}.json').read_text())
        plan = lote['planId'] if lote['planId'].startswith('plan') else 'plan-' + lote['planId']
        tareas = {t['id']: t for t in recorrer(json.loads((DATOS / f'plans/{plan}.json').read_text()))}
        for d in lote.get('panelDecisions') or []:
            if d.get('outcome') != 'ACCEPTED' or not d.get('accepted'):
                continue
            instrucciones = (d['accepted'].get('quiz') or {}).get('instructions')
            for q in d['quizIds']:
                motivos = []
                for tid in d['taskIds']:
                    t = tareas.get(tid)
                    if t and t.get('nodeId') == q:
                        h = (t.get('origin') or {}).get('finding') or {}
                        motivos.append(f"{h.get('analyzer') or t.get('diagnosisKind')}/{h.get('rule') or '-'}")
                out.setdefault((q, instrucciones), (tanda, sorted(set(motivos)), d['accepted'].get('rationale')))
    return out


if __name__ == '__main__':
    vista = analisis()
    base, ahora = (ejercicios(s['digest']['root'], {}) for s in vista['states'])
    marcas = json.loads((AQUI / 'marcas-c2.json').read_text())
    filas = {f['quizId']: f for f in marcas['filas']}
    leidas = {**MAL, **DUDOSO, **HEREDADO_MAL_SEPARADOS}

    def revisor(q):
        f = filas.get(q)
        if f is None:
            return None
        if f['id'] in leidas:
            return leidas[f['id']][0]
        return 'heredado' if f['marca'] == 'heredado' else 'bien'

    por_ejercicio = []
    for q in base:
        antes, despues = problemas(base[q]), problemas(ahora[q])
        por_ejercicio.append({'quizId': q, 'id': q[-4:], 'cambiado': q in filas,
                              'antes': peor(antes), 'nAntes': len(antes), 'ahora': peor(despues), 'nAhora': len(despues),
                              'revisor': revisor(q)})
    porque = decisiones()
    consignas = []
    for q, f in filas.items():
        a, b = consigna(f['produccion']), consigna(f['correccion'])
        if a == b:
            continue
        tanda, motivos, razon = porque.get((q, b), (None, [], None))
        tipo = ('formato' if normal(a) == normal(b)
                else 'forms' if re.sub(FORMS, '', normal(a)).strip(' .') == normal(b) else 'reescrita')
        consignas.append({'quizId': q, 'id': f['id'], 'tema': f['tema'], 'tipo': tipo, 'antes': a, 'ahora': b,
                          'tanda': 'C2' if tanda == C2 else 'C1' if tanda == C1 else None, 'tareas': motivos,
                          'permiso': sorted({m.split('/')[0] for m in motivos} & PERMITEN_CONSIGNA),
                          'porque': razon, 'revisor': revisor(q), 'marca': f['marca']})
    (AQUI / 'pasada-c2.json').write_text(json.dumps({'contentVersion': vista['contentVersion'],
                                                     'ejercicios': por_ejercicio, 'consignas': consignas},
                                                    ensure_ascii=False, indent=1))
    print('ejercicios', len(por_ejercicio), 'cambiados', sum(e['cambiado'] for e in por_ejercicio),
          'consignas', collections.Counter(c['tipo'] for c in consignas),
          'reescritas sin permiso', sum(1 for c in consignas if c['tipo'] == 'reescrita' and not c['permiso']))
