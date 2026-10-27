# Imprime para leer a mano las marcas de marcas-c2.json (leer_marcas.py): cada ejercicio con el enunciado, los campos
# que cambiaron (producción → ahora) y los problemas que vio el revisor. Gratis: no consulta.
# Uso: ver_marcas.py <marca|muestra> [desde] [cuántos]   (marca: mal, dudoso, heredado, bien)
import json
import sys
from pathlib import Path

AQUI = Path(__file__).parent
EJERCICIO = 'Ejercicio, como lo ve el alumno antes de contestar'


def campos(texto):
    salida, clave = {}, None
    for renglon in (texto or '').splitlines():
        if ': ' in renglon and not renglon.startswith(' '):
            clave, valor = renglon.split(': ', 1)
            salida[clave] = valor
        elif clave:
            salida[clave] += '\n' + renglon
    return salida


def ver(f):
    antes, ahora = campos(f['produccion']), campos(f['correccion'])
    cabeza = f"### {f['id']} · {f['tema']} · {f['marca']}{' · TRAMO GRAVE' if f['grave'] else ''}"
    renglones = [cabeza]
    if antes.get(EJERCICIO) == ahora.get(EJERCICIO):
        renglones.append(f"  Ejercicio (igual): {ahora.get(EJERCICIO)}")
    for clave in dict.fromkeys(list(antes) + list(ahora)):
        if antes.get(clave) != ahora.get(clave):
            corta = clave.split(',')[0]
            renglones.append(f"  {corta}:\n    PROD  {antes.get(clave)}\n    AHORA {ahora.get(clave)}")
    for p in f['problemas']:
        renglones.append(f"  - [{p.get('where')}{', ya estaba' if p.get('inherited') else ''}] «{p.get('fragment')}» {p.get('what')}")
    if f['marca'] == 'bien' or not f['problemas']:
        renglones.append(f"  Razón: {f['razon']}")
    return '\n'.join(renglones)


if __name__ == '__main__':
    filas = json.loads((AQUI / 'marcas-c2.json').read_text())['filas']
    cual = sys.argv[1]
    elegidas = [f for f in filas if (f['muestra'] if cual == 'muestra' else f['marca'] == cual)]
    desde = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    hasta = desde + int(sys.argv[3]) if len(sys.argv) > 3 else len(elegidas)
    print(f'{len(elegidas)} con «{cual}»; muestro {desde}–{min(hasta, len(elegidas))}\n')
    for f in elegidas[desde:hasta]:
        print(ver(f) + '\n')
