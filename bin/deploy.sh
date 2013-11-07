
pushd cljs-compat
lein do clean, deploy clojars
popd

pushd support
lein do clean, deploy clojars
popd

pushd plugin
# needed to allow for post-release updates to cljs-compat
export LEIN_SNAPSHOTS_IN_RELEASE=true
lein do clean, deploy clojars
popd
