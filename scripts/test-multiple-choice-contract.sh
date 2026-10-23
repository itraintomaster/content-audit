#!/bin/bash
set -euo pipefail

contract_root="$(cd "$(dirname "$0")/.." && pwd)"
export ITTM_ROOT="${ITTM_ROOT:-"$contract_root/../ittm"}"
contract_python="${CONTRACT_PYTHON:-python3}"
export PYTHONDONTWRITEBYTECODE=1

"$contract_python" -c 'import bson, pydantic, jsonschema, pytest'

(cd "$ITTM_ROOT" && "$contract_python" -m unittest discover -s be/scripts/tests -v)
(cd "$ITTM_ROOT/be/tools/pipeline" && "$contract_python" -m pytest -p no:cacheprovider extract/full_course/tests)
(cd "$contract_root" && "$contract_python" -m unittest discover -s .sentinel/agents/quiz-instruction-validator/eval -p 'test_student_view_inputs.py' -v)
(cd "$contract_root" && "$contract_python" -m unittest discover -s scripts/contract_tests -v)
(cd "$contract_root" && "$contract_python" -I -m unittest discover -s scripts/production_publish/tests -t scripts)

# Maven runs only local tests. The panel receives a recorded proposal and fake
# measurements; no model provider or production database is invoked.
(cd "$contract_root" && mvn -o -q -pl audit-cli -am \
  -Dtest='*ProductionContractTest,*Conversion*Test,MultipleChoiceSentencePreservationTest,DefaultTopicPanelTest,DefaultTopicPanelIdentityTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true test)
