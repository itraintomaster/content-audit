# Mi lectura de las marcas del revisor (versión cb2111fe) sobre la C2 entera, 27/10/2026: cada ejercicio marcado,
# leído con los dos lados (ver_marcas.py). Una referencia, no una verdad certificada.
# veredicto: real (el problema está y pesa), leve (está, pero no frena), falsa (no hay problema), decidir (depende de
# una decisión de José). causa: la familia del problema, para contar y mostrar un caso entero por familia.

CAUSAS = {
    'TEMA': 'Practica otra cosa que el tema',
    'PISTA': 'Una pista o la consigna le da la respuesta',
    'AINT': 'Acepta «ain\'t» u otra forma no estándar',
    'DESCARTE': 'Opciones que se eligen por descarte o no son las que nombra la consigna',
    'TRAD': 'Traducción rota o que dice otra cosa',
    'MINI': 'Miniteoría falsa o convertida en una orden',
    'PERDIO': 'Se perdió algo que la respuesta necesita',
    'RESP': 'Acepta una respuesta mala o la clave está mal',
    'CORTAS': 'Formas cortas: se fue el negativo que el tema practica',
    'CONTABLE': 'Contable o incontable: de clasificar a usar',
    'TRADRESP': 'La traducción traduce sólo la respuesta (decidido el 25/10)',
}

MAL = {
    **{k: ('real', 'AINT') for k in ('25be', '1f53', '25d5', '25e3', '25f5', '25f7', '25fc', '2003', '206a', '207a',
                                      '2086', '2099', '214d', '1e9c')},
    **{k: ('real', 'PISTA') for k in ('1eeb', '2616', '241b', '2422', '2424', '24bf', '24c2', '24c3', '24c7', '24cb',
                                       '24d1', '24d2', '24db', '24dc', '24dd', '24e2', '2535', '2539')},
    **{k: ('real', 'DESCARTE') for k in ('26f0', '26f1', '26f4', '26f5', '26f6', '26f8', '2392', '2394', '2ae5',
                                          '2443', '244b', '2c85')},
    **{k: ('real', 'TEMA') for k in ('1fe1', '21a4', '21a6', '21ab', '21b5', '221d', '2764', '227f', '22bb', '29cd',
                                      '2a1a', '244c', '2c89', '245c', '2c8a', '24a8', '24b0', '24b7', '2cd8', '2cdb',
                                      '2cdc', '2cdd', '2ce0', '2ce2', '2ce6', '2ce8', '24f3', '25a5', '2f1e', '2f22')},
    **{k: ('real', 'TRAD') for k in ('23e2', '23eb', '2465', '257c', '258f')},
    **{k: ('real', 'MINI') for k in ('1f8c', '21b4', '21b6', '21ba', '2406', '2505')},
    **{k: ('real', 'PERDIO') for k in ('1f5d', '26d4')},
    **{k: ('real', 'RESP') for k in ('221f', '2ad2', '2415')},
    **{k: ('leve', 'CORTAS') for k in ('21a9', '21aa', '21ae', '21b8')},
    **{k: ('decidir', 'CONTABLE') for k in ('2306', '2309', '230a', '230b', '230c', '230d', '230f', '2310', '2311',
                                             '2312', '2313', '2314')},
    **{k: ('falsa', 'TRADRESP') for k in ('22c7', '22cb', '22d5')},
    '2200': ('leve', 'TEMA'),
    '24be': ('leve', 'RESP'),
    '24f2': ('leve', 'DESCARTE'),
}

# Lo que conviene decir de un caso además de su causa.
NOTAS = {
    '21a9': 'Producción aceptaba una sola de dos respuestas válidas (was y wasn\'t); la corrección lo arregló sacando '
            'el negativo, que es lo que el tema practica.',
    '2ad2': 'La clave pasó a «that» aunque la oración sigue diciendo que el que habla sostiene el sombrero.',
    '22c7': 'En transformar, la traducción traduce sólo la oración del alumno: lo decidimos el 25/10 (F-DATOS-R001).',
    '2306': 'La consigna del tema pide clasificar («singular countable», «plural countable», «uncountable»); la '
            'corrección pasó a usar el sustantivo en una oración. Algunos siguen practicando contable o incontable '
            '(much / many, there is con un incontable) y otros sólo singular o plural.',
}

CAUSAS |= {
    'FORMA': 'La miniteoría obliga a contraer (o a no contraer) cuando el corrector toma las dos',
    'CASO': 'La miniteoría explica la respuesta de este ejercicio en vez de la regla general',
}

DUDOSO = {
    **{k: ('real', 'PISTA') for k in ('1e8d', '1e8e', '1e92', '25ac', '2618', '261c', '2624', '2073')},
    **{k: ('real', 'FORMA') for k in ('25ad', '25b6', '25b7', '25b9', '1fa5', '1fa8', '1faa', '1fb1', '1fb7', '1fbc',
                                       '1fc1', '1fc3', '25ed', '25ef', '25ff', '2611')},
    **{k: ('leve', 'CASO') for k in ('1eae', '1f8d', '1f8f', '1f90', '1f91', '1f92', '1f93', '1f94', '1f95', '1f96',
                                      '1f98', '1f99', '1f9b', '1f9e', '1fb6', '1fb9', '1fc2', '1fc4', '25bf', '25c1',
                                      '25c3', '25c4', '25cb', '25cc', '25ce', '25d0', '25d1', '25f0', '25f8', '25fb',
                                      '2601', '260e', '1fca', '1fd4', '1fdc', '1fdd', '1fe7', '207b', '207c', '207e')},
    **{k: ('falsa', 'CASO') for k in ('1f50', '1f55', '1f5c', '1f62', '25d8', '25e0', '25e2', '25e8', '25e9', '2633',
                                       '2634', '25c7', '25c9', '25cf', '25fa', '2603', '2607', '2612', '25fe', '260b')},
    '260f': ('real', 'RESP'),
}

