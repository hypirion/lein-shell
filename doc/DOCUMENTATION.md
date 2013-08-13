# Documentation

lein-shell is a plugin for running shell commands. Sometimes, you just need to
be able to run some sort of setup tool not available from the JVM, or which does
not (yet?) have a Leiningen plugin which does the job.

Generally speaking, shell commands are straightforward. However, sometimes you
would like to tweak the setup a bit. Perhaps you would like to change the
working directory, or maybe you want to ignore the exit code. lein-shell aims to
be configurable enough that such things can be done through modifications in the
`project.clj` only.

## Installation

Put `[lein-shell "0.2.0"]` into the `:plugins` vector of your `:user` profile
inside `~/.lein/profiles.clj` if you want to use lein shell on a per user basis
(this doesn't *really* make much sense, but you're allowed to if you want to!).

To explicitly say that this project needs lein-shell to be built, putt
`[lein-shell "0.2.0"]` into the `:plugins` vector of your `project.clj`. If you
have no `:plugins` vector in your `project.clj`, it should look like this:

```clj
(defproject your-project-here "version"
 ...
 :plugins [[lein-shell "0.2.0"]]
 ...)
```

## Basic Usage

lein-shell is very straightforward to use. Just prepend the shell command you'd
like to call with `lein shell`, and you're ready to go. For instance, if I
wanted my favourite duck to tell me `Hello from Leiningen!`, I could just do
this:

    lein shell cowsay -f duck 'Hello from Leiningen!'
     _______________________
    < Hello from Leiningen! >
     -----------------------
     \
      \
       \ >()_
          (__)__ _
    

lein-shell doesn't need to be run inside a project, but it is usually the
sensible place to use it.

## (Example) Usage

lein-shell can be used for many things, but it is commonly used for preparation
tasks and inside aliases. It is not limited to this, of course, but usage
outside this scope is somewhat obscure.

### As a Preparation Task

In some projects, you would have to build files from scratch. For instance, if
you have a web server and some `.less` files you'd like to convert to `.css`,
that could be done by lein-shell. If you have an ANTLR grammar you'd like to
compile before you compile your Clojure code, using lein-shell as a preparation
task would also work. As an example, consider a project where I have some
generated code my clojure code depends on. To generate that code before
uberjaring, testing and repl'ing, I can do this:

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :prep-tasks [["shell" "generator" "--in" "build/in.grammar"
                                   "--out" "src/out.clj"]]
  :plugins [[lein-shell "0.2.0"]])
```

Now, the command `generator --in build/in.grammar --out src/out.clj` will always
be called before any in-project evaluation.

Mind you, lein-shell is not a replacement for `make`. A shell command will
always be executed, no matter the circumstances. However, lein-shell can call
`make`, which should solve that issue nicely.

#### A Word About `:prep-tasks`

TODO: How does it work?

### As an Alias

lein-shell can also be used as an alias. I tend to use the 1.7 javadoc for java
documentation, but the 1.6 jvm for backwards compatibility reasons. As such, I
have to call the shell because I cannot use the 1.6 API. Here's how I handle
that:

```clj
(defproject package.name/project "0.1.0-SNAPSHOT"
  ...
  :plugins [[lein-shell "0.2.0"]]
  :aliases {"javadoc" ["shell" "javadoc" "-d" "javadoc"
                        "-sourcepath" "src/" "package.name"]
            "jar" ["do" "javadoc," "jar"]}}})
```

Whenever I want to deploy, I also generate the javadoc and include it within the
jar file generated (through some inclusion filters). In that way, I don't mess
up and include old javadoc files.

If building larger stuff takes time, it possible to set up aliases for the
building tasks so that you do it manually once. While this could be added in by
a shell file, the project may look cleaner if you don't have shell scripts
sprinkled around everywhere.

## Configuration

### Environment variables and directory specification

printenv, pwd

### Exit codes

false

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
  :prep-tasks [["shell" "foo" "arg1" "arg2"] "javac" "compile"]
  :shell {:commands {"foo" {:windows "bar"}}})
```

Here, `lein` will run `foo arg1 arg2` on any non-Windows system and `bar arg1
arg2` on Windows, and this will happen before any task within this given
project.
