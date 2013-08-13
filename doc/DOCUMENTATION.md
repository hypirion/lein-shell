# Documentation

lein-shell is a plugin for running shell commands. Sometimes, you just need to
be able to run some sort of setup tool not available from the JVM, or which does
not (yet?) have a Leiningen plugin which does the job.

Generally speaking, shell commands are straightforward. However, sometimes you
would like to tweak the setup a bit. Perhaps you would like to change the
working directory, or maybe you want to ignore the exit code. lein-shell aims to
be configurable enough that such things can be done through modifications in the
`project.clj` only.

## Basic Usage

TODO: hrm.

## A Word About prep-tasks

TODO: How does it work?

## Environment variables and directory specification

printenv, pwd

## Exit codes

false

## OS-specific subprocess call

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
