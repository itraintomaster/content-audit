# Cada ejercicio de una corrida de la C2, entero: cómo está en producción, qué propuso el corrector, qué se aceptó,
# por qué se rechazó cada intento y qué dijeron los jueces del aceptado. Lee la tanda; no consulta a nadie.
# Uso: revisar.py <etiqueta> <runId>
import json, sys
from comun import OUT, REPO, WORK

etiqueta, run_id = sys.argv[1], sys.argv[2]
c2 = (OUT / 'c2-id.txt').read_text().strip()
b = json.loads((WORK / '.content-audit/batches' / (c2 + '.json')).read_text())
run = next(r for r in b['selectiveRuns'] if r['id'] == run_id)
tareas = set(run.get('taskIds') or [])


def partes(q):
    return ((q or {}).get('form') or {}).get('sentenceParts') or []


def enunciado(ps):
    return ' '.join((p.get('text') or '') if p.get('kind') == 'TEXT' else '___' for p in ps).strip()


def respuestas(ps, q=None):
    rs = [o for p in ps if p.get('kind') == 'CLOZE' for o in (p.get('options') or [])]
    mc = ((q or {}).get('form') or {}).get('multipleChoice')
    if mc:
        rs += [('✓ ' if i.get('incidence', 0) > 0 else '✗ ') + i.get('label', '') for i in mc.get('items') or []]
    return rs


def vista(q):
    if not q:
        return None
    ps = partes(q)
    return {'enunciado': enunciado(ps), 'respuestas': respuestas(ps, q), 'traduccion': q.get('translation'),
            'consigna': q.get('instructions'), 'miniteoria': q.get('miniTheory')}


prod = {}
for q in json.loads((REPO / 'db/quiz-templates.json').read_text()):
    qid = q['_id']['$oid'] if isinstance(q.get('_id'), dict) else q.get('_id')
    prod[qid] = q
temas = {}
for f in (REPO / 'db/english-course').rglob('_knowledge.json'):
    k = json.loads(f.read_text())
    for qf in [f.parent / 'quizzes.json']:
        if qf.exists():
            for q in json.loads(qf.read_text()):
                qid = q['_id']['$oid'] if isinstance(q.get('_id'), dict) else q.get('id') or q.get('_id')
                temas[qid] = {'tema': k.get('label') or k.get('title'), 'consignaDelTema': k.get('instructions'), 'modo': k.get('sentenceMode')}
salida = {'run': {k: run.get(k) for k in ('id', 'status', 'spent', 'reserved', 'detail', 'queryCap')}, 'ejercicios': []}
for qid in run['quizIds']:
    decisiones = [d for d in b['panelDecisions'] if qid in (d.get('quizIds') or []) and tareas & set(d.get('taskIds') or [])]
    cambios = [c for c in b['changes'] if any(s.get('quizId') == qid for s in c['steps']) and tareas & set(c.get('taskIds') or [])]
    final = None
    for c in b['changes']:
        if c.get('droppedReason'):
            continue
        for s in c['steps']:
            if s.get('quizId') == qid and (s.get('after') or {}).get('quiz'):
                final = s['after']['quiz']
    p = prod.get(qid) or {}
    salida['ejercicios'].append({
        'quizId': qid, **temas.get(qid, {}),
        'produccion': {**(vista(p) or {}), 'consigna': p.get('instructions'), 'miniteoria': p.get('miniTheory')},
        'ahora': vista(final),
        'tareas': [{'id': o.get('taskId'), 'desenlace': o.get('kind'), 'detalle': o.get('detail')}
                   for o in b.get('outcomes') or [] if o.get('taskId') in tareas and any(o.get('taskId') in (d.get('taskIds') or []) for d in decisiones)],
        'decisiones': [{'resultado': d.get('outcome'), 'intentos': d.get('attempts'), 'consultas': d.get('queriesSpent'),
                        'resueltas': d.get('resolvedTaskIds'), 'mejoradas': d.get('improvedTaskIds'),
                        'veredicto': d.get('acceptedVerdict'),
                        'rechazos': [{'intento': r.get('attempt'), 'motivos': r.get('reasons')} for r in d.get('rejections') or []],
                        'mejorFallida': vista((d.get('bestFailing') or {}).get('quiz')),
                        'porQue': ((d.get('accepted') or d.get('bestFailing') or {}).get('rationale'))} for d in decisiones],
        'cambiosDeEstaCorrida': [{'id': c['changeId'], 'devuelto': c.get('droppedReason')} for c in cambios]})
(OUT / ('revision-' + etiqueta + '.json')).write_text(json.dumps(salida, ensure_ascii=False, indent=2))
print(json.dumps(salida['run'], ensure_ascii=False))
for e in salida['ejercicios']:
    print('\n====', e['quizId'][-4:], e.get('tema'), '|', e.get('modo'), '|', e['produccion'].get('consigna') or e.get('consignaDelTema'))
    print('  PROD:', e['produccion']['enunciado'], '→', e['produccion']['respuestas'], '|', e['produccion'].get('traduccion'))
    if e['ahora']:
        print('  AHORA:', e['ahora']['enunciado'], '→', e['ahora']['respuestas'], '|', e['ahora'].get('traduccion'))
        if e['ahora'].get('miniteoria') != e['produccion'].get('miniteoria'):
            print('  MINI:', e['ahora'].get('miniteoria'))
        if e['ahora'].get('consigna') != e['produccion'].get('consigna'):
            print('  CONSIGNA:', e['ahora'].get('consigna'))
    for d in e['decisiones']:
        print('  DECISIÓN:', d['resultado'], 'intentos', d['intentos'], 'consultas', d['consultas'], '| resueltas', d['resueltas'], '| mejoradas', d['mejoradas'])
        for r in d['rechazos']:
            for m in r['motivos'] or []:
                print('     - intento', r['intento'], ':', m[:400])
        if d['veredicto']:
            v = d['veredicto']
            print('     veredicto:', {k: v.get(k) for k in ('passes', 'newErrors', 'notCompared', 'reasons')})
    for t in e['tareas']:
        print('  TAREA', t['id'][-12:], t['desenlace'], (t['detalle'] or '')[:200])
