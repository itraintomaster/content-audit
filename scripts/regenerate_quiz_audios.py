#!/usr/bin/env python3
"""Regenera los audios de respuesta (answerAudioUrl) de las quizzes cuyo texto cambio.

Flujo (cada paso es un subcomando; el estado vive en --workdir):

  preflight  compara form.sentences[0] de prod (Atlas, solo lectura) contra db/ HEAD
             para los ids de changed_spoken_sentences.json; solo los que coinciden
             entran al manifest.
  dryrun     arma el manifest clave->texto locutable (limpieza del placeholder "-"),
             separa los casos de revision manual y corre lints (charset, claves
             duplicadas, texto vacio).
  backup     snapshot completo de s3://learney/quizzes-audios/audios_mp3/ al workdir.
  synth      TTS (en-GB-Standard-B, LINEAR16) -> wav -> ffmpeg -> mp3, con workers.
             --keys / --limit para pilotos. Verifica tamanos en disco, no exit codes.
  upload     sube los mp3 generados (content-type audio/mp3) y verifica contra un
             listado fresco de S3 (tamano exacto por clave).
  verify     barrido final: cada clave del manifest existe en S3 con Last-Modified
             de hoy y tamano igual al local.

La URI de Mongo NO vive en este archivo: se lee en runtime del config del proyecto
ittm (aws-lambda-handler/cmd/config.yaml).
"""

import argparse
import base64
import concurrent.futures
import csv
import glob
import json
import os
import re
import subprocess
import sys
import time
import urllib.request

VOICE = {"languageCode": "en-GB", "name": "en-GB-Standard-B"}
AUDIO_ENCODING = "LINEAR16"
GCLOUD_PROJECT = "train-to-master"
S3_PREFIX = "s3://learney/quizzes-audios/audios_mp3"
TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
MONGO_URI_FILE = os.path.expanduser(
    "~/projects/learney/ittm/be/application/serverless/aws-lambda-handler/cmd/config.yaml"
)
DB_GLOB = "db/english-course/*/*/*/quizzes.json"
KNOWLEDGE_GLOB = "db/english-course/*/*/*/_knowledge.json"


# ---------------------------------------------------------------- helpers

def repo_root():
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read_mongo_uri():
    with open(MONGO_URI_FILE) as f:
        for line in f:
            m = re.search(r"(mongodb\+srv://\S+)", line)
            if m:
                return m.group(1)
    raise SystemExit(f"no encontre la URI de Mongo en {MONGO_URI_FILE}")


DIALOG = re.compile(r"^A:\s*(.+?)\s*B:\s*(.+)$", re.S)
LABEL = re.compile(r"^(Ask|No|Sure|OK|First|Zero|Second|Third|Fact|Habit|Guess|Forecast|Polite)\s*:\s*")
# colas clasificatorias que el audio original NO decia (si decia 'correct',
# 'sequence' y 'simultaneous', que por eso no estan aca)
DROP_TAILS = re.compile(r"([.?!])\s+(Defining|Non-defining|Subject|Object|subject|object)$")
CUE = re.compile(r"([\w'-]+)\s*/\s*([\w'-]+)")


def tts_text(s0: str) -> str:
    """Texto locutable a partir de form.sentences[0].

    Reglas (paridad con el pipeline original de 2025, verificada contra los
    aiSentence congelados del ITTM):
      - placeholder '-' de respuesta vacia -> fuera
      - bloques entre parentesis (complementos opcionales de la respuesta,
        p.ej. 'was recycled (by my sister)') -> fuera; el audio original
        decia la oracion pelada
      - dialogos 'A: ... B: ...' -> se locuta solo la respuesta de B, sin
        etiqueta (el original decia 'No, I can't', no el dialogo entero)
      - etiquetas de consigna al inicio ('Fact:', 'Guess:', 'First:', ...)
        -> fuera; el original nunca las decia
      - contracciones despegadas por el deriver ("He 's", "wo n't") -> pegadas
      - espacios y puntuacion normalizados
    """
    # prompt de forma de palabra ('have - having'): se locuta solo la respuesta,
    # igual que el original (decia 'running' para 'run - running')
    wf = re.fullmatch(r"[\w']+ - ([\w']+)", s0.strip())
    if wf:
        return wf.group(1)
    t = re.sub(r"\([^)]*\)", " ", s0)
    m = DIALOG.match(t.strip())
    if m:
        t = m.group(2)
    t = LABEL.sub("", t.strip())
    t = re.sub(r"Link type:\s*", "", t)
    t = DROP_TAILS.sub(r"\1", t.strip())
    t = re.sub(r"(^|\s)-(\s|$)", " ", t)
    return polish(t)


