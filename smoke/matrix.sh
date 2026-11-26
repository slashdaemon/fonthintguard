#!/bin/sh
# Runs smoke/run.sh for every supported Minecraft version (Fabric) and NeoForge line, against the jars
# in dist/ (./gradlew collectJars first), plus one negative control. Results: smoke/runs/results.txt
set -u
root=$(cd "$(dirname "$0")/.." && pwd); cd "$root"
v=$(sed -n 's/^mod_version=//p' gradle.properties)
out=smoke/runs/results.txt; mkdir -p smoke/runs; : > "$out"
r() { smoke/run.sh "$@" | tee -a "$out"; }

for mc in 1.20.5 1.20.6 1.21 1.21.1; do r fabric $mc dist/fonthintguard-$v-fabric-1.20.5-1.21.1.jar; done
for mc in 1.21.2 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8; do r fabric $mc dist/fonthintguard-$v-fabric-1.21.2-1.21.8.jar; done
for mc in 1.21.9 1.21.10 1.21.11; do r fabric $mc dist/fonthintguard-$v-fabric-1.21.9-1.21.11.jar; done

# Latest NeoForge build of each line (stable if the line has one, else its last beta)
meta=$(curl -s https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml | sed -n 's:.*<version>\(.*\)</version>.*:\1:p')
latest() { s=$(echo "$meta" | grep "^$1\.[0-9]*$" | tail -1); [ -n "$s" ] && echo "$s" || echo "$meta" | grep "^$1\." | tail -1; }
# NeoForge 20.5 is left out: beta-only, and ModDevGradle can't resolve its artifacts
for line in 20.6 21.0 21.1; do r neoforge "$(latest $line)" dist/fonthintguard-$v-neoforge-1.20.6-1.21.1.jar; done
for line in 21.2 21.3 21.4 21.5 21.6 21.7 21.8 21.9 21.10 21.11; do r neoforge "$(latest $line)" dist/fonthintguard-$v-neoforge-1.21.2-1.21.11.jar; done

# Negative control: the getGlyph-era jar with its version range widened, on 1.21.3 (call moved to
# loadGlyph). Must FAIL at startup; a PASS here means the guard can run half-applied.
neg=smoke/runs/negative; rm -rf "$neg"; mkdir -p "$neg/x"
cp dist/fonthintguard-$v-neoforge-1.20.6-1.21.1.jar "$neg/negative-control.jar"
(cd "$neg/x" && unzip -q ../negative-control.jar META-INF/neoforge.mods.toml \
  && sed -i '' -e 's/\[20\.6,21\.2)/[20.6,22)/' -e 's/\[1\.20\.6,1\.21\.2)/[1.20.6,1.22)/' META-INF/neoforge.mods.toml \
  && zip -q ../negative-control.jar META-INF/neoforge.mods.toml)
echo "NEGATIVE CONTROL (expect FAIL):" | tee -a "$out"
r neoforge "$(latest 21.3)" "$neg/negative-control.jar"
echo DONE | tee -a "$out"
