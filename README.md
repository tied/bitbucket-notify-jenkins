# bitbucket-notify-jenkins
A Bitbucket Server plugin that notifies Jenkins when source code changes.

## Requirements

### Bitbucket
- Bitbucket Server 5.0 or later
- Jenkins certificate(s) must be imported into the Bitbucket JVM trust store

## Jenkins
- Jenkins 2
- Git Plugin

## Motivation
Jenkins provides the ability to poll Bitbucket for changes. This leads to inefficiencies when the 
build must occur quickly after the repository change. A better approach is for Bitbucket to send 
notifications to Jenkins as repository changes occur.

There are other plugins that provide similar functionality. However, this plugin is different
in the following ways:

- Geared towards larger corporate environments that have stricter policies and controls
  - limit the URLs that Bitbucket can communicate with
  - ensure the use of canonical clone URLs   
- Focus on simple and correct configuration
  - minimize user configuration/setup
  - support infrastructure automation through a configuration API and/or bitbucket.properties
- Intention to support multiple Jenkins notification schemes
  - Support Jenkins [Git Plugin](https://wiki.jenkins.io/display/JENKINS/Git+Plugin)
  - Support Jenkins [Bitbucket Branch Source plugin](https://wiki.jenkins.io/display/JENKINS/Bitbucket+Branch+Source+Plugin) 

## Features

### Jenkins URL Whitelist
Administrators must maintain a list of known and approved Jenkins instances. Users 
may configure sending notifications to only one of these instances.

### Standardized Clone URLs
Administrators can maintain a template for clone URLs across all repositories. Some mechanisms to 
notify Jenkins require knowledge of the EXACT clone URL configured within Jenkins. Because there 
may be different ways to access a repository, it can be helpful to standardize on a URL scheme. 

### Event Triggers
Repository administrators can select individual events that should trigger notifications. The supported 
events are:

- Branch Created
- Branch Deleted
- File Edit (from Bitbucket User Interface)
- Pull Request Merged
- Repository Push

### API
Provides an API to configure plugin and repository data. This can especially be useful
for establishing seed data during infrastructure automation.

## Notification Mechanics
The Notify Jenkins plugin requires the target Jenkins instance to have the [Git Plugin](https://wiki.jenkins.io/display/JENKINS/Git+Plugin) installed. 

The Jenkins Git plugin exposes an endpoint allowing unauthenticated requests to `https://jenkins/git/notifyCommit?url=<Clone URL of the Git repository>`. 
Upon receiving a request, Jenkins will trigger polling on any job configured with a Git clone 
URL matching the URL supplied in the request query string. If Jenkins polling subsequently finds changes,
the job(s) will begin processing.

Before sending the request to Jenkins, the Notify Jenkins plugin will determine the clone URL by substituting the configured
template URL with the `Project Key` and `Repo Slug` of the repository which sourced the event. For example: 

Given this template:
```
https://mybitbucket/scm/{{PROJECT_KEY}}/{{REPO_SLUG}}.git
```

and this project/repo:
```
Project Key -> MY_PROJECT
Repo Slug -> MY_REPO
```

Then the clone URL used in the notification will be:

```
https://mybitbucket/scm/MY_PROJECT/MY_REPO.git
```

## Usage

## Admin Setup
Once installed, the Bitbucket administrator must configure one or more Jenkins instances and a clone template URL. This can be
done by navigating to `Administration` and selecting `Notify Jenkins` from the `Settings` menu. The configured
instances will then be available for selection by the user when configuring a repository. 

## Repository Setup
To enable for a repository:

1. Navigate to `Repository Settings` -> `Notify Jenkins`
2. Check the `Status` checkbox
3. Select the `Jenkins Instance` to notify
4. Click `Save`

*Optional Settings*
- Select Specific Triggers


## API
This plugin extends the Bitbucket API by exposing the following resources:

### Plugin Configuration
Plugin Configuration data that applies to the entire installation.

#### Get Configuration
Returns the configuration data.

##### Authorization
Any authenticated user can request and view response.

##### Request
```
GET https://mybitbucket/rest/notify-jenkins/1.0/config
```

##### Response
```json
{
    "repoUrlPattern": "https://mybitbucket/scm/{{PROJECT_KEY}}/{{REPO_SLUG}}.git",
    "instances": [
        {
            "code": "PROD",
            "name": "Production",
            "url": "https://prod.myjenkins/jenkins"
        },
        {
            "code": "TEST",
            "name": "Test",
            "url": "https://test.myjenkins/jenkins"
        }
    ]
}
```

#### Advanced Configuration
In addition to the API and user interface, this plugin allows configuration via `bitbucket.properties`.
An example configuration is below:

``` 
...
plugin.com.mastercard.scm.bitbucket.notifyjenkins.repoUrlPattern=https://mybitbucket/scm/{{PROJECT_KEY}}/{{REPO_SLUG}}.git

plugin.com.mastercard.scm.bitbucket.notifyjenkins.instances[0].code=PROD
plugin.com.mastercard.scm.bitbucket.notifyjenkins.instances[0].name=Production
plugin.com.mastercard.scm.bitbucket.notifyjenkins.instances[0].url=https://prod.myjenkins/jenkins

plugin.com.mastercard.scm.bitbucket.notifyjenkins.instances[1].code=TEST
plugin.com.mastercard.scm.bitbucket.notifyjenkins.instances[1].name=Test
plugin.com.mastercard.scm.bitbucket.notifyjenkins.instances[1].url=https://test.myjenkins/jenkins
...
```

When configuring using this method, any configuration values set from the API or user interface will be 
overwritten with values from `bitbucket.properies` each time Bitbucket restarts or the Notify Jenkins
plugin restarts.

##### Authorization
User must be a system administrator.

##### Request
```
PUT https://mybitbucket/rest/notify-jenkins/1.0/config
```
```json
{
    "repoUrlPattern": "https://mybitbucket/scm/{{PROJECT_KEY}}/{{REPO_SLUG}}.git",
    "instances": [
        {
            "code": "PROD",
            "name": "Production",
            "url": "https://prod.myjenkins/jenkins"
        },
        {
            "code": "TEST",
            "name": "Test",
            "url": "https://test.myjenkins/jenkins"
        }
    ]
}
```

##### Response
Returns HTTP 204 (No Content) on success.


### Repository Configuration
Repository Configuration data that determines how to process notifications for the repository.

#### Get Repository Configuration
Get repository configuration data. 

##### Authorization
User must be a repository administrator.

##### Request
```
GET https://mybitbucket/rest/notify-jenkins/1.0/MY_PROJECT/MY_REPO/config
```

##### Response

```json
{
    "active": true,
    "jenkinsInstance": "PROD",
    "triggerRepoPush": true,
    "triggerBranchCreated": true,
    "triggerBranchDeleted": true,
    "triggerFileEdit": false,
    "triggerPRMerged": true
}
```

#### Save Repository Configuration
Save repository configuration data. 

##### Authorization
User must be a repository administrator.

##### Request
```
PUT https://mybitbucket/rest/notify-jenkins/1.0/MY_PROJECT/MY_REPO/config
```

```json
{
    "active": true,
    "jenkinsInstance": "PROD",
    "triggerRepoPush": true,
    "triggerBranchCreated": true,
    "triggerBranchDeleted": true,
    "triggerFileEdit": false,
    "triggerPRMerged": true
}
```

##### Response
Returns HTTP 204 (No Content) on success.

## File Configuration

## Developing

### Requirements

- Java 8
- Maven 3
- Atlassian SDK

### Setup
Install the [Atlassian SDK](https://developer.atlassian.com/docs/getting-started/set-up-the-atlassian-plugin-sdk-and-build-a-project)

### Build

```bash
mvn package
```

### Deploy
The following command uses Atlassian SDK on a Windows OS to deploy to an existing Bitbucket Server instance.

```bash
c:\Applications\Atlassian\atlassian-plugin-sdk-6.2.14\bin\atlas-install-plugin.bat --server myserver --http-port 8990 --username myuser --password mypassword --plugin-key com.mastercard.scm.notifyjenkins.bitbucket-notify-jenkins --context-path
```
