# Contributing

Hello reader!

We would love to get contributors for lein-shell, and all the guidelines below
is not an attempt to scare you away. As long as you're interested in
contributing, we would love to hear from you. The reason we have these
guidelines in place is to have more efficient communication between
contributors, which means that we can merge in your patch faster, or understand
and fix the bug you found more rapidly.

If you're unsure whether you follow the guidelines or not, just send it. We're
not picky, and as mentioned earlier, these guidelines is not there to scare you
away.

## Issues

If you found a bug, have an idea for improvement, a question or something else
you believe consitutes to an issue, we'd love it if you report this on the
GitHub issue tracker. Sending bug reports to personal email addresses is
inappropriate.

If you think you've hit on a bug, it would be great if you could include the
following information (if it makes sense to include it):

* What (small set of) steps will reproduce the problem?
* What is the expected output? What do you see instead?
* What version are you using?

## Patches

Patches are preferred as Github pull requests. Use topic branches instead of
commiting directly to master, to avoid unnecessary merge clutter. It is
preferred that commit messages keeps the following style:

* First line is 50 characters or less
* Then a blank line
* Remaining text should be wrapped at 72 characters

As an example, this would be preferable:

```bash
# Fork the project off Github
$ git clone git@github.com:your-username/lein-shell.git
$ cd lein-shell
$ git checkout -b my-patch
# Do your changes now, and stage them
$ lein test
$ git commit -m "I've fixed this and that, fixes #42."
$ git push
# Submit a pull request
```

## Code style

Try to be aware of the conventions in the existing code. Make a reasonable
attempt to avoid lines longer than 80 columns or function bodies longer than 20
lines. Don't use `when` unless it's for side-effects.

## Testing

Before you're asking for a pull request, we would be very happy if you ensure
that the changes you've done doesn't break any of the existing test cases.
Patches which add test coverage for the functionality they change are especially
welcome, but this is not necessary.

To run the test cases, run `lein test` in the root directory.
