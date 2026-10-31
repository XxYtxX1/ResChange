#!/usr/bin/env sh
# Thin wrapper — calls the system Gradle directly.
# For a real project on another machine, generate a proper wrapper with:
#   gradle wrapper --gradle-version=8.7
exec gradle "$@"
