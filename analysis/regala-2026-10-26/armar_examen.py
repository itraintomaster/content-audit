# Examen del juez «No regala la respuesta» (FEAT-REGALA), 26/10/2026.
#
# Arma una foto del curso con cada caso como un ejercicio propio, en un tema propio, para poder tener la versión
# corregida y la de antes del mismo ejercicio. Los casos salen de:
#   - la C2 de A1 (prueba ancha): la versión corregida (elementAfter) o la de antes (elementBefore) de su revisión;
#   - producción (db/english-course): consignas que recuerdan la regla entera;
#   - inventados: otros patrones de regalo, para no examinar sólo «colocando el verbo delante del sujeto».
# Los resultados esperados son editoriales (míos), no una verdad certificada.
#
# Uso: python3 armar_examen.py <carpeta-nueva>
import hashlib, json, shutil, sys
from copy import deepcopy
from pathlib import Path

REPO = Path('/Users/josecullen/projects/learney/content-audit')
DB = REPO / 'db/english-course'
REVISIONS = REPO / '.content-audit/campaigns/a1-v11/.content-audit/revisions/tanda-A1-c2-1f1d898f-3ba9-3fcc-bc91-a104f79e53ca-plan'
JUDGE = 'answer-giveaway'
COURSE_FILES = {'_course.json', '_milestone.json', '_topic.json', '_knowledge.json', 'quizzes.json'}

