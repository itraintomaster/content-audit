"""Atomic conversion/restoration using a fake destination; no network or real data."""
import copy
import unittest

from production_publish import conversion, manifest_reader, write_plan
from production_publish.tests import fakes
from production_publish.tests.test_cli import CliTestCase


def fixture(restore=False):
    cloze = {'kind': 'CLOZE', 'sentences': ['Is that pen yours?'], 'incidence': 1.0,
             'sentenceParts': [{'kind': 'TEXT', 'text': 'Is that pen', 'options': None},
                               {'kind': 'CLOZE', 'text': '', 'options': ['yours|your own']},
                               {'kind': 'TEXT', 'text': '(your / yours)?', 'options': None}]}
    mc = copy.deepcopy(cloze)
    mc.update(kind='MULTIPLE_CHOICE', selection='SINGLE', items=[
        {'id': 'your', 'label': 'your', 'incidence': 0.0},
        {'id': 'yours', 'label': 'yours', 'incidence': 1.0}])
    mc['sentenceParts'][1]['options'] = None
    mc['sentenceParts'][2]['text'] = '?'
    backup = {'id': fakes.QUIZ_1, 'kind': 'CLOZE', 'form': cloze, 'instructions': 'Completá.',
              'answerAudioUrl': 'audio-original', 'translation': '¿Es tuyo ese bolígrafo?', 'opaque': {'x': 1}}
    before = {'kind': 'CLOZE', 'form': cloze, 'instructions': 'Completá.', 'clozeOriginal': None}
    after = {'kind': 'MULTIPLE_CHOICE', 'form': mc, 'instructions': 'Elegí.', 'clozeOriginal': backup}
    if restore:
        before, after = after, dict(before, clozeOriginal=backup)
    doc = {'collection': 'quizTemplates', 'id': fakes.QUIZ_1, 'nodeId': fakes.QUIZ_1,
           'changeIds': ['conversion-q1'], 'proposalIds': ['proposal-q1'], 'reviewStatus': 'APPROVED',
           'fields': [{'path': p, 'before': before[p], 'after': after[p]}
                      for p in ('kind', 'form', 'instructions', 'clozeOriginal')]}
    manifest = fakes.example_manifest()
    manifest.update(documents=[doc], unsupported=[], excluded=[], counts={
        'documents': 1, 'fields': 4, 'byPath': {p: 1 for p in conversion.PATHS}})
    current = dict(copy.deepcopy(before), _id=fakes.QUIZ_1, answerAudioUrl='audio-original', opaque={'x': 1})
    if not restore:
        del current['clozeOriginal']
    return manifest, {'quizTemplates': {fakes.QUIZ_1: current}, 'knowledges': {}, 'quizzes': {}}


class ValidationTest(unittest.TestCase):
    def test_both_directions_are_complete_and_explicitly_approved(self):
        for restore in (False, True):
            manifest, _ = fixture(restore)
            manifest_reader.validate_manifest(manifest)
            self.assertIsNone(conversion.problem(manifest['documents'][0]))

    def test_incomplete_unapproved_rewritten_or_ambiguous_conversion_is_refused(self):
        changes = [lambda d: d.update(reviewStatus='PENDING'),
                   lambda d: d['fields'].pop(),
                   lambda d: d['fields'][1]['after']['items'][0].update(incidence=1.0),
                   lambda d: d['fields'][1]['after']['sentenceParts'][0].update(text='Another sentence'),
                   lambda d: d['fields'][1]['after'].update(sentences=['A new recording'])]
        for mutate in changes:
            manifest, _ = fixture()
            mutate(manifest['documents'][0])
            with self.subTest(mutate=mutate), self.assertRaises(manifest_reader.Refusal):
                manifest_reader.validate_manifest(manifest)

    def test_restores_an_original_null_instruction_without_relaxing_other_null_writes(self):
        manifest, _ = fixture(True)
        doc = manifest['documents'][0]
        doc['fields'][2]['after'] = None
        for side in ('before', 'after'):
            doc['fields'][3][side]['instructions'] = None
        manifest_reader.validate_manifest(manifest)
        doc['fields'][1]['after'] = None
        with self.assertRaises(manifest_reader.Refusal):
            manifest_reader.validate_manifest(manifest)

    def test_malformed_fields_return_a_refusal(self):
        for fields in (None, [None], [{'path': 'form'}]):
            self.assertIsNotNone(conversion.problem({'fields': fields}))


class ConversionCycleTest(CliTestCase):
    def setUp(self):
        super().setUp()
        self.manifest, docs = fixture()
        self.store = fakes.FakeStore(docs)
        fakes.write_batch(self.workdir, manifest=self.manifest)

    def test_conversion_is_one_compare_and_set_preserves_audio_and_is_idempotent(self):
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(0, code, out)
        self.assertEqual(1, len(self.store.writes))
        result = self.store.data['quizTemplates'][fakes.QUIZ_1]
        self.assertEqual('MULTIPLE_CHOICE', result['kind'])
        self.assertEqual('audio-original', result['answerAudioUrl'])
        self.assertEqual({'x': 1}, result['opaque'])
        self.assertEqual('CLOZE', result['clozeOriginal']['kind'])
        code, out, _ = self.run_cli()
        self.assertEqual(0, code, out)
        self.assertEqual(1, len(self.store.writes))

    def test_conflicts_and_partial_conversion_never_overwrite(self):
        for field, value in (('kind', 'REWRITE'), ('kind', 'MULTIPLE_CHOICE'), ('instructions', 'Editada después')):
            manifest, docs = fixture()
            docs['quizTemplates'][fakes.QUIZ_1][field] = value
            store = fakes.FakeStore(docs)
            plan = write_plan.build(manifest, store, False)
            self.assertFalse(plan.is_clean())
            self.assertEqual([], write_plan.build(manifest, store, skip_drift=True).to_write())
            self.assertEqual([], store.writes)

    def test_restoration_keeps_backup_and_original_form(self):
        manifest, docs = fixture(True)
        self.store = fakes.FakeStore(docs)
        fakes.write_batch(self.workdir, manifest=manifest)
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(0, code, out)
        result = self.store.data['quizTemplates'][fakes.QUIZ_1]
        self.assertEqual('CLOZE', result['kind'])
        self.assertEqual(result['form'], result['clozeOriginal']['form'])
        self.assertEqual('audio-original', result['answerAudioUrl'])
