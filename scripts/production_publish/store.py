"""El acceso a los documentos de producción y las comparaciones entre valores.

Este módulo no importa pymongo ni bson. Define:

  - `Store`: lo único que el paso a producción le pide a la base. La
    implementación real es `mongo_store.MongoStore`; las pruebas usan una
    colección falsa en memoria.
  - `MISSING`: el valor de un campo que el documento no tiene (distinto de null).
  - `same_value`: igualdad estricta, la que decide APPLY / ALREADY / DRIFT. No
    confunde tipos (True no es 1, 1 no es 1.0, "" no es null, ausente no es
    null); el orden de las claves de un subdocumento no importa, el de los
    elementos de una lista sí.
  - `fingerprint_without`: la huella de un documento sin las rutas escritas,
    para verificar que lo demás quedó idéntico.
"""

import copy
import hashlib
import json
from collections.abc import Mapping


class _Missing:
    """El campo no existe en el documento. Es único y sobrevive a copias."""

    __slots__ = ()

    def __repr__(self):
        return "<ausente>"

    def __copy__(self):
        return self

    def __deepcopy__(self, memo):
        return self

    def __bool__(self):
        return False


MISSING = _Missing()


def get_path(doc, path):
    """Valor de `path` (con puntos, sin índices de lista) o MISSING."""
    current = doc
    for key in path.split("."):
        if not isinstance(current, Mapping) or key not in current:
            return MISSING
        current = current[key]
    return current


def remove_path(doc, path):
    """Saca `path` y los subdocumentos que quedan vacíos en ese camino."""
    keys = path.split(".")
    chain = [doc]
    current = doc
    for key in keys[:-1]:
        nxt = current.get(key) if isinstance(current, dict) else None
        if not isinstance(nxt, dict):
            return
        chain.append(nxt)
        current = nxt
    current.pop(keys[-1], None)
    for depth in range(len(keys) - 1, 0, -1):
        if chain[depth]:
            break
        chain[depth - 1].pop(keys[depth - 1], None)


def _kind(value):
    if value is MISSING:
        return "missing"
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "bool"
    if isinstance(value, int):
        return "int"
    if isinstance(value, float):
        return "float"
    if isinstance(value, str):
        return "str"
    if isinstance(value, Mapping):
        return "dict"
    if isinstance(value, (list, tuple)):
        return "list"
    return "other:" + type(value).__name__


def same_value(a, b):
    """Igualdad estricta entre dos valores de documento (ver el docstring del módulo)."""
    kind_a, kind_b = _kind(a), _kind(b)
    if kind_a != kind_b:
        return False
    if kind_a in ("missing", "null"):
        return True
    if kind_a == "dict":
        if set(a.keys()) != set(b.keys()):
            return False
        return all(same_value(a[k], b[k]) for k in a)
    if kind_a == "list":
        return len(a) == len(b) and all(same_value(x, y) for x, y in zip(a, b))
    return a == b


def _canonical(value):
    kind = _kind(value)
    if kind in ("missing", "null"):
        return [kind]
    if kind == "bool":
        return ["bool", value]
    if kind == "int":
        return ["int", type(value).__name__, int(value)]
    if kind == "float":
        return ["float", repr(value)]
    if kind == "str":
        return ["str", value]
    if kind == "dict":
        return ["dict", [[str(k), _canonical(value[k])] for k in sorted(value, key=str)]]
    if kind == "list":
        return ["list", [_canonical(x) for x in value]]
    if isinstance(value, (bytes, bytearray)):
        return ["bytes", bytes(value).hex()]
    # ObjectId, datetime, Decimal128, Timestamp...: su clase y su forma de texto.
    return [kind, str(value)]


def fingerprint(doc):
    text = json.dumps(_canonical(doc), ensure_ascii=False, separators=(",", ":"))
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def fingerprint_without(doc, paths):
    """Huella del documento sin `paths`: si cambia, cambió algo que no se escribió."""
    rest = copy.deepcopy(doc)
    for path in paths:
        remove_path(rest, path)
    return fingerprint(rest)


def short(value, limit=90):
    """Resumen legible de un valor para la salida (nunca para decidir nada)."""
    if value is MISSING:
        return "<ausente>"
    try:
        text = json.dumps(value, ensure_ascii=False, default=str)
    except (TypeError, ValueError):
        text = repr(value)
    return text if len(text) <= limit else text[: limit - 1] + "…"


def to_json_safe(value):
    """Convierte un valor leído de la base a algo que json.dumps acepta (para el respaldo)."""
    if value is MISSING:
        return {"$ausente": True}
    kind = _kind(value)
    if kind in ("null", "bool", "int", "float", "str"):
        return int(value) if kind == "int" else value
    if kind == "dict":
        return {str(k): to_json_safe(v) for k, v in value.items()}
    if kind == "list":
        return [to_json_safe(x) for x in value]
    if isinstance(value, (bytes, bytearray)):
        return {"$tipo": "bytes", "valor": bytes(value).hex()}
    return {"$tipo": type(value).__name__, "valor": str(value)}


class Store:
    """Lo que el paso a producción necesita de la base. Los ids son siempre strings.

    Ninguna operación inserta ni reemplaza documentos: solo se lee y se fija
    campo por campo, y solo si lo que hay sigue siendo lo que se leyó.
    """

    def collections(self):
        """Nombres de las colecciones que existen en el destino."""
        raise NotImplementedError

    def find_document(self, collection, doc_id):
        """El documento completo, o None si no existe."""
        raise NotImplementedError

    def find_instances(self, template_ids):
        """Instancias de la colección `quizzes` que embeben alguno de esos templates.

        Devuelve una lista de (id de la instancia, documento completo).
        """
        raise NotImplementedError

    def compare_and_set(self, collection, doc_id, expected, sets):
        """Fija `sets` (ruta -> valor) solo si cada ruta de `expected` sigue con ese valor.

        `expected` lleva los valores tal como se leyeron (MISSING si el campo no
        existía). Devuelve True si fijó, False si el documento cambió entretanto.
        """
        raise NotImplementedError

    def close(self):
        pass
