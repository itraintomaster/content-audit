# «Volver a corregir» con tope 0 dentro de la C2 (F-MESA-R032 inv. 4): sólo corren las reglas, sin consultar a nadie.
# El tablero no lanza con tope 0 (pide al menos 1), así que va directo a content-audit, como lo pediría el tablero.
# José, 28/10/2026: «Pasá los 317 de vos a tú con la regla gratis».
# Uso: corregir_por_regla.py <etiqueta>   (usa la estimación guardada por estimar.py <etiqueta> …)
import datetime, json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
import comun
comun.OUT = Path(__file__).parent
etiqueta = sys.argv[1]
est = json.loads((comun.OUT / ('estimacion-' + etiqueta + '.json')).read_text())
ahora = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H-%M-%SZ')
pedido = {'action': 'correct', 'batchId': est['batchId'], 'quizIds': est['quizIds'],
          'includeApproved': est.get('includeApproved', False), 'operationId': f'regla-{etiqueta}-{ahora}',
          'contentVersion': est['contentVersion'], 'fingerprint': est['fingerprint'], 'queryCap': 0,
          'reviewer': 'operador-ui'}
r = comun.batch('recorrect', pedido, 'regla-' + etiqueta, timeout=3600)
print(json.dumps({k: r.get(k) for k in ('status', 'spent', 'queryCap', 'operationId', 'detail')}, ensure_ascii=False))