# (clave, origen, id o datos, esperado, por qué)
C2 = 'c2-corregida'
ANTES = 'c2-antes'
PROD = 'produccion'
INVENTADO = 'inventado'
CASOS = [
    # --- regalan ---
    ('26ef', C2, '67fab6d599301022953426ef', 'FAILS',
     'La consigna dice el caso de la regla: «Acabas de decidir… no tenías un plan previo» es decisión del momento → will.'),
    ('2535', C2, '67fab6d59930102295342535', 'FAILS',
     'El paréntesis del enunciado dice la operación: «cambia solo «want» por «would like»».'),
    ('21c9', C2, '67fab6d599301022953421c9', 'FAILS',
     '«colocando el verbo delante del sujeto» da el orden, la mitad de lo que se practica.'),
    ('21bc', C2, '67fab6d599301022953421bc', 'FAILS', 'Igual que 21c9.'),
    ('1e8d', C2, '67fab6d59930102295341e8d', 'FAILS',
     '«colocando el verbo antes del sujeto indicado en la pista» descarta las opciones de orden; queda sólo la concordancia. '
     'En la revisión de la prueba ancha lo marqué «bien»: se me pasó.'),
    ('1e92', C2, '67fab6d59930102295341e92', 'FAILS', 'Igual que 1e8d.'),
    ('inv-cortas', INVENTADO, {
        'nivel': 'a1', 'tema': 'Respuestas cortas', 'unidad': 'Present Simple of «Be»', 'modo': 'FILL',
        'consignaTema': 'Escribe la respuesta corta.', 'consigna': "Escribe la respuesta corta: usa aren't.",
        'partes': [('TEXT', 'Are we late? No, we'), ('CLOZE', ["aren't", 'are not']), ('TEXT', '.')]},
     'FAILS', 'La consigna dice la respuesta.'),
    ('inv-estaticos', INVENTADO, {
        'nivel': 'a2', 'tema': 'Verbos estáticos', 'unidad': 'Present Simple and Present Continuous', 'modo': 'FILL',
        'consignaTema': 'Usa el presente continuo si el verbo no es estático, o el presente simple si lo es.',
        'consigna': 'Usa el presente continuo: cook no es un verbo estático.',
        'partes': [('TEXT', 'Dad'), ('CLOZE', ["'s cooking", 'is cooking']), ('TEXT', '(cook) dinner right now.')]},
     'FAILS', 'La consigna dice qué caso de la regla aplica, que es justo lo que el tema practica.'),
    ('inv-plan', INVENTADO, {
        'nivel': 'a1', 'tema': 'Elegir la forma natural', 'unidad': "'Will' and 'Going to'", 'modo': 'FILL',
        'consignaTema': "Escribe oraciones con 'will' o 'going to' según el contexto.",
        'consigna': 'Es un plan que ya tenías: usa going to.',
        'partes': [('TEXT', 'I'), ('CLOZE', ["'m going to visit", 'am going to visit']),
                   ('TEXT', '(visit) my aunt on Sunday. I have the tickets.')]},
     'FAILS', 'La consigna dice el caso y la forma: plan → going to.'),
    ('inv-adverbio', INVENTADO, {
        'nivel': 'a1', 'tema': 'Adverbios de frecuencia', 'unidad': 'Adjetives and Adverbs', 'modo': 'REWRITE',
        'consignaTema': 'Coloca el adverbio en el lugar correcto.',
        'consigna': 'Coloca el adverbio en el lugar correcto.',
        'partes': [('TEXT', 'Tom'), ('CLOZE', ['is often']), ('TEXT', 'tired. (be / often) (Poné often después de is.)')]},
     'FAILS', 'La indicación del enunciado dice dónde va el adverbio, que es lo que se practica.'),
    ('inv-comparativo', INVENTADO, {
        'nivel': 'a2', 'tema': 'Comparativos', 'unidad': 'Adjetives and Adverbs', 'modo': 'FILL',
        'consignaTema': 'Escribe el comparativo del adjetivo.',
        'consigna': 'Escribe el comparativo; en esta palabra se duplica la consonante final y se agrega -er.',
        'partes': [('TEXT', 'Today is'), ('CLOZE', ['hotter']), ('TEXT', '(hot) than yesterday.')]},
     'FAILS', 'La consigna hace la formación del comparativo, que es lo que se practica.'),
    # --- no regalan ---
    ('2572', C2, '67fab6d59930102295342572', 'PASSES', 'Recuerda la regla entera; el alumno decide cuál caso aplica.'),
    ('2129', C2, '67fab6d59930102295342129', 'PASSES', 'Nombra la tarea y dice de dónde salen sujeto y verbo.'),
    ('2127', C2, '67fab6d59930102295342127', 'PASSES', '«Yes, he will.» es contexto dentro de la oración.'),
    ('23e7', C2, '67fab6d599301022953423e7', 'PASSES', 'La pista (I / me) es de otro juez; la consigna nombra la tarea.'),
    ('2419', C2, '67fab6d59930102295342419', 'PASSES', 'Incluir there es la tarea misma, no un paso de ella.'),
    ('2533', C2, '67fab6d59930102295342533', 'PASSES', 'Usar would like es la tarea que nombra el tema.'),
    ('262e', C2, '67fab6d5993010229534262e', 'PASSES', '«en inglés estándar» pide un formato, no la respuesta.'),
    ('2021', C2, '67fab6d59930102295342021', 'PASSES', 'Nombra la tarea y el tiempo del tema.'),
    ('2190', C2, '67fab6d59930102295342190', 'PASSES', 'Pregunta cuál es más natural sin decirlo; el contexto está en la oración.'),
    ('243f', C2, '67fab6d5993010229534243f', 'PASSES', 'Lista las opciones de la regla sin decir cuál.'),
    ('2591', C2, '67fab6d59930102295342591', 'PASSES', 'Recuerda la regla entera; el alumno decide si se puede.'),
    ('2258', C2, '67fab6d59930102295342258', 'PASSES', 'Nombra el tiempo y avisa qué tipos de verbos hay, sin decir cuál es cuál.'),
    ('2df0', C2, '67fab6d59930102295342df0', 'PASSES', 'Nombra la tarea.'),
    ('26ef-antes', ANTES, '67fab6d599301022953426ef', 'PASSES', 'Recuerda los dos casos de la regla; la oración decide.'),
    ('21c9-antes', ANTES, '67fab6d599301022953421c9', 'PASSES', 'Nombra la tarea; si una opción se delata, es del juez de opción múltiple.'),
    ('1e8d-antes', ANTES, '67fab6d59930102295341e8d', 'PASSES', 'Nombra la tarea; las opciones son de otro juez.'),
    ('2535-antes', ANTES, '67fab6d59930102295342535', 'PASSES', 'Nombra la tarea.'),
    ('264f', PROD, '67fab6d5993010229534264f', 'PASSES',
     'Recuerda la regla entera y fija el momento; el alumno decide si eat es estático.'),
    ('46fd', PROD, '67fab6d599301022953446fd', 'PASSES', 'Recuerda la regla entera con sus dos casos.'),
    ('4028', PROD, '67fab6d59930102295344028', 'PASSES', 'Recuerda la regla; el alumno decide qué acción fue antes.'),
    ('20b7', PROD, '67fab6d599301022953420b7', 'PASSES', 'Recuerda la regla entera; el alumno decide si like se usa en continuo.'),
]


def read(p):
    return json.loads(Path(p).read_text())


def write(p, value):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, ensure_ascii=False, indent=1))


def identity(key):
    return hashlib.sha256((JUDGE + '/' + key).encode()).hexdigest()[:24]


def oid(i):
    return {'$oid': i}


def production():
    """Every quiz of the course with its level, topic and knowledge."""
    found = {}
    for f in DB.glob('*/*/*/quizzes.json'):
        knowledge = read(f.parent / '_knowledge.json')
        topic = read(f.parent.parent / '_topic.json')
        for q in read(f):
            found[q['id']] = (f.parts[-4], topic, knowledge, q)
    return found


def revision(quiz_id):
    files = sorted(REVISIONS.glob('*-' + quiz_id + '.json'))
    if len(files) != 1:
        raise SystemExit('revisión ambigua o ausente de ' + quiz_id)
    return read(files[0])['proposal']


