#!/bin/bash
#

CURRENT_VERSION=$1
NEXT_VERSION=$2

for f in `find . -name project.clj` README.md `find doc -name '*.md' | grep -v RELEASE-NOTES.md`; do
    sed -i s/$CURRENT_VERSION/$NEXT_VERSION/ $f
done
