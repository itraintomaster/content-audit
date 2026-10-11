"""El paso a producción de una tanda publicada (F-PUBPROD R006 a R009).

  python3 scripts/publish_revisions.py --batch <id> --workdir <campaña> --uri <URI>
      Ensayo: lee la tanda y su lista, se conecta, compara campo por campo y
      cuenta qué escribiría. No escribe nada en la base ni en el curso.

  python3 scripts/publish_revisions.py --batch <id> --workdir <campaña> --uri <URI> \\
      --apply --expect-destination <servidor>/<base>
      Aplicación: exige un ensayo limpio previo de la misma lista contra el mismo
      destino, con las mismas opciones, y que el destino nombrado sea el real.

Todo lo que se niega se niega antes de conectarse. El URI nunca se imprime ni se
registra: solo el servidor y la base. Las excepciones de la base se muestran solo
por su clase.

Salida: 0 ensayo limpio o aplicación verificada; 1 ensayo con problemas,
aplicación abortada o verificación fallida; 2 negada.
"""

import argparse
import os
import re
import sys
from datetime import datetime, timezone

from . import applier, manifest_reader, registry, write_plan
from .manifest_reader import Refusal
from .registry import (ABORTED, APPLIED_VERIFIED, APPLY, DRY_RUN, DRY_RUN_CLEAN, DRY_RUN_WITH_PROBLEMS, EXIT_OK,
                       EXIT_PROBLEMS, EXIT_REFUSED, REFUSED, STARTED, VERIFY_FAILED, RegistryBusy, RegistryError,
                       RunLog)

