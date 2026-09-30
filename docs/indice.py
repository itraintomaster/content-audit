"""
indice.py — arma docs/index.html, la matriz de documentos del proyecto.

Lee los dos tipos que un proyecto acumula y los cataloga en una sola página:

    docs/propuestas/<slug>.html    documentos VIVOS: se corrigen en el mismo archivo
    docs/briefs/NNN-slug.html      instantáneas FECHADAS: se emiten y no se tocan

Se regenera, no se edita a mano: el titular, la bajada y la fecha salen de cada
archivo, así que el índice no puede quedar desactualizado. Correrlo después de
escribir o corregir cualquier documento.

    python3 docs/indice.py

La convención completa está en ~/.claude/skills/visual-brief/assets/documentos.md

Lo ÚNICO que se toca a mano acá es TEMAS, abajo: los temas de un proyecto no son
los de otro.
"""

import glob
import html
import os
import re
import subprocess

AQUI = os.path.dirname(os.path.abspath(__file__))
PROYECTO = os.path.basename(os.path.dirname(AQUI))

# ── lo único por proyecto ──────────────────────────────────────────────────────
# (patrón que se busca en el <span class="meta"> del brief, nombre del grupo).
# El primero que matchea gana, así que van de más específico a más general.
TEMAS = [
    (r"base del curso|backup|línea base", "Base del curso y línea base"),
    (r"consigna|qicor|juez", "Consignas"),
    (r"audio", "Audios"),
    (r"", "Otros"),
]
# Ejemplo real, del proyecto impostor:
#   TEMAS = [
#       (r"caja",                    "Caja After Eight"),
#       (r"bandeja|tablero|6×6|6x6", "Tablero 6×6"),
#       (r"scribus|motor|app",       "App de tarjetas"),
#       (r"blender-agent",           "La herramienta"),
#   ]
# ───────────────────────────────────────────────────────────────────────────────


def limpiar(s):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s)).strip()


def primero(s, *patrones):
    for p in patrones:
        m = re.search(p, s, re.S | re.I)
        if m:
            t = limpiar(m.group(1))
            if t:
                return t
    return ""


def fecha_de_git(ruta):
    """La fecha del último commit que tocó el archivo. Es mejor que el mtime, que
    se pierde en un clone. Si no hay git, cae en el mtime."""
    try:
        r = subprocess.run(["git", "log", "-1", "--format=%cs", "--", ruta],
                           cwd=os.path.dirname(os.path.abspath(ruta)),
                           capture_output=True, text=True, timeout=5)
        if r.returncode == 0 and r.stdout.strip():
            return r.stdout.strip()
    except Exception:
        pass
    import datetime
    return datetime.date.fromtimestamp(os.path.getmtime(ruta)).isoformat()


def leer_brief(ruta):
    s = open(ruta, encoding="utf-8").read()
    meta = primero(s, r'<span class="meta">(.*?)</span>')
    tema = "Todos"
    for patron, nombre in TEMAS:
        if not patron or re.search(patron, meta, re.I):
            tema = nombre
            break
    m = re.search(r"(\d{1,2} \w+ \d{4})", meta)
    return {
        "tipo": "brief",
        "archivo": "briefs/" + os.path.basename(ruta),
        "clave": os.path.basename(ruta)[:3],
        "titulo": primero(s, r"<h1>(.*?)</h1>", r"<title>(.*?)</title>") or "(sin título)",
        "bajada": primero(s, r'<p class="sub">(.*?)</p>'),
        "fecha": m.group(1) if m else fecha_de_git(ruta),
        "tema": tema,
    }


def leer_propuesta(ruta):
    s = open(ruta, encoding="utf-8").read()
    titulo = primero(s,
                     r'class="hero"[^>]*>.*?<h1[^>]*>(.*?)</h1>',
                     r"<h1[^>]*>(.*?)</h1>",
                     r"<title>(.*?)</title>")
    titulo = re.sub(r"\s*·\s*Propuesta\s*$", "", titulo)
    return {
        "tipo": "propuesta",
        "archivo": "propuestas/" + os.path.basename(ruta),
        "clave": os.path.splitext(os.path.basename(ruta))[0],
        "titulo": titulo or "(sin título)",
        "bajada": primero(s,
                          r'class="hero"[^>]*>.*?class="tag"[^>]*>(.*?)</',
                          r'class="tag"[^>]*>(.*?)</',
                          r'<p class="sub">(.*?)</p>'),
        "fecha": fecha_de_git(ruta),
        "tema": None,
    }