def polish(t: str) -> str:
    t = re.sub(r"\s+'(ll|s|re|ve|d|m|t)\b", r"'\1", t)
    t = re.sub(r"\s+n't\b", "n't", t)
    t = re.sub(r"\s+", " ", t).strip()
    t = re.sub(r"\s+([.?!,;:])", r"\1", t)
    return t


def is_cue_only(text: str) -> bool:
    """TEXT que es puro andamiaje de eleccion ('Our / ours', 'My house / old / your house')."""
    t = (text or "").strip()
    return " / " in t and not re.search(r"[.?!]", t) and len(t) <= 60


def spoken_from_choice_parts(parts) -> str:
    """Oracion locutable para quizzes de eleccion (cue 'X / Y' en el TEXT).

    - cue embebido en una oracion ('I didn't meet something / anything.',
      'Our / ours') cuya respuesta del CLOZE es una de las alternativas ->
      se sustituye por la respuesta y ese CLOZE se consume (no se repite)
    - TEXT que es solo un prompt-cue no sustituible ('My house / old / your
      house' seguido de la oracion completa en el CLOZE) -> fuera
    - CLOZE cuya opcion trae dos variantes de oracion con ' / ' -> se locuta
      la primera (el audio original hacia eso)
    """
    toks, consumed = [], set()
    prev_cloze_value = None
    for i, p in enumerate(parts):
        if p["kind"] == "TEXT":
            t = re.sub(r"\([^)]*\)", " ", p.get("text") or "")
            if not t.strip():
                continue
            m = CUE.search(t)
            substituted = False
            # cue DESPUES de su CLOZE ('She dressed' + [-] + 'herself / -.'):
            # si el cue arranca el TEXT y la respuesta recien emitida es una de
            # sus alternativas, el cue se recorta (la respuesta ya sono)
            if m and prev_cloze_value is not None and not t[: m.start()].strip():
                if prev_cloze_value.lower() in (m.group(1).lower(), m.group(2).lower()):
                    t = t[: m.start()] + t[m.end():]
                    substituted = True
            # cue ANTES de su CLOZE: se sustituye por la respuesta y se consume
            nxt = next((j for j in range(i + 1, len(parts)) if parts[j]["kind"] == "CLOZE"), None)
            if not substituted and m and nxt is not None and nxt not in consumed:
                ans = ((parts[nxt].get("options") or [""])[0] or "").split("|")[0]
                if ans.lower() in (m.group(1).lower(), m.group(2).lower()):
                    t = t[: m.start()] + ans + t[m.end():]
                    consumed.add(nxt)
                    substituted = True
            if not substituted and m and is_cue_only(p.get("text")):
                continue
            if not t.strip():
                continue
            toks.append(t)
            prev_cloze_value = None
        elif p["kind"] == "CLOZE":
            if i in consumed:
                continue
            v = ((p.get("options") or [""])[0] or "").split("|")[0]
            if "/" in v:
                # dos variantes aceptadas en una opcion ("has not kept/hasn't kept",
                # "X yet / X ya"): se locuta la primera. El pipeline viejo decia las
                # dos pegadas cuando no habia espacios: defecto, no estilo.
                v = re.split(r"\s*/\s*", v)[0].strip()
            prev_cloze_value = v
            if v == "-" or not v:
                continue
            toks.append(v)
    return polish(" ".join(toks))


def load_head_quizzes():
    """Quizzes de db/ HEAD (working tree) + sentenceMode del knowledge."""
    root = repo_root()
    modes = {}
    for f in glob.glob(os.path.join(root, KNOWLEDGE_GLOB)):
        k = json.load(open(f))
        kid = k.get("id") or (k.get("_id") or {}).get("$oid")
        modes[kid] = k.get("sentenceMode")
    out = {}
    for f in glob.glob(os.path.join(root, DB_GLOB)):
        for q in json.load(open(f)):
            qid = q.get("id") or (q.get("_id") or {}).get("$oid")
            kid = q.get("knowledgeId")
            if isinstance(kid, dict):
                kid = kid.get("$oid")
            out[qid] = {
                "key": q.get("answerAudioUrl"),
                "mode": modes.get(kid) or "FILL",
                "sentences": (q.get("form") or {}).get("sentences") or [],
                "parts": (q.get("form") or {}).get("sentenceParts") or [],
                "title": q.get("title"),
            }
    return out


