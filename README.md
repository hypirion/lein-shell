# lein-shell

A Leiningen plugin for calling shell commands.

## Usage

Use this for user-level plugins:

Put `[lein-shell "0.2.0"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
lein-shell 0.2.0`.

Use this for project-level plugins:

Put `[lein-shell "0.2.0"]` into the `:plugins` vector of your project.clj.

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
:prep-tasks [["shell" "make"]]
```

### OS-specific subprocess call

Different operating systems may use different commands for equivalent
functionality. When such issues arises, it would be convenient if you could
somehow specify this. This is possible with lein-shell: Say you have a command
named `foo` in Linux, but `bar` in Windows, and you want to run this command as
a prepared task before compiling and similar. To enable auto-preparation for
such a task, a setup like this should suffice:

```clj
(defproject ...
  ...
  :prep-tasks [["shell" "foo" "arg1" "arg2"] "javac" "compile" ]
  :shell {:commands {"foo" {:windows "bar"}}})
```

Here, `lein` will run `foo arg1 arg2` on any non-Windows system and `bar arg1
arg2` on Windows, and this will happen before any task within this given
project.

## License

Copyright © 2013 Jean Niklas L'orange

Distributed under the Eclipse Public License, the same as Clojure.
