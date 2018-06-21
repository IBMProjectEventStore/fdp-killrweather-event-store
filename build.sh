#!/usr/bin/env bash

set -eu

HERE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"

echo $HERE

. $HERE/version.sh

# The only allowed argument is the optional version string
[[ $# -gt 0 ]] && VERSION=$1
echo "$0: Using version $VERSION"

cd $HERE
sbt -no-color "set version in ThisBuild := \"$VERSION\"" "show version" clean package dockerBuildAndPush

echo "$PWD: built package and docker image(s). Pushed the docker image(s)."
