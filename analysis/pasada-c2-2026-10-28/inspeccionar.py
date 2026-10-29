# Inspección gratis de la C2 de A1: «recorrect estimate» sin ejercicios devuelve cada ejercicio con sus hallazgos, si el
# corrector lo puede tomar y si está aprobado. Uso: inspeccionar.py <etiqueta>  → inspeccion-<etiqueta>.json
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
import comun
comun.OUT = Path(__file__).parent
C2 = 'tanda-A1-c2-1f1d898f-3ba9-3fcc-bc91-a104f79e53ca'
estado = comun.batch('recorrect', {'action': 'estimate', 'batchId': C2, 'quizIds': [], 'includeApproved': False},
                     'inspeccion-' + sys.argv[1], timeout=1800)
print(len(estado['quizzes']), 'ejercicios; versión', estado.get('contentVersion'))
