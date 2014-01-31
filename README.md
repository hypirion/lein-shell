# lein-shell

A Leiningen plugin for calling shell commands. lein-shell is an active, stable
project.

## Installation

Put `[lein-shell "0.4.0"]` into the `:plugins` vector of your `:user` profile
inside `~/.lein/profiles.clj` if you want to use lein shell on a per user basis
(this doesn't *really* make much sense, but you're allowed to if you want to!).

To explicitly say that this project needs lein-shell to be built, putt
`[lein-shell "0.4.0"]` into the `:plugins` vector of your `project.clj`. If you
have no `:plugins` vector in your `project.clj`, it should look like this:

```clj
(defproject your-project-here "version"
 ...
 :plugins [[lein-shell "0.4.0"]]
 ...)
```

## Quickstart

It is very straightforward to use lein-shell: lein-shell will call the shell
command with eventual parameters you include. For instance, if you want your
favourite cow to say hello to you from Leiningen, the following will be printed
within your shell:

    $ lein shell cowsay 'Hello from Leiningen!'
	 _______________________
    < Hello from Leiningen! >
     -----------------------
            \   ^__^
             \  (oo)\_______
                (__)\       )\/\
                    ||----w |
                    ||     ||

Now, this may look rather useless as you can just omit `lein shell` and get the
exact same result in less time. However, it may be of value if you're using
`make` or `ANTLR` to generate files for you, needed by your Clojure project. For
example, to automatically call `make` before running tasks, add this to your
`project.clj` map:

```clj
:prep-tasks [["shell" "make"] "javac" "compile"]
```

If the command exits with a nonzero exit code, shell will (attempt to) exit
Leiningen with the same exit code. If wanted to, this functionality can be
overridden, and many other settings can be modified as well.

## Documentation

For more information, have a look at [the documentation][documentation]. It
contains a lot of examples, some which hopefully are useful to you.

[documentation]: https://github.com/hyPiRion/lein-shell/blob/stable/doc/DOCUMENTATION.md

## License

Copyright © 2013-2014 Jean Niklas L'orange

Distributed under the Eclipse Public License, the same as Clojure.
