# La pasada «Volver a corregir» dentro de la C2 de A1 (José, 28/10/2026: «lanzá la pasada sobre los 570 dentro de la
# C2»). Estima gratis (recorrect estimate) y guarda el pedido para lanzarlo con lanzar.dart <etiqueta> <tope>.
# Uso: estimar.py <etiqueta> <archivo con la lista de ejercicios | ids cortos separados por coma> [aprobados]
# Con «aprobados» incluye los que José ya aprobó (José, 28/10: «corregí igual los 2 aprobados con pista de letras»).
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
import comun
comun.OUT = Path(__file__).parent
C2 = 'tanda-A1-c2-1f1d898f-3ba9-3fcc-bc91-a104f79e53ca'
etiqueta, cuales = sys.argv[1], sys.argv[2]
aprobados = len(sys.argv) > 3 and sys.argv[3] == 'aprobados'
archivo = comun.OUT / cuales
ids = json.loads(archivo.read_text()) if archivo.exists() else ['67fab6d5993010229534' + x for x in cuales.split(',')]
estado = comun.batch('recorrect', {'action': 'estimate', 'batchId': C2, 'quizIds': ids, 'includeApproved': aprobados},
                     'estimacion-' + etiqueta, timeout=1800)
assert estado['allowed'], estado.get('reason')
assert estado['selectedQuizzes'] == len(ids), estado['selectedQuizzes']
estado['quizIds'] = ids
estado['includeApproved'] = aprobados
(comun.OUT / ('estimacion-' + etiqueta + '.json')).write_text(json.dumps(estado, ensure_ascii=False, indent=2))
print(json.dumps({'ejercicios': len(ids), 'version': estado['contentVersion'], 'minimo': estado['minimumQueries'],
                  'corrector': estado.get('correctorRuntime'), 'validan': estado.get('validationAnalyzers')},
                 ensure_ascii=False))
