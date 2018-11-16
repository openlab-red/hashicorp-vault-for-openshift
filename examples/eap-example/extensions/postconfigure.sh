#!/bin/bash

${JBOSS_HOME}/bin/jboss-cli.sh --file=${JBOSS_HOME}/extensions/vault.cli

# Using dynamic -ds.xml
# ${JBOSS_HOME}/bin/jboss-cli.sh --file=${JBOSS_HOME}/extensions/postgresql.cli