def load_changed_ids(workdir):
    path = os.path.join(workdir, "changed_spoken_sentences.json")
    return [c["id"] for c in json.load(open(path))]


# ---------------------------------------------------------------- preflight

def cmd_preflight(args):
    from bson import ObjectId
    from pymongo import MongoClient

    head = load_head_quizzes()
    ids = load_changed_ids(args.workdir)
    client = MongoClient(read_mongo_uri(), serverSelectionTimeoutMS=10000)
    col = client["learney"]["quizTemplates"]

    ok, divergent, missing = [], [], []
    for i in range(0, len(ids), 200):
        chunk = ids[i : i + 200]
        docs = {
            str(d["_id"]): d
            for d in col.find(
                {"_id": {"$in": [ObjectId(x) for x in chunk]}},
                {"form.sentences": 1},
            )
        }
        for qid in chunk:
            d = docs.get(qid)
            if d is None:
                missing.append(qid)
                continue
            prod_s = ((d.get("form") or {}).get("sentences") or [""])[0]
            head_s = (head[qid]["sentences"] or [""])[0]
            if prod_s == head_s:
                ok.append(qid)
            else:
                divergent.append({"id": qid, "key": head[qid]["key"], "prod": prod_s, "head": head_s})
        print(f"  {min(i+200, len(ids))}/{len(ids)}", end="\r")
    report = {"ok": ok, "divergent": divergent, "missing": missing}
    out = os.path.join(args.workdir, "preflight_report.json")
    json.dump(report, open(out, "w"), indent=1, ensure_ascii=False)
    print(f"\nprod==HEAD: {len(ok)} | divergentes: {len(divergent)} | no existen en prod: {len(missing)}")
    print(f"reporte: {out}")


# ---------------------------------------------------------------- dryrun

SUSPICIOUS = re.compile(r"[$_\[\]|{}<>*#@%^~`()]")


