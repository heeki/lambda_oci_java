# Overview
This repository aims to provide sample code to demonstrate the deployment of a Java-based Lambda function as an OCI-compliant container image. 

## Execution
A makefile was created for convenience of execution. It depends on an `etc/execute_env.sh` file that contains the values for all of the necessary variables.

```bash
PROFILE=[AWS_CLI_PROFILE]
STACK=[AWS_CLOUDFORMATION_STACK_NAME]
TEMPLATE=[PATH/TO/TEMPLATE.YAML]
OUTPUT=[PATH/TO/OUTPUT.YAML]
S3BUCKET=[S3_BUCKET_NAME_FOR_SAM_PACKAGE]
P_STAGE=[API_GATEWAY_STAGE]
P_SWAGGER_BUCKET=[S3_BUCKET_NAME_FOR_SWAGGER_DEFINITIONS]
$(eval P_SWAGGER_KEY=$(shell shasum -a 256 iac/swagger.yaml | awk '{print $$1}'))

ACCOUNTID=[AWS_ACCOUNT_ID]
REGION=[AWS_REGION]
CIMAGE=[ECR_IMAGE_REPOSITORY_NAME]
CVERSION=[ECR_IMAGE_REPOSITORY_TAG]
P_TABLE=[DYNAMODB_TABLE_NAME]
P_TABLEARN=[DYNAMODB_TABLE_ARN]
P_IMAGEURI="${ACCOUNTID}.dkr.ecr.${REGION}.amazonaws.com/${CIMAGE}:${CVERSION}"
PARAMS="ParameterKey=apiStage,ParameterValue=${P_STAGE} ParameterKey=swaggerBucket,ParameterValue=${P_SWAGGER_BUCKET} ParameterKey=swaggerKey,ParameterValue=${P_SWAGGER_KEY} ParameterKey=imageUri,ParameterValue=${P_IMAGEURI}"

FN=[LAMBDA_FUNCTION_NAME_FOR_LOCAL_TESTING]
EXECID=[EXEC_ID_FOR_DOCKER_TESTING]
```

The process for compiling, building, and deploying the full stack is below.
```bash
make mvn.compile
make docker
make sam
```

## Local Testing
For testing locally, the jar-based and oci-based functions can be invoked with the following commands.
```bash
make sam.local.invoke.jar
make sam.local.invoke.oci
```