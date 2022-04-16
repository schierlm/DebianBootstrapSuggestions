DebianBootstrapSuggestions
==========================

Debian packages that may need closer examination for bootstrappability

Summary
-------

This repo contains scripts and package lists of packages that have some kind of build self-dependency,
analyzed from Debian's dependency information.

First, there are 98 ["essential" packages](essential.list) built from 67 [source packages](selfdeps/essential.tsv).
These packages need to be installed on every Debian package, so they cannot participate in dependency resolution.

Next, building a package always needs an additional 40 ["build-essential" packages](essential.list) installed, which are
built from 11 [source packages](selfdeps/build-essential.tsv). Therefore, these packages cannot participate in
dependency resolution either and have to be manually checked.

In addition, there are 41 [source packages](selfdeps/directselfdeps.tsv) that directly build-depend on a package that is built
by the package itself, so these are the most direct dependencies that should be manually checked.


The rest gets hairy. The only "accurate" way to find more dependency cycles I found is to resolve the build-dependencies
for each package individually, translate the binary packages in those dependencies to source dependencies and analyze the graph.
Debian's resolver is not the fastest, so I took a shortcut. I first used [botch](https://packages.debian.org/buster/botch) to create
a graph that contains **any** package relationships (build-depends, is-built-from, as well as virtual package alternatives).
This graph contains too many edges, but it can be used as an "upper limit". I repeatedly removed all sources and sinks, then created
a list of remaining source package nodes (about 3000) and ran the resolver for each of them.

213 [source packages](selfdeps/selfdeps.tsv) need to have a package that is built by itself installed, without actually declaring
that dependency directly.

There are 11 additional small strongly connected components (cycles) consisting of
34 [source packages](cycles/small-cycles.txt) with cycle lengths of up to three.

But the rest of the resulting graph is a mess... It consists of only three strongly connected components, but their sizes are
16 ([php related](cycles/php-cycle.pdf)), 31 ([ruby related](cycles/ruby-cycle.pdf)) and 749 (!).

For the last one I was not even able to create an acceptable visualization (the best I could get is interactively exploring the
graph with yEd), so I provide the [raw data](intermediate/) instead.

[This text file](intermediate/big-cycle-dependencies.txt) contains all the three big components, not only the largest one.

Update
------

The [author of botch](https://github.com/josch) reached out and pointed out that in fact, botch also could have done that kind
of dependency resolution, and they even provide a page with [weekly dependency statistics](https://bootstrap.debian.net/botch-native/amd64/stats.html).

More interesting details can be found in [the issue itself](https://github.com/schierlm/DebianBootstrapSuggestions/issues/1).
