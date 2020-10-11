#!/usr/bin/env bash

set -x

./gradlew dokkaHtml
mkdir -p build/docs

cp -r build/dokka/html/. build/docs/
cp -r public/. build/docs/

