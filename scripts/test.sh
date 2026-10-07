#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir="$repo_dir/target/test-classes"
rm -rf "$classes_dir"
mkdir -p "$classes_dir"
javac -d "$classes_dir" $(find "$repo_dir/src/main/java" "$repo_dir/src/test/java" -name '*.java' -print)
java -cp "$classes_dir" learnmarket.templates.MarketplaceLifecycleTemplatesTest
