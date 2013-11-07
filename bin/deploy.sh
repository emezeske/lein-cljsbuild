
pushd cljs-compat
lein do clean, deploy clojars
popd

pushd support
lein do clean, deploy clojars
popd

pushd plugin
lein do clean, deploy clojars
popd
