"""Lee el piloto de Didáctica A1: qué hace la versión nueva con los «revisar» y si cambia lo demás más que volver a
preguntarle a la vieja. Escribe resultado.json con las tablas y los casos enteros de cada desacuerdo."""
import collections, datetime, json, pathlib, re, sys

P = pathlib.Path(__file__).parent
CAMP = pathlib.Path("/Users/josecullen/projects/learney/content-audit/.content-audit/campaigns/a1-v11/.content-audit")
ESTADO = {"hoy": "correction", "base": "base"}
VERSION = {"nueva": "250b2141804036a842611e489d5d1f10e89093fa1559f87838f953c002bb263d",
           "vieja": "8398c4381d5e7053d85a75396144bbb562c08025d297edda964ce7a3cfc6fa78"}
viejos = json.loads((P / "viejos.json").read_text())


def ventanas():
    """Cada corrida, con su intervalo en UTC, sacado de piloto.log (hora local -03)."""
    v, abiertas = {}, {}
    lineas = (P / "piloto.log").read_text().splitlines()
    if (P / "otra-vez.log").exists(): lineas += (P / "otra-vez.log").read_text().splitlines()
    for l in lineas:
        m = re.match(r"(\d\d:\d\d:\d\d) (empieza|termina) (\w+) (\w+)", l)
        if not m:
            continue
        t = datetime.datetime.fromisoformat("2026-11-06T" + m[1]) + datetime.timedelta(hours=3)
        if m[2] == "empieza":
            abiertas[(m[3], m[4])] = t
        else:
            v[(m[3], m[4])] = (abiertas.pop((m[3], m[4])), t + datetime.timedelta(seconds=5))
    for k, t in abiertas.items():
        v[k] = (t, datetime.datetime(2100, 1, 1))
    return v


def ledger(version):
    f = P / f"wd-{version}/.content-audit/evaluations/exam_a1-didactic-judge.jsonl"
    rows = []
    if f.exists():
        for l in f.read_text().splitlines():
            r = json.loads(l)
            r["t"] = datetime.datetime.fromisoformat(r["recordedAt"].replace("Z", ""))
            rows.append(r)
    return rows


def ejercicios(foto):
    out = {}
    for f in (P / f"foto-{foto}").rglob("quizzes.json"):
        for q in json.loads(f.read_text()):
            partes = (q.get("form") or {}).get("sentenceParts") or []
            texto = " ".join("___" if p["kind"] == "CLOZE" else p["text"] for p in partes)
            opciones = [p.get("options") for p in partes if p["kind"] == "CLOZE"]
            items = (q.get("form") or {}).get("items") or []
            if items:  # opción múltiple: la correcta primero, después las otras
                opciones = [[i["label"] + (" (correcta)" if float((i.get("incidence") or {}).get("$numberDouble", i.get("incidence") or 0)) > 0 else "") for i in items]]
            out[q["id"]] = {"tema": q.get("topicName"), "titulo": q.get("title"), "consigna": q.get("instructions"),
                            "oracion": texto, "respuestas": opciones, "tipo": q.get("kind"),
                            "traduccion": q.get("translation"), "knowledgeId": (q.get("knowledgeId") or {}).get("$oid"),
                            "ruta": str(f.parent.relative_to(P / f"foto-{foto}"))}
    return out


win = ventanas()
res = {"corridas": {}, "tablas": {}, "casos": {}}
nuevos = {}
for version in ("nueva", "vieja", "nueva2"):
    filas = ledger(version)
    for foto in ("hoy", "base"):
        w = win.get((version, foto))
        if not w:
            continue
        dentro = [r for r in filas if w[0] <= r["t"] <= w[1]]
        # un ejercicio que no cambió entre la base y hoy reusa la consulta de la corrida anterior: vale la última hasta acá
        por = {}
        for r in sorted(filas, key=lambda r: r["t"]):
            if r["t"] <= w[1]:
                por[r["subjectRef"]] = r
        salida = P / f"salida-{version}-{foto}.jsonl"
        lineas = [json.loads(l) for l in salida.read_text().splitlines()] if salida.exists() else []
        res["corridas"][f"{version}-{foto}"] = {
            "consultas": len(dentro), "versiones": dict(collections.Counter(r["evaluatorVersion"][:8] for r in dentro)),
            "resultado": dict(collections.Counter(x["resultado"] for x in lineas))}
        nuevos[(version, foto)] = {q: json.loads(r["payload"]) for q, r in por.items()}

