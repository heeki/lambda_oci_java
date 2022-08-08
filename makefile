include etc/environment.sh

all: mvn.package docker sam

mvn.compile:
	mvn dependency:copy-dependencies -DincludeScope=compile
mvn.package:
	mvn package

docker: docker.build docker.login docker.tag docker.push
docker.build:
	docker build -f dockerfile.lambda -t ${CIMAGE}:${CVERSION} .
docker.login:
	aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACCOUNTID}.dkr.ecr.${REGION}.amazonaws.com
docker.tag:
	docker tag ${CIMAGE}:${CVERSION} ${ACCOUNTID}.dkr.ecr.${REGION}.amazonaws.com/${CIMAGE}:${CVERSION}
docker.push:
	docker push ${ACCOUNTID}.dkr.ecr.${REGION}.amazonaws.com/${CIMAGE}:${CVERSION}
docker.run:
	docker run -p 9000:8080 -e AWS_DEFAULT_REGION=${REGION} -e TABLE=${P_TABLE} ${CIMAGE}:${CVERSION}
docker.test:
	curl -s -XPOST -d @etc/event.json http://localhost:9000/2015-03-31/functions/function/invocations | jq -r ".body" | jq
docker.ssh:
	docker exec -it ${EXECID} /bin/bash
docker.clean.rm:
	for i in `docker ps -a | awk '{print $$12}'`; do docker rm $$i; done
docker.clean.rmi:
	for i in `docker images | grep none | awk '{print $$3}'`; do docker rmi $$i; done

ddb: ddb.package ddb.deploy
ddb.package:
	sam package -t ${DDB_TEMPLATE} --output-template-file ${DDB_OUTPUT} --s3-bucket ${S3BUCKET}
ddb.deploy:
	sam deploy -t ${DDB_OUTPUT} --stack-name ${DDB_STACK} --parameter-overrides ${DDB_PARAMS} --capabilities CAPABILITY_NAMED_IAM

api: api.package api.deploy
api.build:
	sam build --profile ${PROFILE} --template ${APIGW_TEMPLATE} --parameter-overrides ${APIGW_PARAMS} --build-dir build --manifest requirements.txt --use-container
api.package:
	sam package -t ${APIGW_TEMPLATE} --image-repository ${P_IMAGEURI} --output-template-file ${APIGW_OUTPUT} --s3-bucket ${S3BUCKET}
api.deploy:
	sam deploy -t ${APIGW_OUTPUT} --stack-name ${APIGW_STACK} --parameter-overrides ${APIGW_PARAMS} --image-repository ${P_IMAGEURI} --capabilities CAPABILITY_NAMED_IAM

local.invoke.jar:
	sam local invoke -t ${APIGW_TEMPLATE} --parameter-overrides ${APIGW_PARAMS} --env-vars etc/envvars.json -e etc/event.json Fn | jq -r ".body" | jq
local.invoke.oci:
	sam local invoke -t ${APIGW_TEMPLATE} --parameter-overrides ${APIGW_PARAMS} --env-vars etc/envvars.json -e etc/event.json FnOci | jq -r ".body" | jq
local.api:
	sam local start-api -t ${APIGW_TEMPLATE} --parameter-overrides ${APIGW_PARAMS} --warm-containers LAZY
lambda.invoke:
	aws --profile ${PROFILE} lambda invoke --function-name ${O_FN} --invocation-type RequestResponse --payload file://etc/event.json --cli-binary-format raw-in-base64-out --log-type Tail tmp/fn.json | jq "." > tmp/response.json
	cat tmp/response.json | jq -r ".LogResult" | base64 --decode

test:
	$(eval P_SWAGGER_KEY=$(shell shasum -a 256 iac/swagger.yaml | awk '{print $$1}'))
clean:
	rm -rf build/*