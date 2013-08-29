#!/bin/bash
#
# Build the lein-cljsbuild plugin and support JARs, install them
# locally, and test them in the simple and advanced example projects.
# If arguments are specified, only build the example projects they name.

set -e

projects="$@"
[ -z "$projects" ] && projects='simple advanced'
project_root=$(dirname $(realpath $0))/..
pushd $project_root

rm -rf ~/.m2/repository/lein-cljsbuild ~/.m2/repository/cljsbuild/
for d in support plugin; do
	pushd $d
	lein do install, test
	popd
done
for d in $projects; do
	pushd example-projects/$d
    printf "\n** Testing project '$d'\n\n"
	if [ $d = advanced ]; then
		extra_command=', cljsbuild test'
	fi
	lein clean
	lein do cljsbuild clean, cljsbuild once$extra_command
	popd
done
