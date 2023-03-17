# Overview
This repository aims to provide sample code to demonstrate the deployment of a Java-based Lambda function as an OCI-compliant container image. 

## Execution
A makefile was created for convenience of execution. It depends on an `etc/environment.sh` file that contains the values for all of the necessary variables.

```bash
PROFILE=aws-cli-profile
REGION=aws-region
S3BUCKET=aws-sam-cli-bucket-for-templates
JAVA_HOME=/usr/local/opt/openjdk@11
TARGET=target/oci-0.0.1-SNAPSHOT.jar

ACCOUNTID=aws-account-id
CIMAGE=ecr-repository-name
CVERSION=container-image-version-label

DDB_STACK=oci-dynamodb
DDB_TEMPLATE=iac/dynamodb.yaml
DDB_OUTPUT=iac/dynamodb_output.yaml
DDB_PARAMS="ParameterKey=name,ParameterValue=${P_STAGE}"
O_TABLE_ARN=output-arn-for-dynamodb-table

ECR_STACK=oci-ecr
ECR_TEMPLATE=iac/ecr.yaml
ECR_OUTPUT=iac/ecr_output.yaml
ECR_PARAMS="ParameterKey=pName,ParameterValue=${CIMAGE}"
O_REPOSITORY=output-arn-for-ecr-repository

P_IMAGEURI=${ACCOUNTID}.dkr.ecr.${REGION}.amazonaws.com/${CIMAGE}:${CVERSION}
P_STAGE=dev
APIGW_STACK=oci-apigw-lambda
APIGW_TEMPLATE=iac/apigw.yaml
APIGW_OUTPUT=iac/apigw_output.yaml
APIGW_PARAMS="ParameterKey=pApiStage,ParameterValue=${P_STAGE} ParameterKey=pImageUri,ParameterValue=${P_IMAGEURI} ParameterKey=pTableArn,ParameterValue=${O_TABLE_ARN}"
O_FN=output-arn-for-lambda-function
```

The process for compiling, building, and deploying the full stack is below.
```bash
make mvn.package
make docker
make apigw
```

## Local Testing
If you are using an AWS CLI profile that is not the default, you will need to point to the appropriate profile while testing: `export AWS_PROFILE=aws-cli-profile`.

For testing locally, `etc/envvars.json` first needs to be setup. You'll need to update the `TABLE_NAME` environment variable for each of the functions, and you'll need to get STS credentials by running `make local.sts`.

```json
{
    "Fn": {
        "TABLE_NAME": "dynamodb-table-name"
    },
    "FnOci": {
        "TABLE_NAME": "dynamodb-table-name",
        "AWS_ACCESS_KEY_ID": "access-key-from-output",
        "AWS_SECRET_ACCESS_KEY": "secret-key-from-output",
        "AWS_SESSION_TOKEN": "session-token-from-output",
        "AWS_REGION": "aws-region"
    }
}
```

Once `etc/envvars.json` is configured, the jar-based and oci-based functions can be invoked with the following commands.

```bash
// local test with jar
make mvn.package
make local.invoke.jar

// local test with oci
make docker
make local.invoke.oci
```

And lastly, you'll need to configure an `etc/event.json` payload that will be passed to the Lambda function. A sample event payload can be generated using SAM: `sam local generate-event apigateway aws-proxy`.