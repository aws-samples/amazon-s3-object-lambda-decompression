## A quick tour of this repository
* lambda-code : contains the source code for the six lambda functions, with each sub-directory containing the function itself, a unit-test file, a Makefile containing build instructions and a requirements.txt containing the libraries to be `pip install`ed
* template.yaml : contains the SAM template to build and launch the six lambda functions as well an an IAM role
* buildspec.yaml : contains instructions for AWS CodeBuild to build the functions. This is required to create a pipeline

## How to use this repository

1. Clone and navigate to the repository
2. Build the functions:
    ```
   sam build --use-container
    ```
   This pulls in the required packages and native libraries and creates the deployment packages in .aws-sam/build. It also runs the unit tests. Note the --use-container argument is necessary because the native libraries must be pulled in using a docker image resembling the runtime environment of AWS Lambda
   
3. (optional) Run the python unit tests in an environment that mimics the Lambda runtime environment
    ```
   docker run -it -v $(pwd):/mnt lambci/lambda:build-python3.7 /mnt/run_unit_tests
    ``` 
   The Java unit tests automatically get run with the build, but you can run them again via
    ```
   cd lambda-code/decompress_zip && mvn test && cd ../..
    ```
4. Deploy the stack:
    ```
   sam deploy --guided --template-file .aws-sam/build/template.yaml --capabilities CAPABILITY_NAMED_IAM
    ```
   The CAPABILITY_NAMED_IAM capability is needed to deploy any SAM stack or CloudFormation stack that creates IAM roles
   
(Clicking the "deploy" button on the Serverless Application Repository is equivalent to cloning and running the commands above)
