#!/bin/env bash

podman run --rm  -it -v "$PWD":/documents/:Z  asciidoctor/docker-asciidoctor make



