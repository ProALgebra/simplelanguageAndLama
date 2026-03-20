#!/usr/bin/env bash

curl -O https://www.antlr.org/download/antlr-4.13.2-complete.jar
$JAVA_HOME/bin/java -cp antlr-4.13.2-complete.jar org.antlr.v4.Tool -package com.oracle.truffle.sl.parser -no-listener -visitor language/src/main/java/com/oracle/truffle/sl/parser/SimpleLanguage.g4