TOOL = "production_publish"
REPO_DEFAULT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Cualquier cosa con forma de URI («esquema://…») se tapa antes de mostrarse.
URI_LIKE = re.compile(r"[A-Za-z][A-Za-z0-9+.-]*://\S*")


def hide_uris(text):
    return URI_LIKE.sub("<URI oculto>", text)


class _Parser(argparse.ArgumentParser):
    def error(self, message):
        self.print_usage(sys.stderr)
        self.exit(EXIT_REFUSED, "%s: error: %s\n" % (self.prog, hide_uris(message)))


def build_parser():
    p = _Parser(prog="publish_revisions.py --batch", allow_abbrev=False,
                description="Lleva a producción lo que dice la lista de una tanda publicada. "
                            "Sin --apply es un ensayo y no escribe nada.")
    p.add_argument("--batch", required=True, help="identidad de la tanda publicada")
    p.add_argument("--workdir", default=REPO_DEFAULT,
                   help="carpeta de la campaña (la que tiene .content-audit/batches/); "
                        "ahí queda también el registro de producción (default: el repo)")
    p.add_argument("--uri", required=True, help="URI de la base; nunca se imprime ni se registra")
    p.add_argument("--db", default="learney", help="base (default: learney)")
    p.add_argument("--apply", action="store_true",
                   help="escribe; exige un ensayo limpio previo y --expect-destination")
    p.add_argument("--expect-destination", metavar="SERVIDOR/BASE",
                   help="el destino que José nombra; si no es el real, no se escribe")
    p.add_argument("--skip-drift", action="store_true",
                   help="sigue sin los documentos con deriva o inexistentes, y los registra")
    p.add_argument("--skip-unsupported", action="store_true",
                   help="sigue aunque la lista declare cambios sin lugar en producción, y los registra")
    return p


def _now():
    return datetime.now(timezone.utc)


class Run(RunLog):
    """Una corrida del paso por tanda: cada renglón lleva además la tanda, la lista y las opciones."""

    def __init__(self, args, out, clock):
        self.workdir = os.path.abspath(args.workdir)
        super().__init__(self.workdir, TOOL, APPLY if args.apply else DRY_RUN, clock, out)
        self.args = args
        self.flags = {"skipDrift": bool(args.skip_drift), "skipUnsupported": bool(args.skip_unsupported)}
        self.loaded = None

    def row(self, result, **extra):
        return super().row(result, batchId=self.args.batch,
                           planId=self.loaded.plan_id if self.loaded else None,
                           manifestDigest=self.loaded.digest if self.loaded else None,
                           flags=self.flags, **extra)

    def refuse(self, refusal):
        self.say("NEGADO (%s): %s" % (refusal.code, refusal.message))
        for code, message in refusal.problems[1:manifest_reader.MAX_LISTED_PROBLEMS]:
            self.say("  - (%s) %s" % (code, message))
        if len(refusal.problems) > manifest_reader.MAX_LISTED_PROBLEMS:
            self.say("  … y %d problemas más." % (len(refusal.problems) - manifest_reader.MAX_LISTED_PROBLEMS))
        self.say("No se conectó a ninguna base y no se escribió nada.")
        self.record(REFUSED, reason=refusal.as_reason())
        self.say_registry()
        return EXIT_REFUSED

    def say_registry(self, detail=None):
        where = self.registry.relative(self.registry.path)
        if detail:
            self.say("Registro: %s; detalle y respaldo: %s (corrida %s)" % (where, detail, self.run_id))
        else:
            self.say("Registro: %s (corrida %s)" % (where, self.run_id))


def main(argv=None, *, store_factory=None, out=None, now=None):
    """`store_factory(uri, db)` abre la base (en las pruebas, una colección falsa)."""
    args = build_parser().parse_args(argv)
    run = Run(args, out if out is not None else sys.stdout, now or _now)
    try:
        run.destination = registry.destination_of(args.uri, args.db)
    except ValueError as e:
        return run.refuse(Refusal("BAD_URI", "No se puede leer el destino: %s." % e))
    try:
        with run.registry.lock():
            return _locked(run, store_factory)
    except RegistryBusy as e:
        return run.refuse(Refusal("REGISTRY_BUSY", str(e)))
    except RegistryError as e:
        run.say("!! %s" % e)
        run.say("No se escribió nada en la base.")
        return EXIT_REFUSED


def _locked(run, store_factory):
    args = run.args
    try:
        run.loaded = manifest_reader.load(run.workdir, args.batch)
    except Refusal as refusal:
        return run.refuse(refusal)
    refusal = _refusal_before_connecting(run)
    if refusal:
        return run.refuse(refusal)

    if store_factory is None:
        from .mongo_store import open_store
        store_factory = open_store

    _header(run)
    store = None
    try:
        try:
            store = store_factory(args.uri, args.db)
            plan = _plan_from_store(run, store)
        except _Abort as abort:
            return _aborted(run, abort.code, abort.message)
        except Exception as e:  # la clase alcanza: el mensaje de la base puede traer el destino
            return _aborted(run, "STORE_ERROR", "La base falló al leer (%s); no se escribió nada."
                            % type(e).__name__, errorClass=type(e).__name__)
        _plan_summary(run, plan)
        if not args.apply:
            return _dry_run(run, plan)
        return _execute(run, store, plan)
    finally:
        if store is not None:
            try:
                store.close()
            except Exception:
                pass


def _refusal_before_connecting(run):
    """Con la lista ya leída: lo sin lugar en producción, el destino nombrado y el ensayo limpio previo.

    Devuelve la negativa, o None si se puede seguir.
    """
    args = run.args
    unsupported = run.loaded.manifest.get("unsupported") or []
    if unsupported and not args.skip_unsupported:
        return Refusal(
            "UNSUPPORTED",
            "La lista declara %d cambio(s) sin lugar en producción (%s). Para seguir sin ellos, "
            "pedilo con --skip-unsupported." % (len(unsupported), ", ".join(
                "%s/%s" % (u.get("changeId"), u.get("part")) for u in unsupported[:10])))

    if args.expect_destination is not None:
        try:
            expected = registry.parse_expected_destination(args.expect_destination)
        except ValueError as e:
            return Refusal("BAD_EXPECTED_DESTINATION", str(e))
        if expected != run.destination:
            return Refusal("DESTINATION_MISMATCH", "El destino nombrado (%s) no es el real (%s): no se escribe."
                           % (registry.destination_label(expected), registry.destination_label(run.destination)))
    elif args.apply:
        return Refusal("DESTINATION_NOT_NAMED", "Para aplicar hay que nombrar el destino con "
                       "--expect-destination %s." % registry.destination_label(run.destination))

    if args.apply:
        try:
            rows = run.registry.rows()
        except RegistryError as e:
            return Refusal("REGISTRY_UNREADABLE", str(e))
        problem = registry.clean_dry_run_problem(rows, TOOL, args.batch, run.loaded.digest,
                                                 run.destination, run.flags)
        if problem:
            return Refusal("NO_CLEAN_DRY_RUN", problem)
    return None


class _Abort(Exception):
    def __init__(self, code, message):
        super().__init__(message)
        self.code = code
        self.message = message


def _plan_from_store(run, store):
    manifest = run.loaded.manifest
    missing = sorted(write_plan.required_collections(manifest) - set(store.collections()))
    if missing:
        raise _Abort("WRONG_DATABASE",
                     "La base %s no tiene %s: ¿es la base correcta? No se escribió nada."
                     % (registry.destination_label(run.destination), ", ".join(missing)))
    return write_plan.build(manifest, store, run.args.skip_drift)


def _header(run):
    loaded = run.loaded
    reviews = loaded.manifest.get("reviews", {})
    run.say("Paso a producción — tanda %s (plan %s)" % (loaded.batch_id, loaded.plan_id))
    run.say("  lista:   %s (huella %s)" % (run.registry.relative(loaded.manifest_path), loaded.digest[:12]))
    run.say("  destino: %s" % registry.destination_label(run.destination))
    run.say("  modo:    %s" % ("APLICACIÓN" if run.args.apply else "ENSAYO (no escribe nada)"))
    run.say("  revisión: %s aprobados, %s sin revisar (viajan), %s rechazados (quedaron afuera)"
            % (reviews.get("approved"), reviews.get("pending"), reviews.get("rejected")))
    if run.args.skip_unsupported and loaded.manifest.get("unsupported"):
        run.say("  sin lugar en producción, se sigue sin ellos: %d" % len(loaded.manifest["unsupported"]))


def _plan_summary(run, plan):
    c = plan.counts()
    run.say("  documentos de la lista: %d; copias embebidas en %s: %d"
            % (c["documents"], write_plan.INSTANCES, c["instances"]))
    run.say("  a escribir: %d documento(s), %d campo(s); ya aplicados: %d; con deriva: %d; inexistentes: %d"
            % (c["documentsToWrite"], c["fieldsToWrite"], c["already"], c["drift"], c["missing"]))
    if c["skippedDrift"] or c["skippedWithMaster"]:
        run.say("  salteados por --skip-drift: %d (más %d copias de esos ejercicios)"
                % (c["skippedDrift"], c["skippedWithMaster"]))
    if c["staleCopies"]:
        run.say("  copias embebidas que ya estaban desfasadas (no se tocan): %d" % c["staleCopies"])
    for line in plan.report_lines():
        run.say(line)


def _run_detail(run, plan, result, extra=None):
    loaded = run.loaded
    detail = {
        "result": result,
        "startedAt": registry.utc_iso(run.started),
        "batchId": loaded.batch_id,
        "planId": loaded.plan_id,
        "manifestDigest": loaded.digest,
        "manifest": run.registry.relative(loaded.manifest_path),
        "flags": run.flags,
        "note": ("Ensayo: «old» es lo que hay hoy y «new» lo que se escribiría; no se escribió nada."
                 if run.mode == DRY_RUN else
                 "Respaldo: por cada campo escrito, «old» es lo que había y «new» lo que se escribió."),
        "counts": plan.counts(),
        "skippedUnsupported": _skipped_unsupported(run),
        "documents": [d.detail() for d in plan.all()],
    }
    if extra:
        detail.update(extra)
    return detail


def _skipped_unsupported(run):
    if not run.args.skip_unsupported:
        return []
    return run.loaded.manifest.get("unsupported") or []


def _common(run, plan):
    c = plan.counts()
    return {
        "counts": c,
        "reviews": run.loaded.manifest.get("reviews"),
        "skippedDrift": [d.key for d in plan.skipped(write_plan.SKIP_DRIFT)],
        "staleCopies": len(plan.skipped(write_plan.SKIP_STALE_COPY)),
        "skippedUnsupported": [u.get("changeId") for u in _skipped_unsupported(run)],
    }


def _dry_run(run, plan):
    result = DRY_RUN_CLEAN if plan.is_clean() else DRY_RUN_WITH_PROBLEMS
    reason = None
    if not run.write_detail(_run_detail(run, plan, result)):
        result = DRY_RUN_WITH_PROBLEMS
        reason = {"code": "DETAIL_NOT_WRITTEN", "message": "no se pudo escribir el detalle del ensayo"}
    if result == DRY_RUN_WITH_PROBLEMS and reason is None:
        reason = _plan_problem(plan)
    recorded = run.record(result, reason=reason,
                          wouldWrite=len(plan.to_write()),
                          pendingTraveled=plan.pending_documents(plan.to_write()),
                          **_common(run, plan))
    if not recorded:
        result = DRY_RUN_WITH_PROBLEMS
    if result == DRY_RUN_CLEAN:
        run.say("Resultado: %s — el ensayo salió limpio; no se escribió nada. Para aplicar, con las "
                "mismas opciones: --apply --expect-destination %s"
                % (result, registry.destination_label(run.destination)))
    elif plan.blocked():
        run.say("Resultado: %s — no se escribió nada. Lo BLOQUEADO no se saltea con --skip-drift: la "
                "lista tiene que venir sin esos cambios." % result)
    else:
        run.say("Resultado: %s — no se escribió nada. Resolvé lo de arriba (o seguí sin esos documentos "
                "con --skip-drift) y ensayá de nuevo." % result)
    run.say_registry()
    return EXIT_OK if result == DRY_RUN_CLEAN else EXIT_PROBLEMS


def _plan_problem(plan):
    """El motivo por el que el plan frena la corrida: lo bloqueado antes que la deriva."""
    blocked = plan.blocked()
    if blocked:
        code = blocked[0].blocked[0]
        return {"code": code, "message": "%d documento(s) bloqueado(s) (%s): cambios del formulario que no se "
                                         "publican" % (len(blocked), code)}
    return {"code": "DRIFT", "message": "%d documento(s) con deriva o inexistentes" % len(plan.problems())}


def _aborted(run, code, message, **extra):
    run.say("ABORTADO (%s): %s" % (code, message))
    run.record(ABORTED, reason={"code": code, "message": message}, **extra)
    run.say_registry()
    return EXIT_PROBLEMS


def _execute(run, store, plan):
    if not plan.is_clean():
        # El ensayo limpio pudo quedar viejo: alguien cambió la base entretanto.
        problem = _plan_problem(plan)
        return _aborted(run, problem["code"], "%s desde el ensayo; no se escribió nada." % problem["message"],
                        **_common(run, plan))
    to_write = plan.to_write()
    problem = run.begin_writing(_run_detail(run, plan, STARTED), planned=len(to_write), **_common(run, plan))
    if problem:
        run.say("ABORTADO: %s." % problem)
        run.say_registry()
        return EXIT_PROBLEMS
    detail_rel = run.registry.relative(run.detail_path)

    run.say("Escribiendo %d documento(s)…" % len(to_write))
    outcome = applier.apply(plan, store)  # un Ctrl-C no se lleva el resultado: queda adentro
    result = _result_of(outcome)

    written_ids = ["%s/%s" % (d.collection, d.doc_id) for d in outcome.written]
    stopped_at = outcome.stopped_at.key if outcome.stopped_at is not None else None
    extra = {
        "written": written_ids,
        "verification": outcome.verification,
        "indeterminate": outcome.indeterminate,
        "stoppedReason": outcome.stopped_reason,
        "stoppedAt": stopped_at,
        "errorClass": outcome.error_class,
    }
    if not run.write_detail(_run_detail(run, plan, result, extra)):
        run.say("!! El respaldo previo a escribir quedó en %s." % detail_rel)

    reason = None
    if result == VERIFY_FAILED:
        reason = {"code": "VERIFY_FAILED", "message": "%d documento(s) no quedaron como dice la lista"
                  % len(outcome.failures())}
    elif result == ABORTED:
        reason = {"code": outcome.stopped_reason,
                  "message": "se dejó de escribir en %s" % (stopped_at or "(antes de empezar)")}
    recorded = run.record(result, reason=reason, writtenIds=written_ids,
                          indeterminate=outcome.indeterminate_keys(), verified=result == APPLIED_VERIFIED,
                          pendingTraveled=plan.pending_documents(outcome.written),
                          stoppedReason=outcome.stopped_reason, stoppedAt=stopped_at,
                          errorClass=outcome.error_class, **_common(run, plan))

    _print_outcome(run, plan, outcome, result, stopped_at)
    run.say_registry(detail_rel)
    if not recorded:
        return EXIT_PROBLEMS
    return EXIT_OK if result == APPLIED_VERIFIED else EXIT_PROBLEMS


def _result_of(outcome):
    """Verificación fallida antes que corte: lo que no quedó como dice la lista es lo más grave."""
    if not outcome.verified:
        return VERIFY_FAILED
    if outcome.stopped_reason or outcome.indeterminate:
        return ABORTED
    return APPLIED_VERIFIED


def _print_outcome(run, plan, outcome, result, stopped_at):
    failures = outcome.failures()
    run.say("Escritos: %d de %d." % (len(outcome.written), len(plan.to_write())))
    if outcome.stopped_reason == applier.CONCURRENT_CHANGE:
        run.say("Se dejó de escribir en %s: cambió en la base entre la lectura y la escritura." % stopped_at)
    elif outcome.stopped_reason == applier.STORE_ERROR:
        run.say("Se dejó de escribir en %s: la base falló (%s)." % (stopped_at, outcome.error_class))
    elif outcome.stopped_reason == applier.INTERRUPTED:
        run.say("Se interrumpió la escritura%s." % (" en %s" % stopped_at if stopped_at else ""))
    for item in outcome.indeterminate:
        run.say("  NO SE SABE si quedó escrito %s/%s (%s): revisalo contra el respaldo (old) del detalle."
                % (item["collection"], item["id"], item["problem"]))
    for failure in failures[:30]:
        run.say("  NO VERIFICA %s/%s: %s" % (failure["collection"], failure["id"],
                                            "; ".join(failure["problems"])))
    if len(failures) > 30:
        run.say("  … y %d más (todo queda en el detalle de la corrida)." % (len(failures) - 30))
    run.say("Resultado: %s%s" % (result, {
        APPLIED_VERIFIED: _applied_summary(plan, outcome),
        ABORTED: " — no se reintenta ni se corrige; lo escrito está verificado y el respaldo, en el detalle.",
        VERIFY_FAILED: " — no se reintenta ni se corrige: revisá el detalle; el respaldo (old) está ahí.",
    }[result]))


def _applied_summary(plan, outcome):
    already = plan.counts()["already"]
    skipped = len(plan.skipped())
    if not outcome.written:
        return (" — se escribieron 0 documentos (ya tenían lo que dice la lista: %d; salteados: %d, ver "
                "arriba); no se tocó nada." % (already, skipped))
    return (" — se escribieron %d documento(s) y quedaron como dice la lista, con el resto de cada documento "
            "idéntico (ya lo tenían: %d; salteados: %d)." % (len(outcome.written), already, skipped))


if __name__ == "__main__":
    raise SystemExit(main())
