"""La base real: el único módulo que importa pymongo y bson, y recién al conectarse.

Solo lee documentos y fija campos con `$set` filtrando por `_id` y por el valor
que se leyó de cada campo (comparar-y-fijar atómico por documento). Nunca
inserta, reemplaza ni borra, y nunca hace upsert.
"""

import re

from .store import MISSING, Store
from .write_plan import INSTANCES

HEX_ID = re.compile(r"[0-9a-f]{24}\Z")


class MongoStore(Store):
    def __init__(self, client, db, object_id_type):
        self._client = client
        self._db = db
        self._object_id = object_id_type
        # El _id tal como está en la base, por (colección, id en texto): así se
        # escribe sobre el mismo documento que se leyó, sea ObjectId o texto.
        self._raw_ids = {}

    def _raw_id(self, collection, doc_id):
        key = (collection, doc_id)
        if key in self._raw_ids:
            return self._raw_ids[key]
        return self._object_id(doc_id) if HEX_ID.match(doc_id) else doc_id

    def collections(self):
        return list(self._db.list_collection_names())

    def find_document(self, collection, doc_id):
        doc = self._db[collection].find_one({"_id": self._raw_id(collection, doc_id)})
        if doc is not None:
            self._raw_ids[(collection, doc_id)] = doc["_id"]
        return doc

    def find_instances(self, template_ids):
        ids = list(template_ids)
        # En las instancias el template._id embebido viene como texto (a veces ObjectId).
        values = ids + [self._object_id(i) for i in ids if HEX_ID.match(i)]
        found = []
        for doc in self._db[INSTANCES].find({"template._id": {"$in": values}}):
            instance_id = str(doc["_id"])
            self._raw_ids[(INSTANCES, instance_id)] = doc["_id"]
            found.append((instance_id, doc))
        return found

    def compare_and_set(self, collection, doc_id, expected, sets):
        query = {"_id": self._raw_id(collection, doc_id)}
        for path, value in expected.items():
            query[path] = {"$exists": False} if value is MISSING else value
        result = self._db[collection].update_one(query, {"$set": dict(sets)}, upsert=False)
        return result.matched_count == 1

    def close(self):
        self._client.close()


def open_store(uri, db_name):
    """Abre la conexión. Las excepciones de pymongo se muestran solo por su clase."""
    import pymongo  # perezoso: nada más del paso a producción necesita pymongo
    from bson import ObjectId

    client = pymongo.MongoClient(uri, serverSelectionTimeoutMS=10000,
                                 appname="content-audit-production-publish")
    return MongoStore(client, client[db_name], ObjectId)