def cmd_dryrun(args):
    head = load_head_quizzes()
    pre = json.load(open(os.path.join(args.workdir, "preflight_report.json")))
    ids = pre["ok"]

    overrides = {}
    ov_path = os.path.join(args.workdir, "manual_overrides.json")
    if os.path.exists(ov_path):
        overrides = json.load(open(ov_path))

    manifest, manual, lint = [], [], []
    keys_seen = {}
    for qid in ids:
        q = head[qid]
        s0 = (q["sentences"] or [""])[0]
        row = {"id": qid, "key": q["key"], "mode": q["mode"], "raw": s0}
        if not q["key"]:
            lint.append({**row, "issue": "sin answerAudioUrl"})
            continue
        if q["key"] in keys_seen:
            lint.append({**row, "issue": f"clave duplicada con {keys_seen[q['key']]}"})
            continue
        keys_seen[q["key"]] = qid
        if q["key"] in overrides:
            manifest.append({**row, "text": overrides[q["key"]], "source": "override"})
            continue
        if "/" in s0:
            text = spoken_from_choice_parts(q["parts"])
            row["source"] = "parts"
        else:
            text = tts_text(s0)
        row["text"] = text
        enders = len(re.findall(r"[.?!]", text))
        if not text or len(text) < 4:
            lint.append({**row, "issue": "texto vacio o minimo"})
            continue
        if q["mode"] == "REWRITE" and enders >= 2:
            manual.append({**row, "issue": "REWRITE con 2+ oraciones (posible dup estimulo+respuesta)"})
            continue
        if SUSPICIOUS.search(text):
            lint.append({**row, "issue": "caracteres sospechosos"})
            continue
        manifest.append(row)

    # duplicidad de claves contra TODO el curso (no solo el set)
    all_keys = {}
    for qid, q in head.items():
        all_keys.setdefault(q["key"], []).append(qid)
    global_dups = {k: v for k, v in all_keys.items() if len(v) > 1}

    json.dump(manifest, open(os.path.join(args.workdir, "regen_manifest.json"), "w"), indent=1, ensure_ascii=False)
    json.dump(manual, open(os.path.join(args.workdir, "manual_review.json"), "w"), indent=1, ensure_ascii=False)
    json.dump(lint, open(os.path.join(args.workdir, "lint_report.json"), "w"), indent=1, ensure_ascii=False)
    with open(os.path.join(args.workdir, "regen_manifest.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["key", "mode", "text"])
        for r in manifest:
            w.writerow([r["key"], r["mode"], r["text"]])

    fill_multi = sum(1 for r in manifest if r["mode"] == "FILL" and len(re.findall(r"[.?!]", r["text"])) >= 2)
    print(f"manifest: {len(manifest)} | revision manual: {len(manual)} | lints: {len(lint)}")
    print(f"FILL con 2+ oraciones (se locutan enteras, como el original): {fill_multi}")
    print(f"claves duplicadas en el curso entero: {len(global_dups)}")
    chars = sum(len(r["text"]) for r in manifest)
    print(f"caracteres a sintetizar: {chars}")


# ---------------------------------------------------------------- backup

def cmd_backup(args):
    dest = os.path.join(args.workdir, "s3_backup_audios_mp3")
    os.makedirs(dest, exist_ok=True)
    print(f"sync {S3_PREFIX}/ -> {dest}")
    r = subprocess.run(["aws", "s3", "sync", S3_PREFIX + "/", dest], capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[-2000:])
        raise SystemExit("backup fallo")
    n = len([f for f in os.listdir(dest) if f.endswith(".mp3")])
    total = sum(os.path.getsize(os.path.join(dest, f)) for f in os.listdir(dest))
    print(f"backup verificado en disco: {n} mp3, {total/1e6:.1f} MB")


# ---------------------------------------------------------------- synth

class Token:
    def __init__(self):
        self.value = None
        self.born = 0

    def get(self):
        if self.value is None or time.time() - self.born > 2400:
            self.value = subprocess.run(
                ["gcloud", "auth", "print-access-token"], capture_output=True, text=True, check=True
            ).stdout.strip()
            self.born = time.time()
        return self.value


def synth_one(row, wav_dir, mp3_dir, token):
    key, text = row["key"], row["text"]
    wav = os.path.join(wav_dir, key + ".wav")
    mp3 = os.path.join(mp3_dir, key + ".mp3")
    body = json.dumps(
        {"input": {"text": text}, "voice": VOICE, "audioConfig": {"audioEncoding": AUDIO_ENCODING}}
    ).encode()
    req = urllib.request.Request(
        TTS_URL,
        data=body,
        headers={
            "Content-Type": "application/json",
            "X-Goog-User-Project": GCLOUD_PROJECT,
            "Authorization": "Bearer " + token.get(),
        },
    )
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                payload = json.load(resp)
            break
        except Exception as e:
            if attempt == 3:
                return {"key": key, "ok": False, "error": str(e)[:200]}
            time.sleep(2**attempt)
    audio = base64.b64decode(payload["audioContent"])
    with open(wav, "wb") as f:
        f.write(audio)
    if os.path.getsize(wav) < 2000:
        return {"key": key, "ok": False, "error": "wav sospechosamente chico"}
    r = subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", wav, mp3], capture_output=True, text=True
    )
    if not os.path.exists(mp3) or os.path.getsize(mp3) < 1000:
        return {"key": key, "ok": False, "error": "mp3 no aparecio en disco: " + r.stderr[:150]}
    return {"key": key, "ok": True, "mp3_bytes": os.path.getsize(mp3)}


def cmd_synth(args):
    manifest = json.load(open(os.path.join(args.workdir, "regen_manifest.json")))
    if args.keys:
        wanted = set(args.keys.split(","))
        manifest = [r for r in manifest if r["key"] in wanted]
    if args.limit:
        manifest = manifest[: args.limit]
    wav_dir = os.path.join(args.workdir, "regen", "wav")
    mp3_dir = os.path.join(args.workdir, "regen", "mp3")
    os.makedirs(wav_dir, exist_ok=True)
    os.makedirs(mp3_dir, exist_ok=True)
    # no re-sintetizar lo que ya esta verificado en disco
    manifest = [r for r in manifest if not os.path.exists(os.path.join(mp3_dir, r["key"] + ".mp3"))]
    print(f"a sintetizar: {len(manifest)}")
    token = Token()
    token.get()
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as ex:
        futs = {ex.submit(synth_one, r, wav_dir, mp3_dir, token): r for r in manifest}
        done = 0
        for fut in concurrent.futures.as_completed(futs):
            results.append(fut.result())
            done += 1
            if done % 50 == 0:
                print(f"  {done}/{len(manifest)}", end="\r")
    bad = [r for r in results if not r["ok"]]
    print(f"\nsintetizados ok: {len(results)-len(bad)} | fallidos: {len(bad)}")
    for b in bad[:10]:
        print("  FALLO", b["key"], b["error"])
    json.dump(results, open(os.path.join(args.workdir, "synth_results.json"), "w"), indent=1)


# ---------------------------------------------------------------- upload / verify

def s3_listing():
    r = subprocess.run(
        ["aws", "s3", "ls", S3_PREFIX + "/"], capture_output=True, text=True, check=True
    )
    out = {}
    for line in r.stdout.splitlines():
        parts = line.split()
        if len(parts) >= 4 and parts[3].endswith(".mp3"):
            out[parts[3]] = {"date": parts[0], "time": parts[1], "size": int(parts[2])}
    return out


def cmd_upload(args):
    mp3_dir = os.path.join(args.workdir, "regen", "mp3")
    files = sorted(os.listdir(mp3_dir))
    if args.keys:
        wanted = {k + ".mp3" for k in args.keys.split(",")}
        files = [f for f in files if f in wanted]
    print(f"a subir: {len(files)}")

    def up(f):
        local = os.path.join(mp3_dir, f)
        r = subprocess.run(
            ["aws", "s3", "cp", local, f"{S3_PREFIX}/{f}", "--content-type", "audio/mp3"],
            capture_output=True,
            text=True,
        )
        return f, r.returncode, r.stderr[-200:] if r.returncode else ""

    failures = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=12) as ex:
        done = 0
        for f, rc, err in ex.map(up, files):
            if rc:
                failures.append((f, err))
            done += 1
            if done % 100 == 0:
                print(f"  {done}/{len(files)}", end="\r")
    print(f"\nsubidas con exit!=0: {len(failures)}")
    # verificacion real: listado fresco de S3, tamano exacto
    listing = s3_listing()
    bad = []
    for f in files:
        local_size = os.path.getsize(os.path.join(mp3_dir, f))
        s3 = listing.get(f)
        if s3 is None or s3["size"] != local_size:
            bad.append((f, local_size, s3))
    print(f"verificacion contra listado S3: {len(files)-len(bad)}/{len(files)} con tamano exacto")
    for b in bad[:10]:
        print("  MAL", b)
    if bad:
        raise SystemExit("hay claves sin verificar en S3")


