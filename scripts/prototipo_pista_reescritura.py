#!/usr/bin/env python3
"""Prototipo de FEAT-PISTA, sin encender: «la reescritura pide palabras que no salen de la oración de partida ni de la
pista». Medido el 1/10/2026 sobre el curso del 29/9: 60 marcas entre 3.957 oraciones enteras; 11 de los 27 casos sin
marca de escribir/pista.json; muestra nueva de 30 marcas revisadas a mano, 25 reales (83 %); las 60 revisadas, 56 reales.

Sobre los ejercicios de hueco para escribir cuya respuesta es la oración entera (el tipo WHOLE_SENTENCE de
hint-coherence): cada palabra de contenido de la primera respuesta (los tokens de spaCy de la oración guardada, con su
lema y su etiqueta) tiene que salir del enunciado —la oración de partida y la pista— o de la consigna, comparada por
lema, con la flexión regular y la tabla de formas irregulares. No cuentan las palabras de la gramática que la
reescritura agrega sola (listas cerradas de abajo). Si alguna no sale, el ejercicio es una marca.

Uso:
  python3 scripts/prototipo_pista_reescritura.py \
      --curso db/english-course --spacy <.cache/spacy-nlp/<sha>.json> [--salida marcas.json]
"""
import argparse
import collections
import glob
import json
import os
import re

IRR = collections.defaultdict(set)
PARTICIPLES = set()
BASE_OF = {}
_SPACY = {}


def load_irregular(path):
    d = json.load(open(path, encoding='utf-8'))
    for v in d['verbos']:
        for f in v.get('pasado', []) + v.get('participio', []):
            IRR[v['base']].add(f.lower())
        PARTICIPLES.update(f.lower() for f in v.get('participio', []))
    for o in d['otras']:
        for f in o.get('formas', []):
            IRR[o['base']].add(f.lower())
    for base, forms in IRR.items():
        for f in forms:
            BASE_OF.setdefault(f, base)  # drank → drink, so «drunk» in the answer is announced


def load_spacy(path):
    d = json.load(open(path, encoding='utf-8'))
    for s in d['processed_sentences']:
        _SPACY[s['original_sentence']] = s['processed_words']


def load_course(root):
    """(level, topic folder, knowledge, quizzes) for every knowledge of db/english-course, in the order of the course."""
    out = []
    for kfile in sorted(glob.glob(os.path.join(root, '*', '*', '*', '_knowledge.json'))):
        folder = os.path.dirname(kfile)
        level = os.path.relpath(folder, root).split(os.sep)[0]
        out.append((level, folder, json.load(open(kfile, encoding='utf-8')),
                    json.load(open(os.path.join(folder, 'quizzes.json'), encoding='utf-8'))))
    return out

HINT = re.compile(r'\(([^)]*)\)')
WORD = re.compile(r"[A-Za-z0-9][A-Za-z0-9'-]*")
CONTENT = {'NOUN', 'PROPN', 'VERB', 'ADJ', 'ADV', 'NUM'}
# Words of the grammar a rewrite brings by itself: not content to announce.
FUNCTION_LEMMAS = {'not', "n't", 'so', 'too', 'also', 'very', 'really', 'just', 'even', 'only', 'please', 'then',
                   'there', 'here', 'let', 'how', 'when', 'where', 'why', 'what', 'neither', 'either', 'yes', 'no',
                   'ever', 'as', 'well', 'rather', 'than', 'more', 'most', 'less', 'least',
                   # indefinite pronouns spaCy may tag as nouns: «We had none.»
                   'none', 'nothing', 'nobody', 'everything', 'everybody', 'everyone', 'something', 'somebody',
                   'someone', 'anything', 'anybody', 'anyone', 'one', 'ones'}
AUXILIARIES = {'do', 'have', 'be', 'will', 'would', 'can', 'could', 'shall', 'should', 'may', 'might', 'must'}
# Reported speech shifts its time expressions by a closed list: the trigger in the source announces the shift.
TIME_SHIFTS = {'today': {'that', 'day'}, 'tonight': {'that', 'night'}, 'tomorrow': {'next', 'following', 'day'},
               'yesterday': {'day', 'before', 'previous'}, 'last': {'before', 'previous'}, 'ago': {'before', 'earlier'},
               'now': {'then'}, 'here': {'there'}, 'this': {'that'}, 'these': {'those'},
               'next': {'following', 'after'}}

