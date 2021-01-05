include etc/execute_env.sh

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
	docker exec -it c95d8ee23285 /bin/bash
docker.clean.rm:
	for i in `docker ps -a | awk '{print $$12}'`; do docker rm $$i; done
docker.clean.rmi:
	for i in `docker images | grep none | awk '{print $$3}'`; do docker rmi $$i; done

sam: sam.package sam.deploy
sam.build:
	sam build --profile ${PROFILE} --template ${TEMPLATE} --parameter-overrides ${PARAMS} --build-dir build --manifest requirements.txt --use-container
sam.package:
	aws s3 cp iac/swagger.yaml s3://${P_SWAGGER_BUCKET}/${P_SWAGGER_KEY}
	sam package -t ${TEMPLATE} --image-repository ${P_IMAGEURI} --output-template-file ${OUTPUT} --s3-bucket ${S3BUCKET}
sam.deploy:
	sam deploy -t ${OUTPUT} --stack-name ${STACK} --parameter-overrides ${PARAMS} --image-repository ${P_IMAGEURI} --capabilities CAPABILITY_NAMED_IAM

sam.local.invoke:
	sam local invoke -t ${TEMPLATE} --parameter-overrides ${PARAMS} --env-vars etc/envvars.json -e etc/event.json Fn | jq -r ".body" | jq
sam.local.api:
	sam local start-api -t ${TEMPLATE} --parameter-overrides ${PARAMS}
lambda.invoke:
	aws --profile ${PROFILE} lambda invoke --function-name ${FN} --invocation-type RequestResponse --payload file://etc/event.json --cli-binary-format raw-in-base64-out --log-type Tail tmp/fn.json | jq "." > tmp/response.json
	cat tmp/response.json | jq -r ".LogResult" | base64 --decode

test:
	$(eval P_SWAGGER_KEY=$(shell shasum -a 256 iac/swagger.yaml | awk '{print $$1}'))
clean:
	rm -rf build/*