#!/bin/sh

# checkout all modules
mvn -q --non-recursive scm:bootstrap -Dmodule.name=etcd-java
mvn -q --non-recursive scm:bootstrap -Dmodule.name=document-service-client-java

# build collectors
mvn -q clean install
mvn -q clean package -Dmaven.test.skip=true

# cleanup
rm -rf etcd-java
rm -rf document-service-client-java
