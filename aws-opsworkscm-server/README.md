# How to test this code
* Make sure it builds with `mvn package`
* Try it manually:
    1. Set up SAM (see "Setting up Testing" in https://quip-amazon.com/zgqfAWaCPUxh/CloudFormation-Uluru-Onboarding#TTM9CA2VAxl)
    2. Start the lambda locally with `sam local start-lambda`
    3. Invoke the lambda using `sudo sam local invoke "TestEntrypoint" -e sam-tests/FILE.json`
        * replace `FILE.json` with the handler and execution stage you want to test (e.g. `create-execute.json`)
        * before this json file works, you need to add proper credentials and roles to the file

* Use CloudFormation Handler Contracts with `cfn test`. More on that here: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test.html

____________________
# Information from CFN team

## AWS::OpsWorksCM::Server

Congratulations on starting development! Next steps:

1. Write the JSON schema describing your resource, `aws-opsworkscm-server.json`
2. The RPDK will automatically generate the correct resource model from the
   schema whenever the project is built via Maven. You can also do this manually
   with the following command: `cfn generate`
3. Implement your resource handlers


Please don't modify files under `target/generated-sources/rpdk`, as they will be
automatically overwritten.

The code use [Lombok](https://projectlombok.org/), and [you may have to install
IDE integrations](https://projectlombok.org/) to enable auto-complete for
Lombok-annotated classes.
