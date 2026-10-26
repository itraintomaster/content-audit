# Busca en todos los ejercicios que cambió la C2 el defecto de los 6 «mal» de las primeras 100 marcas (26/10/2026):
# la corrección sacó de la consigna «Usa short forms / long forms», el corrector sigue aceptando una sola forma y la
# consigna del tema no dice nada de contracciones, así que ahora rechaza la otra forma, que es correcta.
# Es determinista y gratis: lee `batch mark` sin --budget (no consulta) y el curso de producción.
# «Una sola forma» es un hueco que acepta una sola respuesta y esa respuesta tiene la otra forma (contraída o larga).
# Uso: forma_corta.py
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV, OUT  # noqa: E402

AQUI = Path(__file__).parent
CURSO = REPO / 'db/english-course'
PIDE_FORMA = re.compile(r'short forms|long forms|contracci|contra[eé]|forma corta|forma larga', re.I)
# Una forma con contracción, o una larga que la tiene: si es la única que acepta el hueco, la otra queda afuera.
CONTRAIDA = re.compile(r"n't\b|'(s|re|m|ll|ve|d)\b", re.I)
LARGA = re.compile(r"\b(is|are|am|was|were|do|does|did|will|have|has) not\b|\b(I am|you are|we are|they are|he is|she is|it is)\b",
                   re.I)


def consignas_de_los_temas():
    """quizId → consigna del tema, desde el curso de producción."""
    tema = {}
    for k in CURSO.glob('*/*/*/_knowledge.json'):
        consigna = json.loads(k.read_text()).get('instructions') or ''
        for q in json.loads((k.parent / 'quizzes.json').read_text()):
            tema[q['_id'] if isinstance(q['_id'], str) else q['_id'].get('$oid')] = consigna
    return tema


def campos(texto):
    out = {}
    for renglon in (texto or '').split('\n'):
        rotulo, _, valor = renglon.partition(': ')
        out[rotulo.split(',')[0]] = valor
    return out


def respuestas(valor):
    """Las formas que acepta cada hueco: «Hueco 1: 're | are» → [{'re', 'are'}]."""
    return [set(x.strip() for x in parte.split(':', 1)[1].split('|')) for parte in re.split(r'(?=Hueco \d+:)', valor or '')
            if ':' in parte]


if __name__ == '__main__':
    c2 = (OUT / 'c2-id.txt').read_text().strip()
    p = subprocess.run(['bash', str(REPO / 'audit-cli.sh'), '--workdir', str(WORK), 'batch', 'mark', c2, '-f', 'json'],
                       cwd=REPO, env=ENV, capture_output=True, text=True, timeout=900)
    if p.returncode:
        raise SystemExit(p.stderr[-2000:])
    filas = json.loads(p.stdout)['rows']
    tema = consignas_de_los_temas()
    hallados = []
    for row in filas:
        a, b = campos(row['before']), campos(row['after'])
        antes, ahora = a.get('Consigna del ejercicio', ''), b.get('Consigna del ejercicio', '')
        if not PIDE_FORMA.search(antes) or PIDE_FORMA.search(ahora):
            continue
        del_tema = tema.get(row['quizId'], '')
        huecos = respuestas(b.get('Respuestas que acepta el corrector'))
        hallados.append({'id': row['quizId'][-4:], 'quizId': row['quizId'], 'tema': row['topic'],
                         'consignaDelTema': del_tema, 'temaPideLaForma': bool(PIDE_FORMA.search(del_tema)),
                         'unaSolaForma': any(len(h) == 1 and (CONTRAIDA.search(next(iter(h))) or
                                                              LARGA.search(next(iter(h)))) for h in huecos),
                         'marca': (row.get('verdict') or {}).get('mark'), 'estado': row['state'],
                         'consignaAntes': antes, 'consignaAhora': ahora,
                         'ejercicio': b.get('Ejercicio'), 'respuestas': b.get('Respuestas que acepta el corrector')})
    sospechosos = [h for h in hallados if not h['temaPideLaForma'] and h['unaSolaForma']]
    salida = {'cambiados': len(filas), 'sacaronLaForma': len(hallados), 'sospechosos': len(sospechosos),
              'porTema': {}, 'hallados': hallados}
    for h in sospechosos:
        salida['porTema'][h['tema']] = salida['porTema'].get(h['tema'], 0) + 1
    (AQUI / 'forma-corta.json').write_text(json.dumps(salida, ensure_ascii=False, indent=1))
    print(json.dumps({k: v for k, v in salida.items() if k != 'hallados'}, ensure_ascii=False, indent=1))
