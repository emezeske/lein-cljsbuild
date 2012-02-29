# Migrating from 0.0.x to 0.1.x

The `0.1.2` release of lein-cljsbuild adds several new features that necessitate
some changes to the format of the `:cljsbuild` configuration entry.

With that said, `0.1.2` **is** backwards-compatible with your existing configuration.
However, it will be complaining loudly about how it's deprecated.  This document
explains how to fix that.

The next major release will drop backwards compatibilty with `0.0.x`, so get
your project updated soon!

## The Easy Way

Before doing anything else, `lein cljsbuild clean`.  This will remove old temporary files
that may be renamed in the new version.  Most importantly, it will remove any crossover files
that have been copied into place.  **This is very important.  If you are using crossovers,
skipping this step will cause pain!**

Once you've updated your project to use `[lein-cljsbuild "0.1.2"]`, run any subcommand,
for instance, `lein cljsbuild once`.  The plugin will complain that your configuration
is deprecated.  However, it should automatically convert your `:cljsbuild` entry to the new
format, and print it out.

You should be able to just replace your existing `:cljsbuild` entry with this one and be all set.

## The Hard Way

In case you want to know specifically what has changed:

* Several plugin-global options have been added.  Thus, what used to be the top-level
config has been moved into the `:builds` entry.

* The `:builds` entry is not allowed to be either a map or a vector, like the `:cljsbuild`
entry used to be.  It is always a vector of maps.

* The `:output-dir` entry for each of the `:compiler` entries must now be unique.  Previously, they
could be set to the same directory, which caused a nasty race condition (and failed compiles).

* `:crossovers` are no longer specified per-build -- they are global to the whole project.
Instead of being copied into a `:source-path`, they're all copied into a single `:crossover-path`.
