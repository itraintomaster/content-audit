"""The one supported type transition: a fully reviewed conversion with a complete cloze backup.

Both directions carry the entire form, kind, instructions and backup. They are one
compare-and-set operation, never independent field edits or a document replacement.
"""
import re

from .store import same_value

PATHS = frozenset(('kind', 'form', 'instructions', 'clozeOriginal'))


def is_conversion(fields):
    return isinstance(fields, list) and any(isinstance(f, dict) and f.get('path') == 'form' for f in fields)


def problem(document):
    fields = document.get('fields', [])
    if not isinstance(fields, list) or any(not isinstance(f, dict) for f in fields):
        return 'Los campos de la conversión son inválidos.'
    if not is_conversion(fields):
        if any(f.get('path') in ('kind', 'clozeOriginal') for f in fields):
            return 'El tipo y la copia sólo viajan con una conversión completa.'
        return None
    if document.get('collection') != 'quizTemplates' or document.get('reviewStatus') != 'APPROVED':
        return 'La conversión necesita aprobación explícita.'
    if {f.get('path') for f in fields} != PATHS or len(fields) != len(PATHS):
        return 'La conversión debe llevar tipo, formulario entero, consigna y cloze original.'
    if any('before' not in f or 'after' not in f for f in fields):
        return 'Falta el antes o el después de la conversión.'
    values = {f['path']: f for f in fields}
    before, after = values['form'].get('before'), values['form'].get('after')
    if not isinstance(before, dict) or not isinstance(after, dict):
        return 'Falta el formulario completo antes y después.'
    kinds = before.get('kind'), after.get('kind')
    if kinds not in (('CLOZE', 'MULTIPLE_CHOICE'), ('MULTIPLE_CHOICE', 'CLOZE')):
        return 'Sólo se admite pasar de cloze a opción múltiple o restaurar el cloze.'
    if values['kind']['before'] != kinds[0] or values['kind']['after'] != kinds[1]:
        return 'El tipo del ejercicio y el del formulario deben coincidir.'
    for side in ('before', 'after'):
        if not (isinstance(values['instructions'].get(side), str) or kinds[0 if side == 'before' else 1] == 'CLOZE' and values['instructions'].get(side) is None):
            return 'La consigna debe ser un texto.'
    backup = values['clozeOriginal']['after']
    if not isinstance(backup, dict) or backup.get('id') != document.get('nodeId') or backup.get('kind') != 'CLOZE':
        return 'Falta una copia completa del mismo cloze.'
    if not isinstance(backup.get('form'), dict) or backup['form'].get('kind') != 'CLOZE' or backup.get('clozeOriginal') is not None:
        return 'La copia debe ser un cloze sin copias anidadas.'
    cloze = before if kinds[0] == 'CLOZE' else after
    mc = after if kinds[1] == 'MULTIPLE_CHOICE' else before
    if not same_value(backup['form'], cloze):
        return 'El cloze guardado no reproduce el formulario original.'
    if not same_value(backup.get('instructions'), values['instructions']['before' if kinds[0] == 'CLOZE' else 'after']):
        return 'La copia no guarda la consigna original.'
    if kinds[0] == 'MULTIPLE_CHOICE' and not same_value(values['clozeOriginal']['before'], backup):
        return 'Restaurar debe conservar la copia original.'
    if kinds[0] == 'CLOZE' and values['clozeOriginal']['before'] is not None:
        return 'Una nueva conversión no puede reemplazar una copia existente.'
    if not same_value(cloze.get('sentences'), mc.get('sentences')):
        return 'La oración guardada debe quedar idéntica.'
    for key in set(cloze) | set(mc):
        if key not in ('kind', 'sentenceParts', 'items', 'selection') and not same_value(cloze.get(key), mc.get(key)):
            return 'La conversión cambia otro atributo del formulario: %s.' % key
    old_parts, new_parts = cloze.get('sentenceParts'), mc.get('sentenceParts')
    if not isinstance(old_parts, list) or not isinstance(new_parts, list) or len(old_parts) != len(new_parts):
        return 'La conversión debe conservar las partes del enunciado.'
    if any(not isinstance(p, dict) for p in old_parts + new_parts):
        return 'Las partes del enunciado son inválidas.'
    gaps = [p for p in old_parts if p.get('kind') == 'CLOZE']
    items = mc.get('items')
    if len(gaps) != 1 or not isinstance(gaps[0].get('options'), list) or not gaps[0]['options'] or not isinstance(gaps[0]['options'][0], str) or mc.get('selection') != 'SINGLE' or not isinstance(items, list) or not 2 <= len(items) <= 8:
        return 'La conversión necesita un hueco y entre dos y ocho opciones, con selección única.'
    if any(not isinstance(i, dict) or not isinstance(i.get('label'), str) or not i['label'].strip()
           or type(i.get('incidence')) not in (int, float) or i['incidence'] not in (0, 1) for i in items):
        return 'Las opciones deben tener texto y una incidencia de 0 o 1.'
    right = [i for i in items if i['incidence'] == 1]
    normalize = lambda s: s.strip().replace('’', "'").casefold()
    labels = [normalize(i['label']) for i in items]
    if len(set(labels)) != len(labels) or len(right) != 1 or normalize(right[0]['label']) != normalize(gaps[0]['options'][0].split('|')[0]):
        return 'Debe haber una sola correcta, la primera respuesta actual, y ninguna opción repetida.'
    # The visible sentence can only lose the explicit choice hint. It cannot be rewritten.
    for old, new in zip(old_parts, new_parts):
        if old.get('kind') != new.get('kind'):
            return 'La conversión no puede cambiar las partes del enunciado.'
        if old.get('kind') == 'CLOZE':
            expected = dict(old, options=None)
        elif old.get('kind') == 'TEXT':
            text = old.get('text') or ''
            if not isinstance(text, str):
                return 'El texto del enunciado es inválido.'
            def remove_hint(match):
                hints = {normalize(x) for x in match.group(1).split('/')}
                return '' if hints == set(labels) else match.group(0)
            expected = dict(old, text=re.sub(r'\s+', ' ', re.sub(r'\(([^()]*)\)', remove_hint, text)).strip())
        else:
            return 'El enunciado contiene una parte desconocida.'
        if not same_value(expected, new):
            return 'La conversión cambia el enunciado además de quitar la pista.'
    return None
