#!/bin/bash

${JBOSS_HOME}/bin/jboss-cli.sh --file=${JBOSS_HOME}/extensions/vault.cli
${JBOSS_HOME}/bin/jboss-cli.sh --file=${JBOSS_HOME}/extensions/postgresql.cli