DUDOSO |= {
    **{k: ('real', 'PISTA') for k in ('26ef', '21bc', '21bf', '21c9', '22c6', '2319')},
    **{k: ('leve', 'CASO') for k in ('207f', '2081', '2082', '2087', '2089', '208d', '208e', '20a5', '20ad', '20b1',
                                      '20b3', '20b4', '20b5', '20c4', '2165', '222d', '2237', '224a', '2256', '2261',
                                      '2262', '226d', '2323', '2328', '2a11', '2363')},
    **{k: ('falsa', 'CASO') for k in ('2354', '2356', '2359', '235a', '235b', '235c', '235d', '235e', '235f', '2360',
                                       '2361', '2362', '2364', '2366', '2a10')},
    **{k: ('leve', 'TRAD') for k in ('20ee', '20f7')},
    '275e': ('real', 'FORMA'),
}

CAUSAS |= {'SUELTA': 'Una oración suelta que nombra a alguien sin presentarlo'}

DUDOSO |= {
    **{k: ('falsa', 'CASO') for k in ('2a13', '2a14', '2a16', '2a18', '2a1b', '2a1c', '2a1d', '2a1e', '2a20', '2a24',
                                       '2a26', '2a27', '2a2a', '2a2e', '2a2f', '2a34', '2a35', '2a36', '23e0', '2bc2',
                                       '24e0')},
    **{k: ('leve', 'CASO') for k in ('2a12', '2a2c', '2ad8', '23cb', '2445', '2449', '2453', '2c84', '2cd3', '2ce7',
                                      '24f7', '2502', '2503', '2504', '2506', '2507', '2508', '2509')},
    **{k: ('real', 'PISTA') for k in ('24bd', '24c5', '24c6', '24ce', '24d5', '24e1', '24e6', '24e7', '24f5')},
    '24ea': ('leve', 'PISTA'),
    '245a': ('falsa', 'SUELTA'),
}
DUDOSO['24f7'] = ('real', 'RESP')
NOTAS['24f7'] = 'Dejó de aceptar «Could I have some cake?», la forma natural que aceptaba producción.'
NOTAS['245a'] = 'Es el mismo caso del ejemplo «his wedding» que todavía tiene el prompt: el ejemplo contradice la regla nueva.'

DUDOSO |= {
    **{k: ('leve', 'CASO') for k in ('2dd0', '2dd1', '2dd2', '2dd3', '2dd4', '2dd5', '2dd6', '2dd7', '2dd8', '2dd9',
                                      '2dda', '2ddc', '2de1', '253d', '2548', '254a', '2ef2', '2efe', '2eff', '2f01',
                                      '2f05', '2ef3', '2efd', '2f02', '2545', '2570', '2573')},
    **{k: ('falsa', 'CASO') for k in ('253f', '2543', '254e', '2f04', '2551', '2557', '2559', '2ed6', '2ed8', '2edb',
                                       '2edc', '2edd', '2ede', '2edf', '2ee1', '2ee3', '2ee4', '2ee6', '2ee7', '256f',
                                       '2571', '2574')},
    '2eed': ('real', 'TRAD'),
}

DUDOSO |= {
    **{k: ('leve', 'CASO') for k in ('2576', '2577', '2579', '257b', '257e', '2580', '2581', '2584', '258c', '2593',
                                      '2594', '2595', '2f06', '2f07', '2f08', '2f0a', '2f0b', '2f0c', '2f0d', '2f0e',
                                      '2f11', '2f12', '2f13', '2f14', '2f16', '2f17', '2f19')},
    **{k: ('falsa', 'CASO') for k in ('2582', '259c', '259d', '2f1a', '2f26', '2f27', '2f2a', '2f2b')},
    **{k: ('leve', 'PISTA') for k in ('2599', '259f', '25a3', '25a9', '2f1b', '2f23', '2f2c')},
    '2589': ('real', 'TRAD'),
}
NOTAS['2599'] = 'El «very» que agregó la corrección deja una sola respuesta posible: «much».'

# Los 120 «heredado»: 118 bien separados (el problema ya estaba en producción); en 2 la corrección agregó una pista
# que el revisor no vio.
HEREDADO_MAL_SEPARADOS = {'2614': ('real', 'PISTA'), '2625': ('real', 'PISTA')}
NOTAS['2614'] = 'Lo marca heredado por el tema, pero la corrección le escribió el «is» en la pista: «(is / eat)».'

# La muestra de «bien»: 20 al azar de toda la tanda (leer_marcas.py, semilla 20261027) y 12 del tramo grave
# (semilla 27). 31 bien; a 21b0 le pasa lo mismo que a 21a9: leve.
MUESTRA_TRAMO = ('2467', '21cc', '24ed', '2063', '2deb', '20b0', '1f64', '1ecd', '1e99', '1fce', '2609', '22d7')
BIEN_QUE_NO = {'21b0': ('leve', 'CORTAS')}