def stems(w):
    s = {w}
    n = len(w)
    if n >= 4 and w.endswith('e'): s.add(w[:-1])
    if n >= 3 and w.endswith('y'): s.add(w[:-1] + 'i')
    if n >= 3 and w[-1] not in 'aeiouwyx' and w[-2] in 'aeiou': s.add(w + w[-1])
    return {x for x in s if len(x) >= 3}

def regular(base):
    """Regular inflections of a base word, also for short ones: go → goes, going; stop → stopped."""
    b = base
    out = {b + 's', b + 'es', b + 'ed', b + 'd', b + 'ing', b + 'er', b + 'est', b + 'ly'}
    if b.endswith('e'): out |= {b[:-1] + 'ing', b[:-1] + 'er', b[:-1] + 'est'}
    if b.endswith('y'): out |= {b[:-1] + 'ies', b[:-1] + 'ied', b[:-1] + 'ier', b[:-1] + 'iest', b[:-1] + 'ily'}
    if len(b) >= 3 and b[-1] not in 'aeiouwyx' and b[-2] in 'aeiou' and b[-3] not in 'aeiou':
        out |= {b + b[-1] + 'ed', b + b[-1] + 'ing', b + b[-1] + 'er', b + b[-1] + 'est'}
    return out

def matches(src, text, lemma):
    """Whether the answer token (text, lemma) is a form of the source word, as hint-coherence compares."""
    if not src or not text: return False
    if text == src or src == lemma: return True
    if len(src) >= 3 and any(text.startswith(s) for s in stems(src)): return True
    if text in regular(src) or (lemma and lemma in regular(src)): return True
    return text in IRR.get(src, ()) or (lemma and src in IRR.get(lemma, ())) or src in IRR.get(text, ())

def bases(w):
    """The word and the bases its regular suffixes leave: drinking → drink; clearest → clear; simpler → simple;
    steadily → steady; simply → simple."""
    out = {w}
    for suf in ('ing', 'ed', 'es', 's', 'er', 'est', 'ly'):
        if w.endswith(suf) and len(w) - len(suf) >= 3:
            b = w[:-len(suf)]
            out |= {b, b + 'e'}
            if len(b) >= 2 and b[-1] == b[-2]: out.add(b[:-1])          # stopped → stop
            if b.endswith('i'): out.add(b[:-1] + 'y')                   # tried, steadily → steady
    if w.endswith('ly') and len(w) >= 5 and w[-3] not in 'aeiou':
        out.add(w[:-2] + 'le')                                           # simply → simple
    return out

def announced(text, lemma, sources):
    answer_bases = bases(text) | bases(lemma) if lemma else bases(text)
    for s in sources:
        if matches(s, text, lemma) or matches(lemma, s, s) or (s in regular(lemma)) or (lemma in regular(s)):
            return True
        source_bases = bases(s) | ({BASE_OF[s]} if s in BASE_OF else set())
        for b in source_bases:
            if b in answer_bases or text in IRR.get(b, ()) or (lemma and lemma in IRR.get(b, ())):
                return True
    return False

TIME = re.compile(r"\b\d{1,2}:\d{2}\b")

def words(text):
    out = [t for t in TIME.findall(text or '')]  # «8:00» is one token for spaCy
    for w in WORD.findall(text or ''):
        w = w.lower()
        out.append(w)
        if '-' in w:
            out.extend(x for x in w.split('-') if x)  # «push-ups» is push and ups for the tokenizer
        m = re.match(r"^([a-z]+)('s|'re|'ve|'ll|'d|'m|n't)$", w)
        if m:
            out.append(m.group(1))
    return out


def participle(w):
    return w in PARTICIPLES or w.endswith('ed') or w == 'been'

def parts_of(q): return q['form'].get('sentenceParts') or []
def without_hints(t): return re.sub(r'\s+', ' ', HINT.sub(' ', t or '')).strip()

def first_answer(q):
    for p in parts_of(q):
        if p['kind'] == 'CLOZE':
            o = p.get('options') or []
            return (o[0] or '').split('|')[0].strip() if o else ''
    return ''

def is_sentence(a):
    a = (a or '').strip()
    return bool(a) and a[0].isupper() and len(WORD.findall(a)) >= 2 and a[-1] in '.?!'

