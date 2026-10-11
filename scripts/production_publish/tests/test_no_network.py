"""Todo el camino corre sin red y sin el driver de Mongo.

Con la red cortada (cualquier intento de conexión se anota y falla) y con
pymongo y bson imposibles de importar, se importan los tres escritores y se
corre el paso a producción entero contra la colección falsa: ensayo, aplicación,
verificación y registro. Si algo intentara conectarse, la prueba lo ve aunque el
código se trague el error.
"""

import contextlib
import io
import socket
import sys
import unittest
from unittest import mock

from production_publish import registry
from production_publish.tests import fakes
from production_publish.tests.fake_driver import load_script

SCRIPTS = ("publish_revisions", "publish_knowledge_labels", "publish_knowledge_instructions")


class NoNetworkTestCase(unittest.TestCase):
    def setUp(self):
        self.attempts = []

        def refuse(*args, **kwargs):
            self.attempts.append(repr(args[-1:])[:80])
            raise OSError("la prueba cortó la red")

        for target, name in ((socket.socket, "connect"), (socket.socket, "connect_ex"), (socket, "create_connection"),
                             (socket, "getaddrinfo")):
            patcher = mock.patch.object(target, name, refuse)
            patcher.start()
            self.addCleanup(patcher.stop)
        # None en sys.modules: `import pymongo` falla (ModuleNotFoundError), esté instalado o no.
        patcher = mock.patch.dict(sys.modules, {"pymongo": None, "pymongo.errors": None, "bson": None,
                                                "bson.json_util": None})
        patcher.start()
        self.addCleanup(patcher.stop)
        self.workdir = fakes.temp_workdir(self)

    def tearDown(self):
        self.assertEqual(self.attempts, [], "algo intentó usar la red")


class NoNetworkTest(NoNetworkTestCase):
    def test_F_PUBPROD_R009_los_tres_escritores_se_importan_sin_el_driver(self):
        for name in SCRIPTS:
            with self.subTest(script=name):
                module = load_script(name)
                self.assertTrue(callable(module.main))
                out = io.StringIO()
                with contextlib.redirect_stdout(out), self.assertRaises(SystemExit) as ctx:
                    module.main(["--help"])
                self.assertEqual(ctx.exception.code, 0)

    def test_F_PUBPROD_R006_el_paso_por_tanda_entero_corre_sin_red(self):
        fakes.write_batch(self.workdir)
        factory = fakes.FakeStoreFactory()
        code, _, _ = fakes.run_cli(self.workdir, factory=factory)
        self.assertEqual(code, 0)
        code, out, _ = fakes.run_cli(self.workdir, *fakes.APPLY_ARGS, factory=factory)
        self.assertEqual(code, 0, out)
        self.assertEqual([r["result"] for r in fakes.registry_rows(self.workdir)],
                         [registry.DRY_RUN_CLEAN, registry.STARTED, registry.APPLIED_VERIFIED])

    def test_F_PUBPROD_R006_las_negativas_por_tanda_desde_publish_revisions_no_conectan(self):
        module = load_script("publish_revisions")
        fakes.write_batch(self.workdir, status="CLOSED")
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            code = module.main(["--batch", fakes.BATCH_ID, "--workdir", self.workdir, "--uri", fakes.URI,
                                "--apply", "--expect-destination", fakes.DESTINATION])
        self.assertEqual(code, 2)
        self.assertEqual(fakes.registry_rows(self.workdir)[-1]["reason"]["code"], "NOT_PUBLISHED")

    def test_F_PUBPROD_R008_sin_el_driver_el_paso_por_tanda_aborta_y_lo_registra(self):
        from production_publish import cli

        fakes.write_batch(self.workdir)
        out = io.StringIO()
        code = cli.main(["--batch", fakes.BATCH_ID, "--workdir", self.workdir, "--uri", fakes.URI],
                        out=out, now=fakes.Clock())
        self.assertEqual(code, 1)
        row = fakes.registry_rows(self.workdir)[-1]
        self.assertEqual((row["result"], row["errorClass"]), (registry.ABORTED, "ModuleNotFoundError"))

    def test_F_PUBPROD_R008_sin_el_driver_los_otros_escritores_abortan_y_lo_registran(self):
        for name in SCRIPTS:
            with self.subTest(script=name):
                repo = fakes.temp_workdir(self)
                module = load_script(name)
                argv = ["--uri", fakes.URI, "--repo", repo, "--apply"]
                if name == "publish_revisions":
                    argv += ["--plan", "un-plan-de-prueba"]
                with mock.patch.object(module, "REPO_DEFAULT", repo), \
                        contextlib.redirect_stdout(io.StringIO()), self.assertRaises(ImportError):
                    module.main(argv)
                row = fakes.registry_rows(repo)[-1]
                self.assertEqual((row["tool"], row["result"], row["errorClass"]),
                                 (name, registry.ABORTED, "ModuleNotFoundError"))


if __name__ == "__main__":
    unittest.main()
