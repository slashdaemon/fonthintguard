#!/bin/sh
# Boot test for one shipped jar on one real client, with the canary font pack enabled so the title
# screen's capital A goes through both patched FT_Load_Glyph calls.
#
#   smoke/run.sh fabric   <mc version>       <jar>     e.g. smoke/run.sh fabric 1.21.4 dist/fonthintguard-1.1.0-fabric-1.21.2-1.21.8.jar
#   smoke/run.sh neoforge <neoforge version> <jar>     e.g. smoke/run.sh neoforge 21.4.161 dist/...-neoforge-1.21.2-1.21.11.jar
#
# PASS: the mod logged its startup check, resources finished loading and the client was still alive
# with no crash report SETTLE seconds later. Needs JAVA_HOME = a JDK 21 and a desktop session.
# Prints one "RESULT <loader> <version> PASS|FAIL <reason>" line; logs stay in smoke/runs/<run>/.
set -u
loader=$1 ver=$2 jar=$(cd "$(dirname "$3")" && pwd)/$(basename "$3")
root=$(cd "$(dirname "$0")/.." && pwd)
TIMEOUT=${TIMEOUT:-900} SETTLE=${SETTLE:-30}

# Minecraft version of the run (NeoForge 21.4.161 -> 1.21.4, 21.0.x -> 1.21, 20.6.x -> 1.20.6)
if [ "$loader" = neoforge ]; then
  mc=$(echo "$ver" | awk -F. '{ if ($2 == "0") print "1." $1; else print "1." $1 "." $2 }')
else
  mc=$ver
fi

# Resource pack format per Minecraft version (1.21.9+ uses min_format/max_format)
case $mc in
  1.20.5|1.20.6) fmt=32 ;; 1.21|1.21.1) fmt=34 ;; 1.21.2|1.21.3) fmt=42 ;; 1.21.4) fmt=46 ;;
  1.21.5) fmt=55 ;; 1.21.6) fmt=63 ;; 1.21.7|1.21.8) fmt=64 ;; 1.21.9|1.21.10) fmt=69 ;; 1.21.11) fmt=75 ;;
  *) echo "RESULT $loader $ver FAIL unknown MC version $mc"; exit 1 ;;
esac

run=$root/smoke/runs/$loader-$ver
rm -rf "$run"; mkdir -p "$run/mods" "$run/resourcepacks/fhg-canary"
cp -R "$root/testpack/assets" "$run/resourcepacks/fhg-canary/"
if [ "$fmt" -ge 65 ]; then
  meta="\"min_format\": $fmt, \"max_format\": $fmt"
else
  meta="\"pack_format\": $fmt"
fi
printf '{"pack": {%s, "description": "Font Hinting Guard canary"}}\n' "$meta" > "$run/resourcepacks/fhg-canary/pack.mcmeta"
printf 'resourcePacks:["vanilla","file/fhg-canary"]\nonboardAccessibility:false\nnarrator:0\nsoundCategory_master:0.0\n' > "$run/options.txt"

if [ "$loader" = neoforge ]; then
  cp "$jar" "$run/mods/"
  set -- -Ploader=neoforge -Pneoforge="$ver" -Pgamedir="$run" :neoforge:runClient
else
  set -- -Ploader=fabric -Pmc="$ver" -Ploader_version="${FABRIC_LOADER:-0.18.6}" -Pjar="$jar" -Pgamedir="$run" :fabric:prodClient
fi
"$root/gradlew" --no-daemon -p "$root/smoke" "$@" > "$run/gradle.log" 2>&1 &
gpid=$!

killtree() { for c in $(pgrep -P "$1"); do killtree "$c"; done; kill -9 "$1" 2>/dev/null; }
finish() { killtree "$gpid"; echo "RESULT $loader $ver $1"; exit 0; }

log=$run/logs/latest.log t=0
while [ $t -lt "$TIMEOUT" ]; do
  sleep 5; t=$((t + 5))
  ls "$run"/crash-reports/*.txt >/dev/null 2>&1 && finish "FAIL crash report: $(grep -m1 -E 'Description|Exception' "$run"/crash-reports/*.txt)"
  kill -0 "$gpid" 2>/dev/null || finish "FAIL client/gradle exited: $(grep -m1 -E 'Exception|FAILED|Error' "$run/gradle.log" "$log" 2>/dev/null)"
  [ -f "$log" ] || continue
  grep -q -E 'MixinApplyError|InvalidInjectionException|InjectionError|patch not applied' "$log" && finish "FAIL $(grep -m1 -E 'MixinApplyError|InvalidInjectionException|InjectionError|patch not applied' "$log")"
  if grep -q 'TrueType hinting disabled' "$log" && grep -q 'Sound engine started' "$log"; then
    sleep "$SETTLE"
    ls "$run"/crash-reports/*.txt >/dev/null 2>&1 && finish "FAIL crash after load: $(grep -m1 -E 'Description|Exception' "$run"/crash-reports/*.txt)"
    kill -0 "$gpid" 2>/dev/null || finish "FAIL exited after load"
    grep -q 'Reloading ResourceManager:.*fhg-canary' "$log" || finish "FAIL canary pack not in the resource reload"
    finish "PASS $(grep -m1 -o '\[FontHintGuard\].*' "$log")"
  fi
done
finish "FAIL timeout after ${TIMEOUT}s"
