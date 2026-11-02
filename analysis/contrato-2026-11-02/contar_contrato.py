# Cuenta la C2 como la ve la pantalla y verifica la invariante del contrato (F-CONTR-R008).
# Uso: python3 verificar_contrato.py <carpeta> && python3 contar_contrato.py <carpeta>
# Lee lo que bajó verificar_contrato.py del 7800 (sólo GET, no consulta a nadie).
# Sale con 1 si hay alguna contradicción o si el análisis no trae el contrato.
import json, sys, collections
d = sys.argv[1]
v = json.load(open(d+'/vista-c2.json'))['view']
dec = json.load(open(d+'/decisiones-c2.json'))
o = json.load(open(d+'/opiniones-c2.json'))
bien = o['bien']['quizzes']; cambios = o['cambios']; ctx = o['contexto']
guardado = dec['decisiones']; qa = dec['quizApprovals']
items_by_q = collections.defaultdict(list)
for i in v['items']: items_by_q[i['q']].append(i)
quizzes = set()
for t in v['topics']:
    quizzes.update(t.get('quizzes', [])); quizzes.update(t.get('bien', []) or []); quizzes.update((t.get('quiet') or {}).keys())
quizzes |= set(items_by_q)
def todos(e): return (e.get('r') == 'todos') if e and 'r' in e else bool(e and e.get('j')) and all(s == 'hoy' for _, s, *r in e['j'])
cat = collections.Counter(); catq = {}
for q in quizzes:
    e = bien.get(q); its = items_by_q.get(q, [])
    aprob = all((guardado.get(i['id']) or [None])[0] == 'APPROVED' for i in its) if its else (qa.get(q) or {}).get('status') == 'APPROVED'
    if aprob: c = 'preparados' if todos(e) else 'vos-aprobas'
    elif e: c = 'todos' if todos(e) else 'faltan'
    else: c = 'problema' if q in ctx or q in bien or its else 'sin-analisis'
    cat[c] += 1; catq[q] = c
print('con r:', sum('r' in e for e in bien.values()), 'de', len(bien))
print('categorías:', dict(cat))
# Chip por ejercicio cambiado.
por_q = collections.defaultdict(set)
for i in v['items']:
    jv = cambios.get(i['id'])
    if jv: por_q[i['q']].add(jv['v'])
mezcla = {q: s for q, s in por_q.items() if len(s) > 1}
print('ejercicios cambiados con chip:', len(por_q), '· con veredictos mezclados:', len(mezcla), list(mezcla.items())[:3])
chip = collections.Counter(next(iter(s)) if len(s) == 1 else 'mezcla' for s in por_q.values())
print('chip por ejercicio:', dict(chip))
# Invariantes.
rojo = lambda r: r.get('s') in ('peor', 'error') and not r.get('leve')
def renglones(q):
    c = ctx.get(q) or {}
    for lista in (c.get('rows') or {}).values(): yield from lista
    yield from c.get('footer') or []
malos_rojo = [q for q, c in catq.items() if c in ('todos', 'preparados') and any(rojo(r) for r in renglones(q))]
print('Todos/Preparados con renglón en rojo:', len(malos_rojo), malos_rojo[:5])
ok_no_todos = [q for q, s in por_q.items() if s == {'ok'} and not todos(bien.get(q))]
todos_no_ok = [q for q in por_q if todos(bien.get(q)) and por_q[q] != {'ok'}]
print('«Lo dan por bueno» fuera de Todos:', len(ok_no_todos), ok_no_todos[:5])
print('Todos sin «Lo dan por bueno»:', len(todos_no_ok), [(q, por_q[q]) for q in todos_no_ok[:5]])
json.dump({'categorias': dict(cat), 'chip': dict(chip), 'rojo': malos_rojo, 'ok_no_todos': ok_no_todos, 'todos_no_ok': todos_no_ok}, open(d+'/conteo-c2.json', 'w'))

problemas = len(malos_rojo) + len(ok_no_todos) + len(todos_no_ok) + (0 if bien and all('r' in e for e in bien.values()) else 1)
print('CONTRADICCIONES:', problemas)
sys.exit(1 if problemas else 0)
