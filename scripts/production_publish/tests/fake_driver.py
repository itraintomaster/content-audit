"""Un pymongo y un bson de juguete, en memoria, para probar los escritores sin red.

`FakeMongo.install()` los pone en sys.modules mientras dura la prueba: los
scripts los importan adentro de la función que conecta, así que reciben estos.
Entienden lo poco que usan los escritores: find_one, find, update_one,
bulk_write con UpdateOne, list_collection_names, y en los filtros la igualdad
por ruta con puntos, `$exists` y `$in`.
"""

import copy
import importlib.util
import json
import os
import re
import sys
import types
from unittest import mock

SCRIPTS = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HEX = re.compile(r"[0-9a-f]{24}\Z")


class ObjectId:
    def __init__(self, value):
        value = str(value)
        if not HEX.match(value):
            raise ValueError("ObjectId inválido")
        self._hex = value

    def __eq__(self, other):
        return isinstance(other, ObjectId) and other._hex == self._hex

    def __hash__(self):
        return hash(("ObjectId", self._hex))

    def __str__(self):
        return self._hex

    def __repr__(self):
        return "ObjectId(%r)" % self._hex


def _object_hook(d):
    if set(d) == {"$oid"}:
        return ObjectId(d["$oid"])
    return d


def _default(value):
    if isinstance(value, ObjectId):
        return {"$oid": str(value)}
    return str(value)


def json_loads(text, **_):
    return json.loads(text, object_hook=_object_hook)


def json_dumps(value, **kwargs):
    return json.dumps(value, default=_default, **kwargs)


# Las excepciones del driver, con el módulo de pymongo (así se reconocen como de la base).
ServerSelectionTimeoutError = type("ServerSelectionTimeoutError", (Exception,), {"__module__": "pymongo.errors"})
OperationFailure = type("OperationFailure", (Exception,), {"__module__": "pymongo.errors"})

_MISSING = object()


def _get(doc, path):
    value = doc
    for key in path.split("."):
        if not isinstance(value, dict) or key not in value:
            return _MISSING
        value = value[key]
    return value


def _set(doc, path, value):
    keys = path.split(".")
    for key in keys[:-1]:
        doc = doc.setdefault(key, {})
    doc[keys[-1]] = copy.deepcopy(value)


def _matches(doc, query):
    for path, condition in (query or {}).items():
        value = _get(doc, path)
        if isinstance(condition, dict) and "$exists" in condition:
            if (value is not _MISSING) != bool(condition["$exists"]):
                return False
        elif isinstance(condition, dict) and "$in" in condition:
            if value is _MISSING or value not in condition["$in"]:
                return False
        elif value is _MISSING or value != condition:
            return False
    return True


class UpdateOne:
    def __init__(self, filter, update, upsert=False):
        self.filter, self.update, self.upsert = filter, update, upsert


class _Result:
    def __init__(self, matched, modified):
        self.matched_count, self.modified_count = matched, modified


class FakeCollection:
    def __init__(self, mongo, db, name):
        self.mongo, self.db, self.name = mongo, db, name

    def _docs(self):
        return self.mongo.data.get(self.db, {}).get(self.name, [])

    def find_one(self, query=None, projection=None):
        self.mongo.reads += 1
        for doc in self._docs():
            if _matches(doc, query):
                return copy.deepcopy(doc)
        return None

    def find(self, query=None, projection=None):
        self.mongo.reads += 1
        return [copy.deepcopy(d) for d in self._docs() if _matches(d, query)]

    def update_one(self, query, update, upsert=False):
        if upsert:
            raise AssertionError("un escritor pidió upsert")
        if set(update) != {"$set"}:
            raise AssertionError("un escritor usó algo que no es $set: %s" % sorted(update))
        self.mongo.on_write(self.name, query, update)
        for doc in self._docs():
            if _matches(doc, query):
                for path, value in update["$set"].items():
                    _set(doc, path, value)
                self.mongo.writes.append((self.name, copy.deepcopy(query), copy.deepcopy(update)))
                if self.mongo.after_write:
                    self.mongo.after_write(self.name, doc)
                return _Result(1, 1)
        self.mongo.writes.append((self.name, copy.deepcopy(query), copy.deepcopy(update)))
        return _Result(0, 0)

    def bulk_write(self, operations, ordered=True):
        matched = modified = 0
        for op in operations:
            result = self.update_one(op.filter, op.update, op.upsert)
            matched += result.matched_count
            modified += result.modified_count
        return _Result(matched, modified)


class FakeDatabase:
    def __init__(self, mongo, name):
        self.mongo, self.name = mongo, name

    def list_collection_names(self):
        return sorted(self.mongo.data.get(self.name, {}))

    def __getitem__(self, collection):
        return FakeCollection(self.mongo, self.name, collection)


class FakeClient:
    def __init__(self, mongo):
        self.mongo = mongo
        self.closed = False

    def __getitem__(self, db):
        return FakeDatabase(self.mongo, db)

    def close(self):
        self.closed = True


class FakeMongo:
    """El estado de la base de juguete: datos, conexiones pedidas y escrituras."""

    def __init__(self, data=None):
        self.data = data if data is not None else {}
        self.clients = []  # (uri, opciones) de cada MongoClient pedido
        self.writes = []
        self.reads = 0
        self.fail_connect = None  # una excepción para tirar al conectarse
        self.after_write = None  # (colección, documento ya escrito) -> None: para estropearlo
        self.write_hooks = []  # (colección, filtro, update) -> None: corre antes de cada escritura

    def on_write(self, collection, query, update):
        for hook in self.write_hooks:
            hook(collection, query, update)

    def collection(self, db, name):
        return self.data.setdefault(db, {}).setdefault(name, [])

    def find(self, db, name, doc_id):
        return next(d for d in self.collection(db, name) if str(d["_id"]) == str(doc_id))

    def client(self, uri, **options):
        self.clients.append((uri, options))
        if self.fail_connect is not None:
            raise self.fail_connect
        return FakeClient(self)

    def modules(self):
        pymongo = types.ModuleType("pymongo")
        errors = types.ModuleType("pymongo.errors")
        errors.ServerSelectionTimeoutError = ServerSelectionTimeoutError
        errors.OperationFailure = OperationFailure
        pymongo.MongoClient = self.client
        pymongo.UpdateOne = UpdateOne
        pymongo.errors = errors
        bson = types.ModuleType("bson")
        json_util = types.ModuleType("bson.json_util")
        json_util.loads = json_loads
        json_util.dumps = json_dumps
        bson.ObjectId = ObjectId
        bson.json_util = json_util
        return {"pymongo": pymongo, "pymongo.errors": errors, "bson": bson, "bson.json_util": json_util}

    def install(self, test):
        patcher = mock.patch.dict(sys.modules, self.modules())
        patcher.start()
        test.addCleanup(patcher.stop)


def load_script(name):
    """Importa scripts/<name>.py con un nombre propio (no se cachea entre pruebas)."""
    spec = importlib.util.spec_from_file_location("bajo_prueba_" + name, os.path.join(SCRIPTS, name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module
