# AWS::CleanRooms::ConfiguredTable

Congratulations on starting development! Next steps:

1. Write the JSON schema describing your resource, `aws-cleanrooms-configuredtable.json`
1. Implement your resource handlers.

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.

## Testing individual handlers via SAM CLI

## cfn invoke

Sample Json Structure

	 {
	   "desiredResourceState": {
	     "configuredTableIdentifier": ""
	   },
	   "previousResourceState": {},
	   "logicalResourceIdentifier": "MyResource"
	 }



#### Create
`cfn invoke -v resource CREATE filename.json`

#### List (nextToken does not get populated, need to use sam local invoke)
`cfn invoke -v resource LIST filename.json`

#### Delete
`cfn invoke -v resource DELETE filename.json`

#### Update
`cfn invoke -v resource UPDATE filename.json`

#### Read
`cfn invoke -v resource READ filename.json`

## sam local invoke

Sample Json Structure

    {
        "credentials": {
            "accessKeyId": "",
            "secretAccessKey": "",
            "sessionToken": “”
        },
        "action": "LIST",
        "request": {
            "clientRequestToken": "",
            "desiredResourceState": {},
            "logicalResourceIdentifier": "",
            "nextToken": ""
        }
    }

Command

`sam local invoke TestEntrypoint -e filename.json`
