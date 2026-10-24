import importlib.util
import json
from concurrent.futures import ThreadPoolExecutor
import os
from pathlib import Path
import subprocess
import tempfile
import time
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
WRAPPER=ROOT/'scripts/codex-runtime/codex'
PACE=ROOT/'scripts/codex-runtime/pace.py'
spec=importlib.util.spec_from_file_location('pace',PACE)
pace=importlib.util.module_from_spec(spec);spec.loader.exec_module(pace)


class CodexPacingTest(unittest.TestCase):
    def test_recovery_cadence_can_change_between_calls_and_restore_backoff(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)
            (root/'pace.lock').write_text('100')
            with patch.object(pace.time,'time',return_value=100),patch.object(pace.time,'sleep') as sleep:
                for gap in [5,30]:
                    (root/'cadence.json').write_text(json.dumps({'version':1,'minimumGapSeconds':gap}))
                    self.assertEqual(pace.run(['/usr/bin/true'],root,15),0)
                    self.assertEqual(sleep.call_args.args,(gap,))
            self.assertEqual(pace.configured_gap(root/'unrelated',15),15)

    def test_invalid_recovery_override_never_launches_a_program(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)
            for value in [{'version':1,'minimumGapSeconds':0},{'version':1,'minimumGapSeconds':True},
                          {'version':2,'minimumGapSeconds':5},[]]:
                (root/'cadence.json').write_text(json.dumps(value))
                with self.subTest(value=value),patch.object(pace.subprocess,'Popen') as launch:
                    with self.assertRaises(ValueError):pace.run(['/usr/bin/true'],root,15)
                    launch.assert_not_called()

    def test_invalid_spacing_never_executes_a_program(self):
        with tempfile.TemporaryDirectory() as temporary:
            for gap in [-1,61,float('nan'),float('inf')]:
                with self.subTest(gap=gap),self.assertRaises(ValueError):
                    pace.run(['/bin/false'],Path(temporary),gap)

    def test_spacing_serializes_calls_without_changing_exit_or_stdin(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);fake=root/'fake.py';events=root/'events'
            fake.write_text('import pathlib,sys,time\np=pathlib.Path(sys.argv[1])\n'
                'with p.open("a") as f:f.write(str(time.time())+"\\n")\n'
                'print(sys.stdin.read(),end="")\nsys.exit(7)\n')
            command=['python3',str(PACE),'--directory',str(root/'gate'),'--minimum-gap','0.15',
                '--','python3',str(fake),str(events)]
            first=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True)
            second=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True)
            # Either child may acquire the lock first; feed both without
            # waiting for one while the other holds the lock awaiting stdin.
            with ThreadPoolExecutor(max_workers=2) as pool:
                one_call=pool.submit(first.communicate,'first',timeout=10)
                two_call=pool.submit(second.communicate,'second',timeout=10)
                one=one_call.result();two=two_call.result()
            self.assertEqual(one[0],'first');self.assertEqual(two[0],'second')
            self.assertEqual((first.returncode,second.returncode),(7,7))
            times=[float(v) for v in events.read_text().splitlines()]
            self.assertGreaterEqual(times[1]-times[0],0.14)

    def test_optional_wrapper_preserves_model_effort_and_authentication(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);fake=root/'fake';args=root/'args';auth=root/'auth'
            fake.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > "$ARGUMENTS"\n'
                'printf "%s\\n" "$HOME" "$CODEX_HOME" > "$AUTH_PATH"\n')
            fake.chmod(0o700)
            env={**os.environ,'CONTENT_AUDIT_CODEX_REAL_BINARY':str(fake),'ARGUMENTS':str(args),
                 'AUTH_PATH':str(auth),'CONTENT_AUDIT_REASONING_EFFORT':'medium',
                 'CONTENT_AUDIT_CODEX_MIN_INTERVAL_SECONDS':'0.01','CONTENT_AUDIT_CODEX_PACE_DIR':str(root/'gate'),
                 'CODEX_HOME':str(root/'same-auth')}
            call=['exec','--ignore-user-config','--ephemeral','-m','gpt-6.1-sol','-']
            p=subprocess.run([str(WRAPPER)]+call,env=env,capture_output=True,text=True)
            self.assertEqual(p.returncode,0,p.stderr)
            self.assertEqual(args.read_text().splitlines(),['exec','-c','model_reasoning_effort="medium"']+call[1:])
            self.assertEqual(auth.read_text().splitlines(),[env['HOME'],env['CODEX_HOME']])
            env['CONTENT_AUDIT_CODEX_MIN_INTERVAL_SECONDS']='0'
            p=subprocess.run([str(WRAPPER),'exec','-m','gpt-6.1-sol','-'],env=env,capture_output=True,text=True)
            self.assertEqual(p.returncode,0,p.stderr)
            self.assertEqual(args.read_text().splitlines(),['exec','--ignore-user-config','-c','model_reasoning_effort="medium"','-m','gpt-6.1-sol','-'])
            p=subprocess.run([str(WRAPPER),'login','status'],env=env,capture_output=True,text=True)
            self.assertEqual(p.returncode,0,p.stderr);self.assertEqual(args.read_text().splitlines(),['login','status'])


if __name__=='__main__':unittest.main()
