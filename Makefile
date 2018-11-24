PROJECT_NAME  = utx
VERSION       = 0.3.0-SNAPSHOT
DOCKER        := $(shell which docker)
DOCKER_IMAGE  := docker-registry:5000/maven:3.6.0-jdk-8

docker_mvn_utx:
	$(DOCKER) run -v $(shell pwd)/:/opt/code --rm -w /opt/code $(DOCKER_IMAGE) /bin/bash -c "mvn clean package -DskipTests; mvn install -DskipTests -Pdev"

upload_utx:
	curl -T $(shell pwd)/target/utx_${VERSION}.tar.gz -u admin:ftpadmin ftp://release-ftpd/actiontech-${PROJECT_NAME}/qa/${VERSION}/utx_${VERSION}.tar.gz
