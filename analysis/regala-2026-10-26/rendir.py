# Rinde y mide el examen del juez «No regala la respuesta» (FEAT-REGALA), 26/10/2026.
# Mismo procedimiento que analysis/c2-mejoras-2026-10-25/examen_sa.py: exam run con tope duro en la campaña A1 · V11,
# la medida contra gold.json y, si todo se juzgó, exam record.
# Uso: rendir.py <etiqueta> [<tope>]   (sin tope: ensayo, no consulta)
import json, subprocess, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / 'c2-mejoras-2026-10-25'))
from comun import REPO, WORK, ENV  # noqa: E402

AQUI = Path(__file__).parent
EXAMEN = AQUI / 'examen'
JUEZ = 'answer-giveaway'


def cli(*args, timeout=7200):
    q = subprocess.run(['bash', str(REPO / 'audit-cli.sh'), '--workdir', str(WORK), *args], cwd=REPO, env=ENV,
                       capture_output=True, text=True, timeout=timeout)
    print(' '.join(args[:3]), 'exit', q.returncode, '\n', q.stdout[-1500:], q.stderr[-1500:], flush=True)
    if q.returncode:
        raise SystemExit(q.returncode)
    return q


def medir(veredictos, metricas, salida):
    gold = json.loads((EXAMEN / 'gold.json').read_text())
    filas = {}
    for linea in veredictos.read_text().splitlines():
        if linea.strip():
            fila = json.loads(linea)
            filas[fila['ejercicio']] = fila
    m = json.loads(metricas.read_text())
    casos, grupos, sin_juzgar, no_esperado = [], {}, [], []
    for c in gold['casos']:
        fila = filas.get(c['ejercicio'])
        obtenido = fila['resultado'] if fila else 'NOT_JUDGED'
        bien = obtenido == c['esperado']
        if obtenido == 'NOT_JUDGED':
            sin_juzgar.append(c['clave'])
        if not bien:
            no_esperado.append(c['ejercicio'])
        g = grupos.setdefault(c['grupo'], {'grupo': c['grupo'], 'casos': 0, 'comoSeEspera': 0, 'noComoSeEspera': 0})
        g['casos'] += 1
        g['comoSeEspera' if bien else 'noComoSeEspera'] += 1
        casos.append({**c, 'obtenido': obtenido, 'comoSeEspera': bien, 'gravedad': fila and fila.get('gravedad'),
                      'detalle': fila and fila.get('detalle')})
    meta = gold['meta']
    alcanzo = (not sin_juzgar and grupos['regala']['comoSeEspera'] >= meta['regalaComoSeEspera']
               and grupos['no-regala']['noComoSeEspera'] <= meta['noRegalaFalsasAlarmas'])
    salida.mkdir(parents=True, exist_ok=False)
    # Las discrepancias primero, con su leyenda (AGENTS.md).
    report = {'leyenda': {'FAILS': 'el juez dice que regala', 'PASSES': 'el juez dice que no regala',
                          'NOT_JUDGED': 'sin veredicto usable: no cuenta como acierto'},
              'discrepancias': [c for c in casos if not c['comoSeEspera']], 'grupos': list(grupos.values()),
              'meta': meta, 'alcanzoLaMeta': alcanzo, 'sinJuzgar': sin_juzgar, 'consultas': m.get('queriesUsed'),
              'casos': casos}
    (salida / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=1))
    print(json.dumps({'grupos': report['grupos'], 'alcanzoLaMeta': alcanzo, 'consultas': report['consultas'],
                      'discrepancias': [(c['clave'], c['esperado'], c['obtenido']) for c in report['discrepancias']]},
                     ensure_ascii=False), flush=True)
    if sin_juzgar:
        print('quedan sin juzgar', sin_juzgar, '— no se escribe measured.json')
        return None
    runtime = m['runtime']
    measured = {'juez': JUEZ, 'version': m['judgeVersion'], 'estado': 'APROBADO' if alcanzo else 'REPROBADO',
                'grupos': list(grupos.values()), 'noDieronLoEsperado': no_esperado, 'alcanzoLaMeta': alcanzo,
                'foto': str(EXAMEN / 'course'),
                'medidoPor': 'analysis/regala-2026-10-26/rendir.py; regala ≥ 10/11 y a lo sumo 1 falsa alarma en 21; '
                             'referencias editoriales del asistente, no verdad certificada',
                'proveedor': runtime.get('providerName'), 'modelo': runtime.get('modelName'),
                'esfuerzo': runtime.get('reasoningEffort'), 'examFingerprint': m.get('examFingerprint'),
                'regressionSuiteFingerprint': m.get('regressionSuiteFingerprint'),
                'regressionCaseIds': m.get('regressionCaseIds') or []}
    (salida / 'measured.json').write_text(json.dumps(measured, ensure_ascii=False, indent=1))
    return salida / 'measured.json'


if __name__ == '__main__':
    etiqueta = sys.argv[1]
    tope = sys.argv[2] if len(sys.argv) > 2 else None
    veredictos, metricas = AQUI / f'examen-{etiqueta}.jsonl', AQUI / f'examen-{etiqueta}-metricas.json'
    if veredictos.exists():
        sys.exit('ya se corrió; no gasto otra vez')
    args = ['exam', 'run', JUEZ, '--cases', str(EXAMEN / 'cases.json'), '--course', str(EXAMEN / 'course'),
            '--concurrency', '4', '--out', str(veredictos), '--metrics-out', str(metricas)]
    if tope:
        args += ['--budget', tope]
    cli(*args)
    if tope:
        medido = medir(veredictos, metricas, AQUI / f'examen-{etiqueta}-medida')
        if medido:
            cli('exam', 'record', str(medido), timeout=600)