def from_model(model):
    """A quiz of a revision, in the shape of the course files."""
    form = model['form']
    out = {'kind': form['kind'], 'sentenceParts': [{'kind': p['kind'], 'text': p.get('text') or '',
                                                    'options': p.get('options')} for p in form['sentenceParts']],
           'sentences': model.get('sentences') or []}
    choice = form.get('multipleChoice')
    if choice:
        out['selection'] = choice.get('selection') or 'SINGLE'
        out['items'] = [{'id': i['id'], 'incidence': float(i['incidence']), 'label': i['label']} for i in choice['items']]
    return {'kind': model['kind'], 'title': model['title'], 'topicName': model.get('topicName'),
            'instructions': model.get('instructions'), 'translation': model.get('translation'),
            'miniTheory': model.get('miniTheory'), 'form': out}


def build(dest):
    if dest.exists():
        raise SystemExit('La carpeta ya existe; no se reescribe un examen congelado.')
    course = dest / 'course'
    for p in sorted(DB.rglob('*.json')):
        if p.name in COURSE_FILES and '.content-audit' not in p.parts:
            target = course / p.relative_to(DB)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(p, target)
    root = read(course / '_course.json')
    milestones = {lvl: read(course / lvl / '_milestone.json') for lvl in ('a1', 'a2', 'b1', 'b2')}
    prod = production()
    cases, gold = [], []
    for key, source, ref, expected, why in CASOS:
        case_id = identity(key)
        if source == INVENTADO:
            level, unit, topic_instructions, mode = ref['nivel'], ref['unidad'], ref['consignaTema'], ref['modo']
            parts = [{'kind': kind, 'text': '' if kind == 'CLOZE' else text, 'options': text if kind == 'CLOZE' else None}
                     for kind, text in ref['partes']]
            sentence = ' '.join(p['options'][0] if p['kind'] == 'CLOZE' else p['text'] for p in parts)
            quiz = {'kind': 'CLOZE', 'title': ref['tema'], 'topicName': unit, 'instructions': ref['consigna'],
                    'translation': '', 'miniTheory': '',
                    'form': {'kind': 'CLOZE', 'sentenceParts': parts, 'sentences': [sentence]}}
            title = ref['tema']
        else:
            level, topic, knowledge, original = prod[ref]
            unit, topic_instructions, mode = topic['label'], knowledge.get('instructions'), knowledge.get('sentenceMode')
            if source == PROD:
                quiz = deepcopy(original)
            else:
                side = 'elementAfter' if source == C2 else 'elementBefore'
                quiz = from_model(revision(ref)[side]['quiz'])
            title = quiz['title']
        topic_id, knowledge_id = identity('topic/' + key), identity('knowledge/' + key)
        milestone = milestones[level]
        topic_node = {'_id': oid(topic_id), 'parentId': milestone['_id'], 'kind': 'TOPIC', 'label': unit,
                      'code': topic_id, 'oldId': topic_id, 'children': None, 'ruleIds': [oid(knowledge_id)]}
        knowledge_node = {'_id': oid(knowledge_id), 'parentId': oid(topic_id), 'kind': 'KNOWLEDGE', 'label': title,
                          'code': knowledge_id, 'oldId': knowledge_id, 'children': None, 'isRule': True,
                          'instructions': topic_instructions, 'sentenceMode': mode}
        quiz.update(id=case_id, _id=oid(case_id), knowledgeId=oid(knowledge_id))
        for stale in ('formCloze', 'instructionsAntesDeRevisar'):
            quiz.pop(stale, None)
        milestone.setdefault('children', []).append(oid(topic_id))
        root.setdefault('knowledgeIds', []).append(oid(knowledge_id))
        folder = course / level / ('regala-' + key)
        write(folder / '_topic.json', topic_node)
        write(folder / 'exercise/_knowledge.json', knowledge_node)
        write(folder / 'exercise/quizzes.json', [quiz])
        cases.append({'ejercicio': case_id})
        gold.append({'ejercicio': case_id, 'clave': key, 'origen': source, 'referencia': ref if source != INVENTADO else None,
                     'grupo': 'regala' if expected == 'FAILS' else 'no-regala', 'esperado': expected, 'porque': why,
                     'consigna': quiz['instructions'],
                     'enunciado': ' '.join('___' if p['kind'] == 'CLOZE' else (p.get('text') or '').strip()
                                           for p in quiz['form']['sentenceParts'])})
    write(course / '_course.json', root)
    for lvl, milestone in milestones.items():
        write(course / lvl / '_milestone.json', milestone)
    write(dest / 'cases.json', cases)
    write(dest / 'gold.json', {'juez': JUDGE, 'criterio': 'Editorial (asistente), 26/10/2026; referencia, no verdad certificada.',
                               'meta': {'regalaComoSeEspera': 10, 'noRegalaFalsasAlarmas': 1}, 'casos': gold})
    print(json.dumps({'casos': len(cases), 'regala': sum(g['esperado'] == 'FAILS' for g in gold),
                      'noRegala': sum(g['esperado'] == 'PASSES' for g in gold)}, ensure_ascii=False))


if __name__ == '__main__':
    build(Path(sys.argv[1]))
