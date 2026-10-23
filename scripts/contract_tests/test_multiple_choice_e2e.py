"""Offline contract: production conversion -> write -> extraction -> JSON -> judge.

Run from content-audit with the pipeline Python dependencies installed:
  ITTM_ROOT=/path/to/ittm python -m unittest discover -s scripts/contract_tests -v

The sibling ittm checkout is the default. Only MongoDB is replaced; no model
or network call participates in this test.
"""
import contextlib
import copy
import importlib.util
import io
import json
import os
from pathlib import Path
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from bson import ObjectId, json_util
from jsonschema import Draft202012Validator

CONTENT_AUDIT = Path(__file__).resolve().parents[2]
ITTM = Path(os.environ.get('ITTM_ROOT', CONTENT_AUDIT.parent / 'ittm')).resolve()
sys.path.insert(0, str(ITTM / 'be/tools/pipeline'))

from extract.full_course.config import ExtractConfig
from extract.full_course.full_course_extractor import FullCourseExtractor
from extract.full_course.models.full_course_data import FullCourseData
from extract.full_course.models.quiz_template_data import QuizTemplateData


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


converter = module('production_mc_converter', ITTM / 'be/scripts/cloze-a-multiple-choice.py')
judge = module('quiz_instruction_inputs', CONTENT_AUDIT / '.sentinel/agents/quiz-instruction-validator/eval/run_evals.py')

COURSE = ObjectId('000000000000000000000000')
MILESTONE = ObjectId('111111111111111111111111')
TOPIC = ObjectId('222222222222222222222222')
KNOWLEDGE = ObjectId('333333333333333333333333')


def cloze(index, answer):
    """Production BSON shape, with multiple accepted sentences and opaque fields."""
    oid = ObjectId('44444444444444444444444%d' % index)
    return {
        '_id': oid, 'id': str(oid), 'knowledgeId': KNOWLEDGE,
        'kind': 'CLOZE', 'title': 'Forms of be', 'code': '',
        'instructions': "Elige 'am' o 'are'.", 'translation': 'Estamos listos.',
        'miniTheory': 'Use am with I and are with we.', 'theoryId': 'be',
        'topicName': 'Present Simple', 'difficulty': 0.0, 'retries': 0.0,
        'noScoreRetries': 0.0, 'successMessage': '', 'audioUrl': '',
        'imageUrl': '', 'answerAudioUrl': 'original-audio-%d' % index, 'answerImageUrl': '',
        'form': {
            'kind': 'CLOZE', 'incidence': 1.0, 'label': '', 'name': '',
            'sentenceParts': [
                {'kind': 'TEXT', 'text': 'I' if answer == 'am' else 'We', 'options': None, 'cue': {'audio': index}},
                {'kind': 'CLOZE', 'text': '', 'options': ["am|'m" if answer == 'am' else "are|'re"], 'optional': False},
                {'kind': 'TEXT', 'text': 'ready.', 'options': None},
            ],
            'sentences': ['I am ready.', "I'm ready."] if answer == 'am' else ['We are ready.', "We're ready."],
            'productionVersion': 2, 'shuffle': False,
        },
        'editorial': {'reviewerId': ObjectId('555555555555555555555555'), 'notes': ['keep']},
    }


class Collection:
    def __init__(self, docs):
        self.docs = copy.deepcopy(docs)
        self.writes = []

    @staticmethod
    def matches(doc, query):
        for key, expected in query.items():
            if isinstance(expected, dict) and '$exists' in expected:
                if (key in doc) != expected['$exists']:
                    return False
            elif isinstance(expected, dict) and '$in' in expected:
                if doc.get(key) not in expected['$in']:
                    return False
            elif doc.get(key) != expected:
                return False
        return True

    def find(self, query, projection=None):
        return [copy.deepcopy(d) for d in self.docs if self.matches(d, query)]

    def find_one(self, query):
        docs = self.find(query)
        return docs[0] if docs else None

    def update_one(self, query, update):
        for doc in self.docs:
            if self.matches(doc, query):
                self.writes.append((copy.deepcopy(query), copy.deepcopy(update)))
                doc.update(copy.deepcopy(update['$set']))
                return SimpleNamespace(matched_count=1)
        return SimpleNamespace(matched_count=0)


class Database:
    def __init__(self, quizzes):
        self.collections = {
            'quizTemplates': Collection(quizzes),
            'courses': Collection([{'_id': COURSE, 'title': 'Production contract', 'knowledgeIds': [KNOWLEDGE]}]),
            'knowledges': Collection([
                {'_id': MILESTONE, 'kind': 'MILESTONE', 'code': 'A1', 'label': 'A1', 'isRule': False, 'children': [TOPIC]},
                {'_id': TOPIC, 'kind': 'TOPIC', 'code': 'be', 'label': 'Be', 'isRule': False, 'parentId': MILESTONE, 'ruleIds': [KNOWLEDGE]},
                {'_id': KNOWLEDGE, 'kind': 'KNOWLEDGE', 'code': 'forms', 'label': 'Forms of be', 'isRule': True, 'parentId': TOPIC},
            ]),
        }

    def __getitem__(self, name):
        return self.collections[name]


