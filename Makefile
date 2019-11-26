PROJECT_NAME  = txle
PROJECT_VERSION=2.19.11.0
DOCKER        := $(shell which docker)
DOCKER_IMAGE  := docker-registry:5000/actiontech/txle/maven:3.6.0

docker_mvn_txle:
	$(DOCKER) run -v $(shell pwd)/:/opt/code --rm -w /opt/code $(DOCKER_IMAGE) /bin/bash -c "mvn clean package -DskipTests; mvn deploy -DskipTests -Pdev -gs settings.xml"

docker_mvn_txle_release:
	$(DOCKER) run -v $(shell pwd)/:/opt/code --rm -w /opt/code $(DOCKER_IMAGE) /bin/bash -c "mvn clean package -DskipTests; mvn deploy -DskipTests -Prelease -gs settings-release.xml"

upload_txle:
	curl -T $(shell pwd)/target/actiontech-txle-${PROJECT_VERSION}.tar.gz -u admin:ftpadmin ftp://release-ftpd/actiontech-${PROJECT_NAME}/qa/${PROJECT_VERSION}/actiontech-txle-${PROJECT_VERSION}.tar.gz
