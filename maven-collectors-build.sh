#!/bin/sh

#checkout all modules
mvn --non-recursive scm:bootstrap -Dmodule.name=jetcd-mods
mvn --non-recursive scm:bootstrap -Dmodule.name=document-service-client-java

# build collectors
cd ..
mvn clean install

rm -rf jetcd-mods
rm -rf document-service-client-java
