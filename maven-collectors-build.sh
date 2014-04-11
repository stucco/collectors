#!/bin/sh

#checkout all modules
mvn --non-recursive scm:bootstrap -Dmodule.name=etcd-java
mvn --non-recursive scm:bootstrap -Dmodule.name=document-service-client-java

# build collectors
mvn clean install

rm -rf etcd-java
rm -rf document-service-client-java
