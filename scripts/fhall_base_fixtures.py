#!/usr/bin/env python3
"""Fixtures de FEAT-HALL sacadas de la base del 29/9 (analisis 2026-09-30T11-54-02 y su plan).

Las pruebas de F-HALL-R013 comparan lo que da el contrato con lo que daba el codigo de antes sobre
el curso vivo del 29/9: el mismo puntaje por analizador en los 11.760 nodos, los mismos diagnosticos
tipados y las mismas 4.036 tareas. El analisis pesa 146 MB y no se versiona; de el salen, comprimidos:

  audit-application/src/test/resources/fhall-base-2026-09-30/
    tokens.jsonl.gz      los tokens de spaCy, uno por linea: [texto, lema, pos, rango COCA, stop, puntuacion]
    sentences.jsonl.gz   cada oracion canonica con sus tokens: [oracion, [indices en tokens]]
    nodes.jsonl.gz       cada nodo en preorden: [target, id, {puntajes}]
    diagnoses.jsonl.gz   el diagnostico tipado de cada nodo, en el mismo orden
  refiner-domain/src/test/resources/fhall-base-2026-09-30/
    nodes.tsv.gz         cada nodo en preorden: target, id, etiqueta y clave=puntaje;...
    plan.tsv.gz          las tareas del plan 2026-09-30T11-54-12, en orden: target, id, etiqueta, tipo

Uso (desde la raiz del repo): python3 scripts/fhall_base_fixtures.py <analisis.json> <plan.json>
"""
import gzip
import json
import os
import sys


def write_gz(path, lines):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = ('\n'.join(lines) + '\n').encode('utf-8')
    with open(path, 'wb') as f:
        # mtime=0: el mismo analisis da los mismos bytes
        with gzip.GzipFile(fileobj=f, mode='wb', compresslevel=9, mtime=0) as gz:
            gz.write(data)


def no_tabs(value):
    text = '' if value is None else str(value)
    if '\t' in text or '\n' in text:
        raise SystemExit('un campo trae tabulador o salto de linea: %r' % text)
    return text


def main(analysis_path, plan_path):
    root = json.load(open(analysis_path, encoding='utf-8'))['root']
    tokens, token_index, sentences = [], {}, {}
    nodes, diagnoses, tsv_nodes = [], [], []

    def entity_id(node):
        return node['entity']['id'] if node.get('entity') else 'root'

    def entity_label(node):
        return node['entity']['label'] if node.get('entity') else 'Course'

    def walk(node):
        nodes.append(json.dumps([node['target'], entity_id(node), node['scores']], ensure_ascii=False))
        diagnoses.append(json.dumps(node['diagnoses'], ensure_ascii=False, sort_keys=True))
        tsv_nodes.append('\t'.join([node['target'], no_tabs(entity_id(node)), no_tabs(entity_label(node)),
                                    ';'.join('%s=%r' % (k, v) for k, v in node['scores'].items())]))
        if node['target'] == 'QUIZ':
            entity = node['entity']
            canonical = (entity.get('sentences') or [''])[0]
            indexes = []
            for t in entity.get('tokens') or []:
                key = (t['text'], t['lemma'], t['posTag'], t['frequencyRank'], t['isStop'], t['isPunct'])
                if key not in token_index:
                    token_index[key] = len(tokens)
                    tokens.append(json.dumps(list(key), ensure_ascii=False))
                indexes.append(token_index[key])
            previous = sentences.get(canonical)
            if previous is not None and previous != indexes:
                raise SystemExit('la misma oracion con tokens distintos: %r' % canonical)
            sentences[canonical] = indexes
        for child in node.get('children') or []:
            walk(child)

    walk(root)
    plan = json.load(open(plan_path, encoding='utf-8'))
    plan_lines = ['\t'.join([t['nodeTarget'], no_tabs(t['nodeId']), no_tabs(t['nodeLabel']), t['diagnosisKind']])
                  for t in plan['tasks']]

    app = 'audit-application/src/test/resources/fhall-base-2026-09-30/'
    write_gz(app + 'tokens.jsonl.gz', tokens)
    write_gz(app + 'sentences.jsonl.gz',
             [json.dumps([s, idx], ensure_ascii=False) for s, idx in sentences.items()])
    write_gz(app + 'nodes.jsonl.gz', nodes)
    write_gz(app + 'diagnoses.jsonl.gz', diagnoses)
    refiner = 'refiner-domain/src/test/resources/fhall-base-2026-09-30/'
    write_gz(refiner + 'nodes.tsv.gz', tsv_nodes)
    write_gz(refiner + 'plan.tsv.gz', plan_lines)
    print('nodos %d, tokens %d, oraciones %d, tareas %d' % (len(nodes), len(tokens), len(sentences), len(plan_lines)))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
