#!/bin/sh

#checkout all modules
mvn --non-recursive scm:checkout -Dmodule.name=jetcd
mvn --non-recursive scm:bootstrap -Dmodule.name=document-service-client-java

# build jetcd with gradle
cd jetcd
./gradlew install

# build collectors
cd ..
mvn clean install

rm -rf jetcd
rm -rf document-service-client-java
