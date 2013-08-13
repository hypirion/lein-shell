# Documentation for lein-shell

lein-shell is a plugin for running shell commands. Sometimes, you just need to
be able to run some sort of setup tool not available from the JVM, or which does
not (yet?) have a Leiningen plugin which does the job.

Generally speaking, shell commands are straightforward. However, sometimes you
would like to tweak the setup a bit. Perhaps you would like to change the
working directory, or maybe you want to ignore the exit code. lein-shell aims to
be configurable enough that such things can be done through modifications in the
`project.clj` only.

## Installation

Put `[lein-shell "0.3.0"]` into the `:plugins` vector of your `:user` profile
inside `~/.lein/profiles.clj` if you want to use lein shell on a per user basis
(this doesn't *really* make much sense, but you're allowed to if you want to!).

To explicitly say that this project needs lein-shell to be built, putt
`[lein-shell "0.3.0"]` into the `:plugins` vector of your `project.clj`. If you
have no `:plugins` vector in your `project.clj`, it should look like this:

```clj
(defproject your-project-here "version"
 ...
 :plugins [[lein-shell "0.3.0"]]
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
                                    "--out" "src/out.clj"]
               "javac" "compile"]
  :plugins [[lein-shell "0.3.0"]])
```

Now, the command `generator --in build/in.grammar --out src/out.clj` will always
be called before any in-project evaluation.

Mind you, lein-shell is not a replacement for `make`. A shell command will
always be executed, no matter the circumstances. However, lein-shell can call
`make`, which should solve that issue nicely.

#### A Word About `:prep-tasks`

So, what is `:prep-tasks` really? How does it work?

All core tasks in Leiningen that must be run inside a project will activate
`:prep-tasks`, which are Leiningen tasks run before the actual task you run. By
default, `:prep-tasks` equals to `["javac" "compile"]`, but this can be
modified. However, once you start modifying it, the default won't activate. So,
if you're in need of `javac` and `compile`, you must put them before or after
the modifications you've done. In the example above, the shell command may
generate code needed for the javac task or the compile task. This is usually the
case, but you can put the shell command at the end if you need the compiled
java/clojure code.

### As an Alias

lein-shell can also be used as an alias. I tend to use the 1.7 javadoc for java
documentation, but the 1.6 jvm for backwards compatibility reasons. As such, I
have to call the shell because I cannot use the 1.6 API. Here's how I handle
that:

```clj
(defproject package.name/project "0.1.0-SNAPSHOT"
  ...
  :plugins [[lein-shell "0.3.0"]]
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

Generally, the default setup should suffice for most use cases. However, there
are times when the defaults are not what you want, or where you would like to
configure the environment or the directory you're running from. Here's how you
would do that.

### Setting Configurations and their Priority

Options are specified inside the project as follows:

```clj
(defproject ...
  ...
  :shell {:option1 choice
          :option2 other-choice
          :commands {"foo" {:option1 choice-for-foo}}})
```

Configuration options can currently be set as a default setting for all shell
commands, or as an option for a specific shell command. Whenever a setting for a
specific shell command is given, then the default setting is ignored. In the
example above, all shell commands will have `option2` set to `other-choice`. The
command `foo` will have set `option1` to `choice-for-foo`, whereas all other
commands have `choice` set for `option1`.

### Environment Variables

To configure the environment variables, you set `:env` to a map with the
environment variables you want to set. The default value is `{}`.

As an example, let's see how we can change the default java command, `JAVA_CMD`:

```clj
(defproject ...
  ...
  :shell {:env {"JAVA_CMD" "java42"})
```

We can see that it's working by using `printenv`. `printenv JAVA_CMD` usually
prints out `java` or nothing, but `lein shell printenv JAVA_CMD` should now
print out `java42`.

### Directory Specification

To configure the working directory (which directory you're calling the command
from), set the `:dir` to a string which specifies the directory to work from.
The default value is the root directory of the project. The directory can be set
relative to the root directory of the project.

As an example, see the setup below.

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :shell {:dir "src"})
```

Assume that the project is placed within `/home/pir/workspace/my-project`.
Calling `lein shell pwd` would then print out
`/home/pir/workspace/my-project/src`, and if we changed `"src"` to e.g.
`"/home/pir"`, it would print `/home/pir` instead.

### Exit Codes

If a shell command returns an exit code different from 0, then lein-shell will
try to exit Leiningen with the same exit code by default. This behaviour can be
overridden by setting the `:exit-code` option to `:ignore`. lein-shell will then
completely ignore the exit code and continue as if it was 0. By default is this
option set to `:default`.

As an example, see this setup below.

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :shell {:commands {"false" {:exit-code :ignore}}})
```

`lein shell false` will then happily return 0, whereas all other commands return
their true exit code.

### OS-Specific Subprocess Call

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

As may be evident, this replacement option is only possible for specific
commands, and is not something you can set in general.
