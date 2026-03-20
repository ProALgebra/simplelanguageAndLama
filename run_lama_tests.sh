#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

REGRESSION_CLASS="com.oracle.truffle.lama.test.LamaRegressionTest"
SMOKE_CLASS="com.oracle.truffle.lama.test.LamaSmokeTest"
T074_CLASS="com.oracle.truffle.lama.test.LamaTest074"
T106_CLASS="com.oracle.truffle.lama.test.LamaTest106"
T112_CLASS="com.oracle.truffle.lama.test.LamaTest112"

run_mvn() {
  echo
  echo ">>> $*"
  "$@"
}

main() {
  run_mvn mvn -pl language -Dtest="$SMOKE_CLASS" test
  run_mvn mvn -pl language -Dtest="$T074_CLASS" test
  run_mvn mvn -pl language -Dtest="$T106_CLASS" test
  run_mvn mvn -pl language -Dtest="$T112_CLASS" test
  run_mvn mvn -pl language -Dtest="$REGRESSION_CLASS" test
}

main
