PROJECT_NAME  = txle
PROJECT_VERSION=2.19.11.0
DOCKER        := $(shell which docker)
DOCKER_IMAGE  := docker-registry:5000/maven:3.6.0-jdk-8

docker_mvn_txle:
	$(DOCKER) run -v $(shell pwd)/:/opt/code --rm -w /opt/code $(DOCKER_IMAGE) /bin/bash -c "mvn clean package -DskipTests; mvn install -DskipTests -Pdev"

upload_txle:
	curl -T $(shell pwd)/target/actiontech-txle-${PROJECT_VERSION}.tar.gz -u admin:ftpadmin ftp://release-ftpd/actiontech-${PROJECT_NAME}/qa/${PROJECT_VERSION}/actiontech-txle-${PROJECT_VERSION}.tar.gz

upload_jar_to_nexus:
	$(DOCKER) run -v $(shell pwd)/settings.xml:/usr/share/maven/conf/settings.xml --rm $(DOCKER_IMAGE) /bin/bash -c "mvn deploy -DskipTests -Pdev"

upload_jar_to_internet:
	$(DOCKER) run -v $(shell pwd)/settings-release.xml:/usr/share/maven/conf/settings.xml --rm $(DOCKER_IMAGE) /bin/bash -c "mvn deploy -DskipTests -Prelease"
