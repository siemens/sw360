ARG JAVA_VERSION=17
ARG UBUNTU_VERSION=jammy
ARG USERNAME=sw360
FROM eclipse-temurin:$JAVA_VERSION-jdk-$UBUNTU_VERSION

# sudo support
#RUN echo "$USERNAME ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/$USERNAME \
#   && chmod 0440 /etc/sudoers.d/$USERNAME

#cgpt
#RUN echo "$USERNAME ALL=(root) NOPASSWD:ALL" | tee /etc/sudoers.d/$USERNAME \
#    && chmod 0440 /etc/sudoers.d/$USERNAME

USER $USERNAME
RUN mkdir -p /app/sw360 \
    && mkdir -p /etc/sw360/rest \
    && chown -R $USERNAME:$USERNAME /app

WORKDIR /app/sw360

COPY . /app/sw360

EXPOSE 8091

RUN apt-get update && apt-get install -y maven

#Install library jasrs in local maven repo 
RUN mvn install:install-file \
    -Dfile=lib/commonIO-18.0.0-SNAPSHOT.jar \
    -DgroupId=org.eclipse.sw360 \
    -DartifactId=commonIO \
    -Dversion=18.0.0-SNAPSHOT \
    -Dpackaging=jar \
    && mvn install:install-file -Dfile=lib/datahandler-18.0.0-SNAPSHOT.jar \
    -DgroupId=org.eclipse.sw360 \
    -DartifactId=datahandler \
    -Dversion=18.0.0-SNAPSHOT \
    -Dpackaging=jar \
    && mvn install:install-file \
    -Dfile=lib/exporters-18.0.0-SNAPSHOT.jar \
    -DgroupId=org.eclipse.sw360 \
    -DartifactId=exporters \
    -Dversion=18.0.0-SNAPSHOT \
    -Dpackaging=jar \
    && mvn install:install-file \
    -Dfile=lib/importers-18.0.0-SNAPSHOT.jar \
    -DgroupId=org.eclipse.sw360 \
    -DartifactId=importers \
    -Dversion=18.0.0-SNAPSHOT \
    -Dpackaging=jar \
    && mvn clean package \
    -DskipTests \
    -Pfatjar
    
#RUN echo "127.0.0.1 sw360-keycloak sw360-kc sw360-keycloak-admin" > /etc/hosts.tmp \
#    && cat /etc/hosts >> /etc/hosts.tmp
#    && mv /etc/hosts.tmp /etc/hosts
    
#COPY --from=builder /etc/hosts.tmp /etc/hosts

# Add entries to /etc/hosts
#RUN echo "127.0.0.1 sw360-keycloak sw360-kc sw360-keycloak-admin" | sudo tee -a /etc/hosts
    
# Add entries to /etc/hosts
#RUN echo "127.0.0.1 sw360-keycloak sw360-kc sw360-keycloak-admin" >> /etc/hosts

ENTRYPOINT [ "java","-jar","/app/sw360/resource-server/target/resource.jar"]