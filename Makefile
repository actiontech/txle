PROJECT_NAME  = utx
VERSION       = 0.3.0
DOCKER        := $(shell which docker)
DOCKER_IMAGE  := maven

docker_mvn_utx:
	$(DOCKER) run -v $(shell pwd)/:/opt/code --rm -w /opt/code $(DOCKER_IMAGE) /bin/bash -c "mvn clean package -DskipTests; mvn install -DskipTests -Pdev"