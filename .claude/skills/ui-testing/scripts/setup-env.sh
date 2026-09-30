#!/usr/bin/env bash
# Prepares a cloud container for UI verification: X display + window manager, Docker daemon, the
# `tea` CLI, and (if it already exists) the Gitea container. Safe to re-run after a container restart.
set -euo pipefail
STATE=${GITEA_UI_STATE:-$HOME/.gitea-ui}; mkdir -p "$STATE/shots" "$STATE/bin"

# Xvfb and Docker ship with the base image; the rest doesn't.
missing=0; for t in xdotool import xprop openbox; do command -v $t >/dev/null || missing=1; done
if [ $missing = 1 ]; then
  apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xdotool imagemagick x11-utils openbox fonts-dejavu >/dev/null
fi

if ! docker info >/dev/null 2>&1; then
  rm -f /var/run/docker.pid; (nohup dockerd > /tmp/dockerd.log 2>&1 &)
  for i in $(seq 1 30); do docker info >/dev/null 2>&1 && break; sleep 2; done
fi

if ! pgrep -x Xvfb >/dev/null; then
  rm -f /tmp/.X99-lock /tmp/.X11-unix/X99; (nohup Xvfb :99 -screen 0 1600x1000x24 > /tmp/xvfb.log 2>&1 &); sleep 3
fi
# Without a window manager, IDE dialogs open without focus and typing goes nowhere.
pgrep -x openbox >/dev/null || (DISPLAY=:99 nohup openbox > /tmp/openbox.log 2>&1 &)

# Static build, so it also runs inside the Alpine-based Gitea container (fixture.sh uses it there).
# Pinned: fixture.sh uses this version's commands (code.gitea.io/tea 1.x renamed them).
if [ ! -x "$STATE/bin/tea" ]; then
  (cd /tmp && CGO_ENABLED=0 GOBIN="$STATE/bin" go install gitea.dev/tea@v0.16.0)
fi

if docker start gitea >/dev/null 2>&1; then
  for i in $(seq 1 30); do curl -sf -m 3 http://localhost:3000/api/v1/version && echo && break; sleep 2; done
else
  echo "No Gitea container yet: run gitea-up.sh"
fi
