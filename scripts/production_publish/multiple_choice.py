"""Una opción múltiple bien armada, como la guarda producción (F-PUBPROD-R002 inv. 4).

Un solo hueco, sin respuestas escritas; de dos a ocho opciones, cada una con texto e
identificador, sin repetir; una sola correcta, y elección de una sola opción. Así están las
2.810 opciones múltiples de A1 en producción. Cada función devuelve qué le falta, o None.
Es la misma regla que aplica el lado Java al armar la lista.
"""

FEWEST, MOST = 2, 8
SINGLE = "SINGLE"
GAP = "CLOZE"


def normalized(label):
    return label.strip().replace("’", "'").casefold()


def gap_problem(parts):
    """El enunciado: un solo hueco, sin respuestas escritas."""
    if not isinstance(parts, list):
        return "no tiene enunciado"
    gaps = [p for p in parts if isinstance(p, dict) and p.get("kind") == GAP]
    if len(gaps) != 1:
        return "tiene %d %s; lleva uno" % (len(gaps), "hueco" if len(gaps) == 1 else "huecos")
    if gaps[0].get("options"):
        return "el hueco trae respuestas escritas"
    return None


def selection_problem(selection):
    if selection != SINGLE:
        return "elige más de una (%s); producción elige una sola" % (selection,)
    return None


def _item_problem(item, ids, labels):
    label = item.get("label") if isinstance(item, dict) else None
    if not isinstance(label, str) or not label.strip():
        return "una opción no tiene texto"
    item_id = item.get("id")
    if not isinstance(item_id, str) or not item_id.strip() or item_id in ids:
        return "la opción «%s» no tiene un identificador propio" % label.strip()
    if normalized(label) in labels:
        return "repite la opción «%s»" % label.strip()
    incidence = item.get("incidence")
    if type(incidence) not in (int, float) or incidence not in (0, 1):
        return "la opción «%s» vale %r; cada una vale 0 o 1" % (label.strip(), incidence)
    ids.add(item_id)
    labels.add(normalized(label))
    return None


def items_problem(items):
    """Las opciones: de dos a ocho, cada una con texto e identificador propios, una sola correcta."""
    if not isinstance(items, list):
        return "no tiene opciones"
    if not FEWEST <= len(items) <= MOST:
        return "tiene %d %s; van de dos a ocho" % (len(items), "opción" if len(items) == 1 else "opciones")
    ids, labels = set(), set()
    for item in items:
        problem = _item_problem(item, ids, labels)
        if problem:
            return problem
    correct = sum(1 for item in items if item["incidence"] == 1)
    if correct != 1:
        return "tiene %d %s; lleva una" % (correct, "correcta" if correct == 1 else "correctas")
    return None


def problem(form):
    """Lo que le falta a un formulario entero para ser una opción múltiple bien armada."""
    return (gap_problem(form.get("sentenceParts")) or selection_problem(form.get("selection"))
            or items_problem(form.get("items")))
