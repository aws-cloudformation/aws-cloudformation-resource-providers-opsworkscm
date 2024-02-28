# AWS::OpsWorksCM::Server

Resource Type definition for AWS::OpsWorksCM::Server

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::OpsWorksCM::Server",
    "Properties" : {
        "<a href="#keypair" title="KeyPair">KeyPair</a>" : <i>String</i>,
        "<a href="#engineversion" title="EngineVersion">EngineVersion</a>" : <i>String</i>,
        "<a href="#servicerolearn" title="ServiceRoleArn">ServiceRoleArn</a>" : <i>String</i>,
        "<a href="#disableautomatedbackup" title="DisableAutomatedBackup">DisableAutomatedBackup</a>" : <i>Boolean</i>,
        "<a href="#backupid" title="BackupId">BackupId</a>" : <i>String</i>,
        "<a href="#enginemodel" title="EngineModel">EngineModel</a>" : <i>String</i>,
        "<a href="#preferredmaintenancewindow" title="PreferredMaintenanceWindow">PreferredMaintenanceWindow</a>" : <i>String</i>,
        "<a href="#associatepublicipaddress" title="AssociatePublicIpAddress">AssociatePublicIpAddress</a>" : <i>Boolean</i>,
        "<a href="#instanceprofilearn" title="InstanceProfileArn">InstanceProfileArn</a>" : <i>String</i>,
        "<a href="#customcertificate" title="CustomCertificate">CustomCertificate</a>" : <i>String</i>,
        "<a href="#preferredbackupwindow" title="PreferredBackupWindow">PreferredBackupWindow</a>" : <i>String</i>,
        "<a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#customdomain" title="CustomDomain">CustomDomain</a>" : <i>String</i>,
        "<a href="#customprivatekey" title="CustomPrivateKey">CustomPrivateKey</a>" : <i>String</i>,
        "<a href="#engineattributes" title="EngineAttributes">EngineAttributes</a>" : <i>[ <a href="engineattribute.md">EngineAttribute</a>, ... ]</i>,
        "<a href="#backupretentioncount" title="BackupRetentionCount">BackupRetentionCount</a>" : <i>Integer</i>,
        "<a href="#instancetype" title="InstanceType">InstanceType</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#engine" title="Engine">Engine</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::OpsWorksCM::Server
Properties:
    <a href="#keypair" title="KeyPair">KeyPair</a>: <i>String</i>
    <a href="#engineversion" title="EngineVersion">EngineVersion</a>: <i>String</i>
    <a href="#servicerolearn" title="ServiceRoleArn">ServiceRoleArn</a>: <i>String</i>
    <a href="#disableautomatedbackup" title="DisableAutomatedBackup">DisableAutomatedBackup</a>: <i>Boolean</i>
    <a href="#backupid" title="BackupId">BackupId</a>: <i>String</i>
    <a href="#enginemodel" title="EngineModel">EngineModel</a>: <i>String</i>
    <a href="#preferredmaintenancewindow" title="PreferredMaintenanceWindow">PreferredMaintenanceWindow</a>: <i>String</i>
    <a href="#associatepublicipaddress" title="AssociatePublicIpAddress">AssociatePublicIpAddress</a>: <i>Boolean</i>
    <a href="#instanceprofilearn" title="InstanceProfileArn">InstanceProfileArn</a>: <i>String</i>
    <a href="#customcertificate" title="CustomCertificate">CustomCertificate</a>: <i>String</i>
    <a href="#preferredbackupwindow" title="PreferredBackupWindow">PreferredBackupWindow</a>: <i>String</i>
    <a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>: <i>
      - String</i>
    <a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
    <a href="#customdomain" title="CustomDomain">CustomDomain</a>: <i>String</i>
    <a href="#customprivatekey" title="CustomPrivateKey">CustomPrivateKey</a>: <i>String</i>
    <a href="#engineattributes" title="EngineAttributes">EngineAttributes</a>: <i>
      - <a href="engineattribute.md">EngineAttribute</a></i>
    <a href="#backupretentioncount" title="BackupRetentionCount">BackupRetentionCount</a>: <i>Integer</i>
    <a href="#instancetype" title="InstanceType">InstanceType</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#engine" title="Engine">Engine</a>: <i>String</i>
</pre>

## Properties

#### KeyPair

_Required_: No

_Type_: String

_Maximum Length_: <code>10000</code>

_Pattern_: <code>.*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### EngineVersion

_Required_: No

_Type_: String

_Maximum Length_: <code>10000</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### ServiceRoleArn

_Required_: Yes

_Type_: String

_Maximum Length_: <code>10000</code>

_Pattern_: <code>arn:aws:iam::[0-9]{12}:role/.*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### DisableAutomatedBackup

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### BackupId

_Required_: No

_Type_: String

_Maximum Length_: <code>79</code>

_Pattern_: <code>[a-zA-Z][a-zA-Z0-9\-\.\:]*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### EngineModel

_Required_: No

_Type_: String

_Maximum Length_: <code>10000</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### PreferredMaintenanceWindow

_Required_: No

_Type_: String

_Maximum Length_: <code>10000</code>

_Pattern_: <code>^((Mon|Tue|Wed|Thu|Fri|Sat|Sun):)?([0-1][0-9]|2[0-3]):[0-5][0-9]$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AssociatePublicIpAddress

_Required_: No

_Type_: Boolean

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### InstanceProfileArn

_Required_: Yes

_Type_: String

_Maximum Length_: <code>10000</code>

_Pattern_: <code>arn:aws:iam::[0-9]{12}:instance-profile/.*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### CustomCertificate

_Required_: No

_Type_: String

_Maximum Length_: <code>2097152</code>

_Pattern_: <code>(?s)\s*-----BEGIN CERTIFICATE-----.+-----END CERTIFICATE-----\s*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### PreferredBackupWindow

_Required_: No

_Type_: String

_Maximum Length_: <code>10000</code>

_Pattern_: <code>^((Mon|Tue|Wed|Thu|Fri|Sat|Sun):)?([0-1][0-9]|2[0-3]):[0-5][0-9]$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SecurityGroupIds

_Required_: No

_Type_: List of String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SubnetIds

_Required_: No

_Type_: List of String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### CustomDomain

_Required_: No

_Type_: String

_Maximum Length_: <code>253</code>

_Pattern_: <code>^(((?!-)[A-Za-z0-9-]{0,62}[A-Za-z0-9])\.)+((?!-)[A-Za-z0-9-]{1,62}[A-Za-z0-9])$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### CustomPrivateKey

_Required_: No

_Type_: String

_Maximum Length_: <code>4096</code>

_Pattern_: <code>(?ms)\s*^-----BEGIN (?-s:.*)PRIVATE KEY-----$.*?^-----END (?-s:.*)PRIVATE KEY-----$\s*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### EngineAttributes

_Required_: No

_Type_: List of <a href="engineattribute.md">EngineAttribute</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### BackupRetentionCount

_Required_: No

_Type_: Integer

_Minimum Length_: <code>1</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### InstanceType

_Required_: Yes

_Type_: String

_Maximum Length_: <code>10000</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Engine

_Required_: No

_Type_: String

_Maximum Length_: <code>10000</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ServerName.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ServerName

Returns the <code>ServerName</code> value.

#### Endpoint

Returns the <code>Endpoint</code> value.

#### Arn

Returns the <code>Arn</code> value.
