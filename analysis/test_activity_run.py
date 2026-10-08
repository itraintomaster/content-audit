import json
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path

import activity_run


class ActivityRunTest(unittest.TestCase):
    def test_start_publica_una_corrida_viva(self):
        with tempfile.TemporaryDirectory() as temporary:
            workdir = Path(temporary)
            path = activity_run.start(
                Namespace(
                    workdir=workdir,
                    id="formal v8",
                    kind="JUDGE_EXAM",
                    title="Formal V8",
                    scope="111 pares",
                    provider="codex-cli",
                    model="gpt-6.1-sol",
                    reasoning_effort="medium",
                    version="v8",
                    total=205,
                    unit="consultas",
                    summary=None,
                    artifact=[],
                )
            )

            record = json.loads(path.read_text())
            feed = json.loads(
                (workdir / ".content-audit/activity/activity.json").read_text()
            )
            self.assertEqual(record["status"], "RUNNING")
            self.assertEqual(record["progress"]["completed"], 0)
            self.assertEqual(feed["currentRunId"], "formal v8")

    def test_start_y_update_guardan_la_linea_de_salida_de_la_ejecucion(self):
        with tempfile.TemporaryDirectory() as temporary:
            workdir = Path(temporary)
            path = activity_run.start(
                Namespace(
                    workdir=workdir,
                    id="correccion-viva",
                    kind="COURSE_CORRECTION",
                    title="Corrección viva",
                    scope="A1",
                    provider="codex-cli",
                    model="gpt-6.1-sol",
                    reasoning_effort="medium",
                    version="v11",
                    total=1000,
                    unit="consultas",
                    total_kind="CEILING",
                    summary=None,
                    artifact=[],
                    baseline_queries=635,
                    baseline_tasks_attempted=917,
                    baseline_proposals=719,
                    baseline_resolved=789,
                    baseline_no_winner=42,
                )
            )

            record = json.loads(path.read_text())
            self.assertEqual(record["baseline"]["queries"], 635)
            self.assertEqual(record["baseline"]["tasksAttempted"], 917)

            activity_run.update(
                Namespace(
                    workdir=workdir,
                    id="correccion-viva",
                    status="RUNNING",
                    completed=33,
                    summary="Sigue avanzando.",
                    next=None,
                    stages=[
                        {
                            "label": "Analizadores locales",
                            "status": "RUNNING",
                            "result": "Proceso activo.",
                        }
                    ],
                    facts=[
                        {
                            "label": "Señal",
                            "value": "Activa",
                            "tone": "POSITIVE",
                        }
                    ],
                    finish=False,
                    baseline_queries=None,
                    baseline_tasks_attempted=None,
                    baseline_proposals=719,
                    baseline_resolved=None,
                    baseline_no_winner=None,
                    progress_estimate={
                        "status": "AVAILABLE",
                        "ratePerMinute": 8.5,
                        "remainingSecondsLow": 120,
                        "remainingSecondsHigh": 240,
                    },
                )
            )

            updated = json.loads(path.read_text())
            self.assertEqual(updated["progress"]["completed"], 33)
            self.assertEqual(updated["progress"]["totalKind"], "CEILING")
            self.assertEqual(
                updated["progress"]["estimate"]["status"], "AVAILABLE"
            )
            self.assertEqual(updated["baseline"]["queries"], 635)
            self.assertEqual(updated["baseline"]["proposals"], 719)
            self.assertEqual(updated["stages"][0]["status"], "RUNNING")
            self.assertEqual(updated["facts"][0]["value"], "Activa")

    def test_update_no_confunde_fin_tecnico_con_aprobacion(self):
        with tempfile.TemporaryDirectory() as temporary:
            workdir = Path(temporary)
            activity_run.start(
                Namespace(
                    workdir=workdir,
                    id="lote-1",
                    kind="CONTENT_CORRECTION",
                    title="Lote 1",
                    scope="6 tareas",
                    provider="codex-cli",
                    model="gpt-6.1-sol",
                    reasoning_effort="medium",
                    version=None,
                    total=6,
                    unit="tareas",
                    summary=None,
                    artifact=[],
                )
            )

            path = activity_run.update(
                Namespace(
                    workdir=workdir,
                    id="lote-1",
                    status="NEEDS_REVIEW",
                    completed=6,
                    summary="El proceso terminó; falta leer el resultado.",
                    next="Revisar la evidencia.",
                    finish=True,
                )
            )

            record = json.loads(path.read_text())
            self.assertEqual(record["status"], "NEEDS_REVIEW")
            self.assertEqual(record["progress"]["completed"], 6)
            self.assertIn("finishedAt", record)

    def test_start_guarda_tanda_y_modelo_especial_de_consigna(self):
        with tempfile.TemporaryDirectory() as temporary:
            workdir = Path(temporary)
            path = activity_run.start(
                Namespace(
                    workdir=workdir,
                    id="validacion-a1",
                    kind="COURSE_VALIDATION",
                    batch_id="tanda-A1-prueba",
                    title="Validar A1",
                    scope="batchId=tanda-A1-prueba · cierre",
                    provider="codex-cli",
                    model="gpt-6.1-sol",
                    instruction_model="gpt-6-luna",
                    reasoning_effort="medium",
                    version="v11",
                    total=20,
                    unit="consultas",
                    summary=None,
                    artifact=[],
                )
            )

            record = json.loads(path.read_text())
            self.assertEqual(record["batchId"], "tanda-A1-prueba")
            self.assertEqual(record["instructionModel"], "gpt-6-luna")
            self.assertEqual(
                record["facts"],
                [
                    {
                        "label": "Modelo de consigna",
                        "value": "gpt-6-luna",
                        "tone": "NEUTRAL",
                    }
                ],
            )


if __name__ == "__main__":
    unittest.main()
