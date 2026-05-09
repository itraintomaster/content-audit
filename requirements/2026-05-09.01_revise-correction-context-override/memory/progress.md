# Progress - FEAT-REVCTX

2026-05-09 — analyst — Feature creado como spin-off del Gap 2 reportado por
  el operador al integrar el dashboard.
  why: el operador identifico que revise hoy opera contra el plan persistido
  y no respeta el contexto proyectado que el cliente le muestra al operador.
  Consecuencia concreta: el LLM de LAPS recibe el contexto basal y puede
  proponer una palabra que ya consumio una propuesta pendiente, anulando el
  beneficio visual del plan proyectado. Este feature habilita un opt-in para
  que revise reciba el correctionContext desde el cliente y lo consuma sin
  combinarlo. Reglas R001 a R006 definen el contrato observable; los doubts
  cubren shape del flag, formato del input, profundidad de validacion de
  coherencia, trazabilidad del artefacto, e interaccion con las invariantes
  secundarias de las estrategias activas. Pendiente: que el operador revise
  doubts y elija opciones, despues pasa a @architect.
