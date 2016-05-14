#!/bin/bash

mvn package

#java -DdebugOperations -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate \
#  -i .//swagger.json \
#  -l csharp

java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate \
    -i ./swagger.json \
    -l esphttpd \
    -o generated/esphttpd