def extract(db, directory):
    config = ExtractConfig(mongo_uri='mongodb://unused', mongo_database='learney',
                           cache_enabled=False, cache_auto_cleanup=False, cache_directory=directory)
    extractor = FullCourseExtractor(config)
    clients = [extractor.course_extractor, extractor.hierarchy_extractor, extractor.quiz_template_extractor]
    with contextlib.ExitStack() as stack:
        for client in clients:
            stack.enter_context(patch.object(client, 'connect', side_effect=lambda c=client: setattr(c, '_database', db)))
            stack.enter_context(patch.object(client, 'disconnect'))
        return extractor.extract_full_course(COURSE, use_cache=False)


def export_and_reload(db, directory):
    course = extract(db, directory)
    exported = Path(directory) / 'course.json'
    exported.write_text(json_util.dumps(course.model_dump(by_alias=True)), encoding='utf-8')
    raw_json = json.loads(exported.read_text(encoding='utf-8'))
    schema = json.loads((ITTM / 'be/tools/pipeline/extract/full_course/schemas/full_course.schema.json').read_text())
    # This contract covers production quizzes. Hierarchy DTO defaults have
    # their own schema contract and do not define the persisted quiz format.
    validator = Draft202012Validator({'$ref': '#/$defs/QuizTemplateData', '$defs': schema['$defs']})
    for milestone in raw_json['milestones']:
        for topic in milestone['topics']:
            for knowledge in topic['knowledges']:
                for quiz in knowledge['quizTemplates']:
                    validator.validate(quiz)
    reread = FullCourseData.model_validate(json_util.loads(exported.read_text(encoding='utf-8')))
    quizzes = reread.milestones[0].topics[0].knowledges[0].quiz_templates
    return [q.model_dump(by_alias=True) for q in quizzes]


def assert_production(test, doc, sentences, items):
    form = doc['form']
    test.assertEqual('MULTIPLE_CHOICE', doc['kind'])
    test.assertEqual('MULTIPLE_CHOICE', form['kind'])
    test.assertEqual('SINGLE', form['selection'])
    test.assertEqual(items, form['items'])
    test.assertEqual(sentences, form['sentences'])
    test.assertEqual(1, sum(i['incidence'] > 0 for i in items))
    test.assertNotIn('multipleChoice', form)
    test.assertNotIn('sentences', doc)


def run_cli(db, apply=True, source_path=None):
    argv = ['cloze-a-multiple-choice.py', '--uri', 'mongodb://fake']
    if apply:
        argv.append('--aplicar')
    if source_path is not None:
        argv.extend(['--desde', str(source_path)])
    with patch('pymongo.MongoClient', return_value={'learney': db}), patch.object(sys, 'argv', argv), contextlib.redirect_stdout(io.StringIO()) as output:
        code = converter.main()
    return code, output.getvalue()