CSS = """
:root{--bg:#fbfaf8;--bg-elev:#fff;--line:#e6e2db;--line-soft:#f0ece5;--fg:#1c1a17;
  --fg-2:#57524b;--fg-3:#8b847a;--accent:#b4531f;--accent-soft:#fdf1e7;
  --ok:#2f6b46;--ok-soft:#e9f4ed;--radius:10px}
@media (prefers-color-scheme:dark){:root{--bg:#131211;--bg-elev:#1b1a18;--line:#302d29;
  --line-soft:#252320;--fg:#efece7;--fg-2:#a8a29a;--fg-3:#7b756c;--accent:#e08b4f;
  --accent-soft:#2a1f16;--ok:#7fc79c;--ok-soft:#16241c}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);padding:44px 24px 80px;
  font:16px/1.6 ui-sans-serif,-apple-system,"SF Pro Text","Segoe UI",system-ui,sans-serif}
.wrap{max-width:920px;margin:0 auto}
h1{font-size:33px;letter-spacing:-.02em;margin:0 0 6px}
.sub{color:var(--fg-2);margin:0 0 26px;font-size:16.5px}
h2{font-size:22px;letter-spacing:-.01em;margin:38px 0 4px}
h2 .q{font-size:13px;font-weight:400;color:var(--fg-3);margin-left:8px}
.que{font-size:14px;color:var(--fg-2);margin:0 0 14px}
h3{font-size:12.5px;text-transform:uppercase;letter-spacing:.08em;color:var(--fg-3);
  margin:24px 0 9px;font-weight:600}
a.d{display:grid;grid-template-columns:auto 1fr auto;gap:14px;align-items:start;
  text-decoration:none;color:inherit;background:var(--bg-elev);border:1px solid var(--line);
  border-radius:var(--radius);padding:13px 15px;margin-bottom:8px}
a.d:hover{border-color:var(--accent)}
a.d.p{border-left:3px solid var(--accent)}
.k{font:600 12.5px/1.5 ui-monospace,monospace;color:var(--fg-3);
  background:var(--line-soft);border-radius:6px;padding:2px 7px;white-space:nowrap}
.t{display:block;font-weight:600;font-size:15.5px;letter-spacing:-.01em}
.b{display:block;font-size:13.5px;color:var(--fg-2);margin-top:2px}
.f{font-size:12px;color:var(--fg-3);white-space:nowrap;padding-top:3px}
.nota{font-size:13.5px;color:var(--fg-2);background:var(--bg-elev);border:1px solid var(--line);
  border-left:3px solid var(--accent);border-radius:var(--radius);padding:12px 14px;margin:0 0 8px}
.vacio{font-size:14px;color:var(--fg-3);font-style:italic;padding:4px 0 8px}
footer{margin-top:44px;padding-top:16px;border-top:1px solid var(--line);
  font-size:12.5px;color:var(--fg-3)}
code{font-family:ui-monospace,monospace;font-size:12.5px;background:var(--line-soft);
  padding:1px 5px;border-radius:4px}
"""


def fila(d):
    cls = "d p" if d["tipo"] == "propuesta" else "d"
    return (f'<a class="{cls}" href="{d["archivo"]}">'
            f'<span class="k">{html.escape(d["clave"])}</span>'
            f'<span><span class="t">{html.escape(d["titulo"])}</span>'
            f'<span class="b">{html.escape(d["bajada"])}</span></span>'
            f'<span class="f">{html.escape(d["fecha"])}</span></a>')


def main():
    props = [leer_propuesta(p) for p in sorted(glob.glob(os.path.join(AQUI, "propuestas", "*.html")))]
    props.sort(key=lambda d: d["fecha"], reverse=True)
    briefs = [leer_brief(p) for p in
              sorted(glob.glob(os.path.join(AQUI, "briefs", "[0-9][0-9][0-9]-*.html")))]

    partes = []
    partes.append(f'<h2>Propuestas <span class="q">{len(props)}</span></h2>')
    partes.append('<p class="que">Documentos vivos: se corrigen en el mismo archivo. '
                  'Ordenados por última revisión.</p>')
    partes.append("\n".join(fila(d) for d in props) if props else
                  '<p class="vacio">Todavía no hay ninguna.</p>')

    partes.append(f'<h2>Briefs <span class="q">{len(briefs)}</span></h2>')
    partes.append('<p class="que">Instantáneas fechadas: se emiten y no se tocan más. '
                  'Si algo cambió, hay un brief más nuevo que lo dice.</p>')
    if briefs:
        por_tema = {}
        for b in briefs:
            por_tema.setdefault(b["tema"], []).append(b)
        orden = [n for _, n in TEMAS] + [t for t in por_tema if t not in [n for _, n in TEMAS]]
        for tema in orden:
            if tema not in por_tema:
                continue
            if len(por_tema) > 1:
                partes.append(f"<h3>{html.escape(tema)}</h3>")
            for b in sorted(por_tema[tema], key=lambda x: x["clave"]):
                partes.append(fila(b))
    else:
        partes.append('<p class="vacio">Todavía no hay ninguno.</p>')

    doc = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Documentos · {html.escape(PROYECTO)}</title><style>{CSS}</style></head>
<body><div class="wrap">
<h1>Documentos del proyecto</h1>
<p class="sub">{len(props)} propuesta{"s" if len(props) != 1 else ""} y
{len(briefs)} brief{"s" if len(briefs) != 1 else ""}. Cada uno es una página que se
abre sola en el browser.</p>
<div class="nota"><strong>Esta página se regenera.</strong> El titular, la bajada y la
fecha de cada fila salen del archivo: corré <code>python3 docs/indice.py</code> después
de escribir o corregir cualquier documento y aparece acá. No editar
<code>index.html</code> a mano.</div>
{chr(10).join(partes)}
<footer>docs/ · el brief más nuevo también está en <code>briefs/brief.html</code>,
que es una URL fija para el browser</footer>
</div></body></html>
"""
    ruta = os.path.join(AQUI, "index.html")
    open(ruta, "w", encoding="utf-8").write(doc)
    print(f"  {len(props)} propuestas + {len(briefs)} briefs -> {os.path.relpath(ruta)}")
    return doc


if __name__ == "__main__":
    main()