def shape(q, mode):
    ps = parts_of(q)
    gaps = [i for i, p in enumerate(ps) if p['kind'] == 'CLOZE']
    if not gaps: return 'GAP'
    before = ' '.join(without_hints(p.get('text')) for p in ps[:gaps[0]] if p['kind'] == 'TEXT').strip()
    after = ' '.join(without_hints(p.get('text')) for p in ps[gaps[-1] + 1:] if p['kind'] == 'TEXT').strip()
    if re.search(r'[A-Za-z0-9]', after): return 'GAP'
    last = first_answer(q)
    if (not before or before[-1] in '.?!:') and len(WORD.findall(last)) >= 2: return 'WHOLE'
    if mode == 'REWRITE' and is_sentence(last): return 'WHOLE'
    return 'GAP'

def answer_tokens(q):
    """The spaCy tokens of the saved sentence that belong to the first answer."""
    toks = _SPACY.get((q['form'].get('sentences') or [''])[0])
    if not toks: return None
    ans = [w.lower() for w in re.findall(r"[A-Za-z0-9:']+", first_answer(q))]
    sw = [t['word'].lower() for t in toks]
    for i in range(len(sw)):
        if sw[i:i + len(ans)] == ans:
            return toks[i:i + len(ans)]
    aset = set(ans)
    return [t for t in toks if t['word'].lower() in aset]

def unannounced(q, mode, with_instructions=True):
    if q['form'].get('kind') != 'CLOZE' or shape(q, mode) != 'WHOLE': return None
    toks = answer_tokens(q)
    if toks is None: return None
    sources = set()
    for p in parts_of(q):
        if p['kind'] == 'TEXT': sources |= set(words(p.get('text')))
    if with_instructions: sources |= set(words(q.get('instructions')))
    for trigger, shifted in TIME_SHIFTS.items():
        if trigger in sources: sources |= shifted
    words_of_answer = [t['word'].lower() for t in toks if not t.get('is_punctuation')]
    elliptical_answer = bool(words_of_answer) and words_of_answer[0] in ('so', 'neither', 'nor')
    missing = []
    for i, t in enumerate(toks):
        if t['pos_tag'] not in CONTENT or t.get('is_punctuation'): continue
        text, lemma = t['word'].lower(), (t['lemma'] or '').lower()
        if lemma in FUNCTION_LEMMAS or text in FUNCTION_LEMMAS: continue
        if lemma in AUXILIARIES:
            rest = [x for x in toks[i + 1:] if not x.get('is_punctuation')]
            if elliptical_answer or not rest:
                continue  # «So had I.», «than you do.»: the auxiliary the short clause asks for
            if lemma == 'have':
                nxt = [x for x in rest if x['word'].lower() not in ("n't", 'not', 'never', 'already', 'just', 'ever')]
                if nxt and participle(nxt[0]['word'].lower()):
                    continue  # «would have been», «had overslept»: the perfect is grammar
        if lemma == 'long' and i > 0 and toks[i - 1]['word'].lower() == 'how':
            continue  # «How long…?»: the question asks for the duration
        if not announced(text, lemma, sources):
            missing.append(lemma or text)
    return missing

if __name__ == '__main__':
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--curso', default='db/english-course')
    ap.add_argument('--spacy', required=True)
    ap.add_argument('--irregulares', default='analysis/recursos-compartidos/formas-irregulares.json')
    ap.add_argument('--sin-consigna', action='store_true')
    ap.add_argument('--salida')
    a = ap.parse_args()
    load_irregular(a.irregulares)
    load_spacy(a.spacy)
    marks = []
    per = collections.Counter()
    whole = 0
    for level, folder, k, qs in load_course(a.curso):
        mode = k.get('sentenceMode') or 'FILL'
        for q in qs:
            m = unannounced(q, mode, with_instructions=not a.sin_consigna)
            if m is None:
                continue
            whole += 1
            if m:
                marks.append({'quizId': q['id'], 'analizador': 'pista', 'palabras': m, 'nivel': level.upper(),
                              'tema': k.get('label')})
                per[level] += 1
    print('oraciones enteras', whole, '· marcas', len(marks), dict(sorted(per.items())))
    if a.salida:
        json.dump(marks, open(a.salida, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
