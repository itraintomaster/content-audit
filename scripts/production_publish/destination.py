"""El destino de una corrida: {host, db}, el servidor y la base (F-PUBPROD R008 y R009).

Nunca usuario, clave ni el URI, que puede traer credenciales en el usuario o en sus
opciones. Los mensajes de error tampoco repiten el URI.
"""

import re
from urllib.parse import urlsplit

URI_SCHEMES = ("mongodb", "mongodb+srv")
# Un servidor: nombre o [IPv6], con puerto numérico opcional. Nada más.
HOST = re.compile(r"(\[[0-9a-f:.]+\]|[a-z0-9._%-]+)(:[0-9]{1,5})?\Z")


def _normalized_hosts(text):
    """Los servidores separados por coma, en minúsculas; None si no hay o alguno no tiene forma de servidor."""
    names = [h.strip().lower() for h in text.split(",") if h.strip()]
    if not names or not all(HOST.match(n) for n in names):
        return None
    return ",".join(names)


def destination_of(uri, db):
    """{host, db} del URI: solo los servidores (con puerto si lo trae) y la base.

    Lanza ValueError con un mensaje que nunca repite el URI.
    """
    if not isinstance(uri, str) or not uri:
        raise ValueError("falta el URI de la base")
    try:
        parts = urlsplit(uri)
    except ValueError:
        raise ValueError("el URI de la base no se puede leer")
    if parts.scheme not in URI_SCHEMES:
        raise ValueError("el URI de la base tiene que empezar con mongodb:// o mongodb+srv://")
    # Una @ después de los servidores es una clave con caracteres sin escapar: lo que
    # urlsplit llamaría servidor podría ser parte de la clave. No se registra nada.
    if "@" in parts.path or "@" in parts.query or "@" in parts.fragment:
        raise ValueError("el URI de la base está mal formado (¿la clave tiene caracteres sin escapar?)")
    host = _normalized_hosts(parts.netloc.rpartition("@")[2])
    if host is None:
        raise ValueError("el URI de la base no dice a qué servidor conectarse")
    if not isinstance(db, str) or not db:
        raise ValueError("falta el nombre de la base")
    return {"host": host, "db": db}


def destination_label(destination):
    return "%s/%s" % (destination["host"], destination["db"]) if destination else "(sin destino)"


def parse_expected_destination(text):
    """`host/db` tal como lo escribe José en --expect-destination."""
    if not isinstance(text, str) or "/" not in text:
        raise ValueError("--expect-destination tiene que ser servidor/base, por ejemplo "
                         "cluster-ejemplo.mongodb.net/learney")
    host, _, db = text.rpartition("/")
    host = _normalized_hosts(host)
    if host is None or not db or ":" in db or "@" in db:
        raise ValueError("--expect-destination tiene que ser servidor/base, sin usuario ni clave")
    return {"host": host, "db": db}
