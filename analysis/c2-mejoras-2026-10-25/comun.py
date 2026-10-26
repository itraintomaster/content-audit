# Prueba del 25/10: rehacer la C2 desde la C1 con el corrector que guarda las mejoras (brief 217).
# Mismo procedimiento que analysis/correction2-real-2026-10-24, contra la campaña real A1 · V11.
from pathlib import Path
import subprocess, json, os, time, datetime

REPO = Path('/Users/josecullen/projects/learney/content-audit')
OUT = Path(__file__).parent
WORK = REPO / '.content-audit/campaigns/a1-v11'
C1 = 'tanda-A1-20261007T084434Z'
DIEZ = json.loads((REPO / 'analysis/correction2-real-2026-10-24/quiz-ids.json').read_text())
CAMARA = '67fab6d59930102295342de8'
ENV = {**os.environ,
       **json.loads((REPO / 'analysis/correction2-real-2026-10-24/comparison-complete/runtime-environment.json').read_text()),
       'JAVA_HOME': '/Users/josecullen/.sdkman/candidates/java/25.0.2-open',
       'CONTENT_AUDIT_CONTENT_FOLDER': str(REPO / 'db/english-course')}
ENV['PATH'] = ENV['JAVA_HOME'] + '/bin:' + ENV['PATH']


def batch(command, request, label, timeout=600):
    start = time.monotonic()
    q = subprocess.run(['bash', str(REPO / 'audit-cli.sh'), '--workdir', str(WORK), 'batch', command,
                        '--request-json', json.dumps(request), '-f', 'json'],
                       cwd=REPO, env=ENV, capture_output=True, text=True, timeout=timeout)
    (OUT / (label + '.json')).write_text(q.stdout)
    (OUT / (label + '.stderr.log')).write_text(q.stderr)
    print(label, 'exit', q.returncode, round(time.monotonic() - start, 1), 's', flush=True)
    if q.returncode:
        raise RuntimeError(q.stderr[-2500:])
    return json.loads(q.stdout)


def ahora():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()
