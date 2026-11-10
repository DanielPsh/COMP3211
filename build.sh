#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "$0")"
action=${1:-build}
case "$action" in
    build|run|test|coverage) ;;
    *) printf 'Usage: %s [build|run|test|coverage]\n' "$0" >&2; exit 2 ;;
esac

mkdir -p build
output_dir=$(mktemp -d build/run.XXXXXX)
keep_output=false
trap 'if [[ $keep_output == false ]]; then rm -rf -- "$output_dir"; fi' EXIT

app_sources=(Main.java)
while IFS= read -r -d '' source; do
    app_sources+=("$source")
done < <(find model controller view -type f -name '*.java' -print0)
javac -g -encoding UTF-8 -d "$output_dir/classes" "${app_sources[@]}"

if [[ $action == build ]]; then
    keep_output=true
    printf 'Compiled application: %s/%s/classes\n' "$PWD" "$output_dir"
    exit
fi

if [[ $action == run ]]; then
    java -cp "$output_dir/classes" Main
    exit
fi

junit_jar=lib/junit-platform-console-standalone-6.1.3.jar
test_sources=()
while IFS= read -r -d '' source; do
    test_sources+=("$source")
done < <(find test -type f -name '*.java' -print0)
javac -g -encoding UTF-8 -cp "$output_dir/classes:$junit_jar" \
    -d "$output_dir/test-classes" "${test_sources[@]}"

junit_args=(-jar "$junit_jar" execute
    --class-path "$output_dir/classes:$output_dir/test-classes"
    --scan-class-path="$output_dir/test-classes"
    --fail-if-no-tests --disable-ansi-colors --details summary)

if [[ $action == test ]]; then
    java "${junit_args[@]}"
    exit
fi

# Use the same verified tool version for instrumentation and report generation.
fetch_jacoco_jar() {
    local artifact=$1 classifier=$2 checksum=$3
    local version=0.8.14
    local filename="$artifact-$version-$classifier.jar"
    local jar="build/tools/$filename"
    local temporary

    if [[ ! -f $jar ]] || ! printf '%s  %s\n' "$checksum" "$jar" | shasum -a 256 --check --status; then
        temporary=$(mktemp build/tools/download.XXXXXX)
        if ! curl --fail --silent --show-error --location --retry 2 \
            "https://repo.maven.apache.org/maven2/org/jacoco/$artifact/$version/$filename" \
            --output "$temporary" ||
            ! printf '%s  %s\n' "$checksum" "$temporary" | shasum -a 256 --check --status; then
            rm -f -- "$temporary"
            printf 'Could not download or verify %s.\n' "$filename" >&2
            return 1
        fi
        mv -f -- "$temporary" "$jar"
    fi
    printf '%s\n' "$jar"
}

mkdir -p build/tools
agent=$(fetch_jacoco_jar org.jacoco.agent runtime \
    3fb76eea65f81bd9415202bab34b6571728841dff1ab8e6bbe81adc2e299face)
cli=$(fetch_jacoco_jar org.jacoco.cli nodeps \
    811c7f8c6b358c5d68a8973cfa867f6892be7a671b697a4b13c4b447e6daf75c)

java "-javaagent:$agent=destfile=$output_dir/model.exec,includes=model.*" \
    "${junit_args[@]}" --reports-dir "$output_dir/junit"
mkdir -p "$output_dir/coverage"
java -jar "$cli" report "$output_dir/model.exec" \
    --classfiles "$output_dir/classes/model" --sourcefiles . --encoding UTF-8 \
    --name 'PIM Model Coverage' --html "$output_dir/coverage" \
    --csv "$output_dir/coverage/model.csv" --xml "$output_dir/coverage/model.xml"

keep_output=true
printf 'Coverage report: %s/%s/coverage/index.html\n' "$PWD" "$output_dir"
