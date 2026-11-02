import json, re, sys, urllib.request
BASE = 'http://127.0.0.1:7800'
C2 = 'tanda-A1-c2-1f1d898f-3ba9-3fcc-bc91-a104f79e53ca'
def sesion():
    html = urllib.request.urlopen(f'{BASE}/?tanda={C2}', timeout=60).read().decode()
    return re.search(r'<meta name="sesion" content="([^"]+)"', html).group(1)
def pedir(metodo, ruta, cuerpo=None, token=None, timeout=900):
    data = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=data, method=metodo, headers={'Content-Type': 'application/json', 'Origin': BASE, 'X-Sesion': token or sesion()})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r: return r.status, json.loads(r.read().decode() or 'null')
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read().decode() or 'null')
