SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

MAVEN ?= mvn

.PHONY: help run test verify clean init env feature-start feature-push switch-develop switch-testing switch-main

help:
	@printf '%s
' 'Available targets:'
	@printf '%s
' '  make run                         Run the JavaFX app'
	@printf '%s
' '  make test                        Run unit tests'
	@printf '%s
' '  make verify                      Run the full Maven verification lifecycle'
	@printf '%s
' '  make clean                       Remove Maven build outputs'
	@printf '%s
' '  make init                        Configure project metadata and GitHub repository'
	@printf '%s
' '  make env                         Check local tooling'
	@printf '%s
' '  make feature-start name=my-work   Create a feature branch from develop'
	@printf '%s
' '  make feature-push                 Push current branch and open a PR to develop'

run:
	$(MAVEN) javafx:run

test:
	$(MAVEN) test

verify:
	$(MAVEN) verify

clean:
	$(MAVEN) clean

init:
	./init-project.sh

env:
	./scripts/check-environment.sh

switch-develop:
	git switch develop

switch-testing:
	git switch testing

switch-main:
	git switch main

feature-start:
	@test -n "$(name)" || (echo 'Usage: make feature-start name=my-change' >&2; exit 2)
	git switch develop
	git pull --ff-only || true
	git switch -c feature/$(name)

feature-push:
	@branch="$$(git branch --show-current)"; 	case "$$branch" in 		feature/*) git push -u origin "$$branch"; gh pr create --base develop --head "$$branch" --fill ;; 		*) echo 'Current branch must match feature/*' >&2; exit 2 ;; 	esac
