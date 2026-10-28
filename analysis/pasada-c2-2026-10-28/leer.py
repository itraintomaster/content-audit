# Lee a mano lo que hizo una corrida de la pasada (gratis): por ejercicio, producción → hoy, la decisión de la mesa, sus
# intentos y motivos de rechazo. Uso: leer.py <runId> [ids cortos separados por coma]
import json, sys
B = ('/Users/josecullen/projects/learney/content-audit/.content-audit/campaigns/a1-v11/.content-audit/batches/'
     'tanda-A1-c2-1f1d898f-3ba9-3fcc-bc91-a104f79e53ca.json')
run = sys.argv[1]
solo = sys.argv[2].split(',') if len(sys.argv) > 2 else None
d = json.load(open(B))


def ver(q):
    if not q:
        return '—'
    f = q.get('form') or {}
    partes = ' '.join(('[' + '|'.join(p.get('options') or []) + ']') if p.get('kind') == 'CLOZE' else (p.get('text') or '')
                      for p in f.get('sentenceParts') or [])
    mc = f.get('multipleChoice')
    opciones = (' · opciones: ' + ' / '.join(('*' if i.get('incidence', 0) > 0 else '') + i.get('label', '')
                                             for i in mc.get('items') or [])) if mc else ''
    return (f"{partes}{opciones}\n      consigna: {q.get('instructions')}\n      traducción: {q.get('translation')}"
            f"\n      miniteoría: {q.get('miniTheory')}")


for p in d['panelDecisions']:
    if not any(run in t for t in p.get('taskIds') or []):
        continue
    for quiz in p['quizIds']:
        if solo and quiz[-4:] not in solo:
            continue
        print(f"### {quiz[-4:]} · {p['outcome']} · intentos {p.get('attempts')}")
        aceptado = (p.get('accepted') or {}).get('quiz')
        print('    aceptado:', ver(aceptado))
        print('    rationale:', (p.get('accepted') or {}).get('rationale'))
        print('    resueltas:', len(p.get('resolvedTaskIds') or []), 'de', len(p.get('taskIds') or []))
        for r in p.get('rejections') or []:
            for m in r.get('reasons') or []:
                print('    rechazo', r.get('attempt'), ':', m[:400])
        print()
