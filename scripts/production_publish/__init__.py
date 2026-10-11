"""Paso a producción de una tanda publicada (F-PUBPROD R006 a R009).

Lleva a la base de producción exactamente lo que dice la lista de publicación
que dejó `batch publish` / `tray publish` en
`<workdir>/.content-audit/batch-publications/<batchId>/manifest.json`, y nada más:

  - manifest_reader: lee la tanda y su lista, y niega todo lo dudoso ANTES de
    conectarse (tanda no publicada, huella distinta, veredicto que no es
    «pasa», pisos sin decidir, rechazados, rutas prohibidas, texto nulo...).
  - write_plan: compara cada campo con lo que hay en la base (comparar-y-fijar):
    APPLY (está el «antes»), ALREADY (ya está el «después») o DRIFT (otra cosa).
  - applier: escribe solo esos campos con el «después» de la lista y verifica
    cada campo y que el resto del documento quedó idéntico.
  - registry: el registro append-only de cada corrida, con el destino
    (servidor y base, nunca credenciales) y el respaldo de lo escrito.
  - store / mongo_store: el acceso a la base. Solo mongo_store importa pymongo,
    y de forma perezosa; las pruebas usan una colección falsa en memoria.
  - cli: el modo `--batch` de scripts/publish_revisions.py.

Todo es biblioteca estándar salvo mongo_store.
"""