# las entradas son las mismas que lee el curso: la vieja, sobre el mismo ejercicio, recibe la misma huella
campana = collections.defaultdict(set)
for l in (CAMP / "evaluations/a1-didactic-judge.jsonl").read_text().splitlines():
    r = json.loads(l)
    if r.get("evaluatorVersion") == VERSION["vieja"]:
        campana[r["subjectRef"]].add(r["contentFingerprint"])
iguales = distintas = 0
for r in ledger("vieja"):
    if r["subjectRef"] in campana:
        if r["contentFingerprint"] in campana[r["subjectRef"]]:
            iguales += 1
        else:
            distintas += 1
res["entradas_vieja_vs_campana"] = {"misma_huella": iguales, "otra_huella": distintas}

for foto in ("hoy", "base"):
    casos = json.loads((P / f"casos-{foto}.json").read_text())
    ej = ejercicios(foto)
    antes = viejos[ESTADO[foto]]
    nueva = nuevos.get(("nueva", foto), {})
    vieja = nuevos.get(("vieja", foto), {})
    t = {}
    for grupo in ("REVIEW", "SUITABLE", "IMPROVE", "REWRITE_REQUIRED"):
        ids = [c["ejercicio"] for c in casos if c["grupo"] == grupo]
        t[grupo] = {"n": len(ids),
                    "nueva": dict(collections.Counter((nueva.get(q) or {}).get("status", "SIN") for q in ids)),
                    "vieja_otra_vez": dict(collections.Counter((vieja.get(q) or {}).get("status", "SIN") for q in ids))}
    ids = [c["ejercicio"] for c in casos if c["grupo"] != "REVIEW"]
    def acuerdo(otra):
        both = [q for q in ids if q in otra]
        return {"n": len(both), "igual": sum(otra[q]["status"] == antes[q]["status"] for q in both),
                "bloquea_igual": sum((otra[q]["status"] == "REWRITE_REQUIRED") == (antes[q]["status"] == "REWRITE_REQUIRED")
                                     for q in both)}
    t["control_nueva_vs_campana"] = acuerdo(nueva)
    t["control_vieja_vs_campana"] = acuerdo(vieja)
    res["tablas"][foto] = t
    lista = []
    for c in casos:
        q = c["ejercicio"]
        a, n, v = antes.get(q, {}), nueva.get(q), vieja.get(q)
        if not n:
            continue
        if n["status"] != a.get("status") or (v and v["status"] != a.get("status")):
            lista.append({"ejercicio": q, "grupo": c["grupo"], "ej": ej.get(q),
                          "campana": {k: a.get(k) for k in ("status", "issue", "reason", "proposedExample")},
                          "nueva": {k: n.get(k) for k in ("status", "issue", "reason", "proposedExample", "handoffs")},
                          "vieja_otra_vez": v and {k: v.get(k) for k in ("status", "issue", "reason")}})
    res["casos"][foto] = lista
    otra = nuevos.get(("nueva2", foto), {})
    todos = [c["ejercicio"] for c in casos if c["ejercicio"] in nueva and c["ejercicio"] in otra]
    res["tablas"][foto]["nueva_otra_vez"] = {"n": len(todos),
        "igual": sum(nueva[q]["status"] == otra[q]["status"] for q in todos),
        "bloquea_igual": sum((nueva[q]["status"] == "REWRITE_REQUIRED") == (otra[q]["status"] == "REWRITE_REQUIRED") for q in todos),
        "revisar": sum(otra[q]["status"] == "REVIEW" for q in todos),
        "difieren": [{"ejercicio": q, "grupo": next(c["grupo"] for c in casos if c["ejercicio"] == q), "nueva": nueva[q]["status"],
                      "otra": otra[q]["status"], "ej": ej.get(q), "razon_otra": otra[q].get("reason")}
                     for q in todos if nueva[q]["status"] != otra[q]["status"]]}
    # temas: los que tenían algún «revisar» y con la nueva no tienen ninguno entre los casos medidos
    temas = collections.defaultdict(lambda: [0, 0])
    for c in casos:
        if c["grupo"] == "REVIEW" and c["ejercicio"] in nueva:
            k = (ej.get(c["ejercicio"]) or {}).get("knowledgeId")
            temas[k][0] += 1
            temas[k][1] += nueva[c["ejercicio"]]["status"] == "REVIEW"
    res["tablas"][foto]["temas_con_revisar"] = len(temas)
    res["tablas"][foto]["temas_sin_revisar_con_la_nueva"] = sum(1 for x in temas.values() if x[1] == 0)

(P / "resultado.json").write_text(json.dumps(res, ensure_ascii=False, indent=1))
print(json.dumps({k: res[k] for k in ("corridas", "entradas_vieja_vs_campana", "tablas")}, ensure_ascii=False, indent=1))
print({f: len(v) for f, v in res["casos"].items()})