def cmd_verify(args):
    manifest = json.load(open(os.path.join(args.workdir, "regen_manifest.json")))
    mp3_dir = os.path.join(args.workdir, "regen", "mp3")
    listing = s3_listing()
    today = time.strftime("%Y-%m-%d")
    ok, stale, size_mismatch, missing_local = 0, [], [], []
    for r in manifest:
        f = r["key"] + ".mp3"
        local = os.path.join(mp3_dir, f)
        s3 = listing.get(f)
        if not os.path.exists(local):
            missing_local.append(f)
            continue
        if s3 is None or s3["date"] != today:
            stale.append((f, s3 and s3["date"]))
        elif s3["size"] != os.path.getsize(local):
            size_mismatch.append(f)
        else:
            ok += 1
    print(f"manifest: {len(manifest)} | en S3 con fecha {today} y tamano exacto: {ok}")
    print(f"sin mp3 local: {len(missing_local)} | fecha vieja: {len(stale)} | tamano distinto: {len(size_mismatch)}")
    for s in stale[:10]:
        print("  VIEJA", s)


# ---------------------------------------------------------------- main

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--workdir", required=True, help="directorio de estado (scratchpad)")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("preflight")
    sub.add_parser("dryrun")
    sub.add_parser("backup")
    ps = sub.add_parser("synth")
    ps.add_argument("--keys", help="claves separadas por coma (piloto)")
    ps.add_argument("--limit", type=int)
    ps.add_argument("--workers", type=int, default=8)
    pu = sub.add_parser("upload")
    pu.add_argument("--keys", help="claves separadas por coma (piloto)")
    sub.add_parser("verify")
    args = p.parse_args()
    {"preflight": cmd_preflight, "dryrun": cmd_dryrun, "backup": cmd_backup,
     "synth": cmd_synth, "upload": cmd_upload, "verify": cmd_verify}[args.cmd](args)


if __name__ == "__main__":
    main()