class MultipleChoiceProductionContractTest(unittest.TestCase):
    def test_every_stored_production_multiple_choice_retains_its_form_and_judge_options(self):
        checked = 0
        for path in (CONTENT_AUDIT / 'db/english-course').rglob('quizzes.json'):
            for doc in json_util.loads(path.read_text(encoding='utf-8')):
                if doc.get('form', {}).get('kind') != 'MULTIPLE_CHOICE':
                    continue
                with self.subTest(quiz=doc['id']):
                    model = QuizTemplateData.model_validate(doc)
                    reread = json_util.loads(json_util.dumps(model.model_dump(by_alias=True)))
                    self.assertEqual(doc, reread)
                    items = doc['form']['items']
                    correct = [i['label'] for i in items if i['incidence'] > 0]
                    self.assertEqual(1, len(correct))
                    level = path.relative_to(CONTENT_AUDIT / 'db/english-course').parts[0].upper()
                    inputs = judge.student_view_inputs({'quiz': reread, 'cefrLevel': level, 'topic': doc['topicName']})
                    self.assertEqual('\n'.join('- ' + i['label'] + (' (correcta)' if i['incidence'] > 0 else '') for i in items), inputs['options'])
                    self.assertNotIn('multipleChoice', reread['form'])
                checked += 1
        self.assertGreater(checked, 0, 'The production course fixture must be present')

    def test_cli_conversion_writes_extracts_exports_and_reloads_into_judge_inputs(self):
        originals = [cloze(1, 'am'), cloze(2, 'are')]
        db = Database(originals)
        collection = db['quizTemplates']
        with tempfile.TemporaryDirectory() as directory:
            code, output = run_cli(db, apply=False)
            self.assertIn(code, (None, 0), output)
            self.assertEqual([], collection.writes)
            code, output = run_cli(db)
            self.assertIn(code, (None, 0), output)
            self.assertEqual(2, len(collection.writes))
            saved = copy.deepcopy(collection.docs)
            actual = export_and_reload(db, directory)
            self.assertEqual(saved, actual)
            for original, result in zip(originals, actual):
                right = 'am' if original['id'] == originals[0]['id'] else 'are'
                items = [{'id': label, 'label': label, 'incidence': float(label == right)} for label in ['am', 'are']]
                assert_production(self, result, original['form']['sentences'], items)
                self.assertEqual(original['form'], result['formCloze'])
                for field in ['translation', 'answerAudioUrl', 'miniTheory', 'editorial']:
                    self.assertEqual(original[field], result[field])
                inputs = judge.student_view_inputs({'quiz': result, 'cefrLevel': 'A1', 'topic': 'Present Simple'})
                self.assertEqual(('I' if right == 'am' else 'We') + ' ___ ready.', inputs['exercise'])
                self.assertEqual(result['instructions'], inputs['instructions'])
                self.assertEqual('A1', inputs['cefrLevel'])
                self.assertEqual('Present Simple', inputs['topic'])
                self.assertEqual('Hueco 1: ' + right, inputs['acceptedAnswers'])
                self.assertEqual('\n'.join('- ' + label + (' (correcta)' if label == right else '') for label in ['am', 'are']), inputs['options'])
            # The CLI runs again against the saved documents: it writes nothing.
            self.assertIn(run_cli(db)[0], (None, 0))
            self.assertEqual(2, len(collection.writes))
            self.assertEqual(saved, collection.docs)

    def test_cli_reads_production_extended_json_without_changing_bson_types(self):
        originals = [cloze(1, 'am'), cloze(2, 'are')]
        db = Database(originals)
        with tempfile.TemporaryDirectory() as directory:
            source_path = Path(directory) / 'mongo-export.json'
            source_path.write_text(json_util.dumps(originals, json_options=json_util.CANONICAL_JSON_OPTIONS), encoding='utf-8')
            code, output = run_cli(db, source_path=source_path)
            self.assertIn(code, (None, 0), output)
            self.assertEqual(2, len(db['quizTemplates'].writes))
            results = export_and_reload(db, directory)
            for original, result in zip(originals, results):
                self.assertEqual(original['_id'], result['_id'])
                self.assertEqual(original['form'], result['formCloze'])
                self.assertEqual(original['form']['sentences'], result['form']['sentences'])
                self.assertIsInstance(result['form']['incidence'], float)

    def test_cli_refuses_concurrent_edit_without_counting_a_write(self):
        db = Database([cloze(1, 'am'), cloze(2, 'are')])
        collection = db['quizTemplates']
        update = collection.update_one
        def edit_before_update(query, change):
            collection.docs[0]['form']['sentences'] = ['Edited after reading.']
            return update(query, change)
        with patch.object(collection, 'update_one', side_effect=edit_before_update):
            code, output = run_cli(db)
        self.assertEqual(1, code)
        self.assertIn('Conflicto', output)
        self.assertIn('escritos: 0', output)
        self.assertEqual([], collection.writes)
        self.assertEqual(['Edited after reading.'], collection.docs[0]['form']['sentences'])

    def test_production_ids_labels_and_bson_numbers_survive_without_conversion(self):
        doc = cloze(1, 'am')
        doc['kind'] = doc['form']['kind'] = 'MULTIPLE_CHOICE'
        doc['form']['selection'] = 'SINGLE'
        doc['form']['sentenceParts'][1]['options'] = None
        doc['form']['items'] = [
            {'id': 'choice-2', 'label': 'are', 'incidence': 0.0, 'feedback': {'retry': True}},
            {'id': 'choice-1', 'label': 'am', 'incidence': 1.0, 'feedback': {'retry': False}},
        ]
        with tempfile.TemporaryDirectory() as directory:
            result, = export_and_reload(Database([doc]), directory)
            self.assertEqual(doc, result)
            assert_production(self, result, doc['form']['sentences'], doc['form']['items'])
            # Test the same persisted payload in canonical Mongo Extended JSON too.
            raw = json.loads(json_util.dumps(result, json_options=json_util.CANONICAL_JSON_OPTIONS))
            inputs = judge.student_view_inputs({'quiz': raw, 'cefrLevel': 'A1', 'topic': 'Present Simple'})
            self.assertEqual('Hueco 1: am', inputs['acceptedAnswers'])
            self.assertEqual('- are\n- am (correcta)', inputs['options'])


if __name__ == '__main__':
    unittest.main()
