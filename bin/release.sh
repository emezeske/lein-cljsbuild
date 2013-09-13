#!/bin/bash
#
# Release a specific branch to master.

set -e

branch=$1
if [ -z "$1" ]; then
    echo "Usage: $0 <branch>."
    exit 1
fi

read -p "Are you sure you want to release $branch to master (y/n)? " -n 1 reply
echo
if [[ ! $reply =~ ^[Yy]$ ]]; then
    echo 'Aborting.'
    exit 1
fi

echo "Releasing $branch."
git checkout master
git merge $branch
git push --all
git checkout $branch
git tag -m "Release ${branch}." $branch
git push --tags
git checkout master
git branch -D $branch
git push origin :refs/heads/$branch

bin/deploy.sh

