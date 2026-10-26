# El tramo grave de la corrida completa de A1 (brief 225), con el curso de hoy: los ejercicios corregibles de la C2,
# sin aprobar, con algún hallazgo HIGH o BLOCKING. Estima gratis (recorrect estimate) y guarda el pedido para lanzarlo
# con lanzar.dart tramo-grave <tope>. No consulta a nadie.
# Uso: tramo_grave.py
import json
from comun import OUT, batch

GRAVES = ('HIGH', 'BLOCKING')
c2 = (OUT / 'c2-id.txt').read_text().strip()
inspeccion = batch('recorrect', {'action': 'estimate', 'batchId': c2, 'quizIds': [], 'includeApproved': False},
                   'c2-inspeccion-tramo-grave', timeout=1200)
ids = [q['quizId'] for q in inspeccion['quizzes']
       if q.get('correctable') and not q.get('approved')
       and any(f.get('severity') in GRAVES for f in q.get('findings') or [])]
antes = set(json.loads((OUT / 'tramos-por-gravedad.json').read_text())['grave']['quizIds'])
estado = batch('recorrect', {'action': 'estimate', 'batchId': c2, 'quizIds': ids, 'includeApproved': False},
               'estimacion-tramo-grave', timeout=1200)
assert estado['allowed'], estado.get('reason')
assert estado['selectedQuizzes'] == len(ids), estado['selectedQuizzes']
estado['quizIds'] = ids
estado['includeApproved'] = False
(OUT / 'estimacion-tramo-grave.json').write_text(json.dumps(estado, ensure_ascii=False, indent=2))
print(json.dumps({'c2': c2, 'version': estado['contentVersion'], 'ejercicios': len(ids),
                  'igualQueEl225': len(antes & set(ids)), 'salieron': len(antes - set(ids)), 'entraron': len(set(ids) - antes),
                  'minimo': estado['minimumQueries'], 'minimoEn225': 1268, 'corrector': estado.get('correctorRuntime'),
                  'nota': estado.get('estimateNote')}, ensure_ascii=False, indent=1))
