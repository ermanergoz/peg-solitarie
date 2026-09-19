#!/bin/zsh
# levels.sh <serial> <count> <shot-prefix> : from a Results screen, tap "Next level" and solve <count> levels.
# Added by qa-engineer 2026-09-20. Stops (exit 2) on any screen it does not recognise (e.g. an interstitial).
D=${0:a:h}; S=$1; N=$2; P=$3
for i in $(seq 1 $N); do
  python3 $D/ui.py $S tap "Next level" || { python3 $D/ui.py $S shot $P-unexpected-$i; exit 2; }
  sleep 4
  L=$(python3 $D/ui.py $S dump | grep -oE "desc='Challenge level [0-9]+'" | grep -oE "[0-9]+")
  [ -z "$L" ] && { echo "not on a game screen"; python3 $D/ui.py $S shot $P-unexpected-$i; exit 2; }
  echo "== level $L"
  python3 $D/robot.py $S | tail -3 || exit 2
  sleep 4
  python3 $D/ui.py $S dump | grep -E "text='(Perfect!|New best!|Next level|[A-Za-z ]+!)'" | cut -c1-90
  python3 $D/ui.py $S shot $P-L$L-result >/dev/null
done
