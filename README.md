

# Update image Maven plugin
## What is it used for?

It is used to automate the next pipeline:

`src` → `.jar` → `docker image` → artifactory → OpenShift

1. run `maven package` and build spring boot app (or whatever you need)
2. build docker image and push it into a repository
3. update docker image in the cloud (currently in OpenShift)

Actually, the plugin itself automates steps 2 and 3.

## How to use 
### With maven
#### What you really need:
* Type: `mvn clean package -DskipTests com.github.strakhova:update-image-maven-plugin:update` to make full update

#### May be useful also 
* Type: `mvn clean compile jar:jar org.springframework.boot:spring-boot-maven-plugin:repackage com.github.strakhova:update-image-maven-plugin:update` 
  to make full update (fastest command, but works only with Spring Boot projects)
* Type: `mvn com.github.strakhova:update-image-maven-plugin:update` to make partial update (without generating new jar)
* Type: `mvn com.github.strakhova:update-image-maven-plugin:build-push` to make build and push (without generating new jar)

### With Intellij IDEA
1. Add Configuration
2. Create Maven Run Configuration
3. Command line:
  `-N com.github.strakhova:update-image-maven-plugin:update`
4. Before launch: 
  `clean package -DskipTests`
 


## Configuration
Add required properties 
* in `{project root}/../config.yaml` (most preferable way)
* or in `{project root}/gitignore/config.yaml` (add `{project root}/gitignore` to `.gitignore` file then)

[See default config](./src/main/resources/config/default.yaml)
```
docker:
  host: "tcp://127.0.0.1:2375"
  username: "user121"
  authorization: "password"
artifactory:
  url: "artifactory.xxxx.com:xxx/"
  repository: "xx.docker.dev/"
cloudProvider:
  type: "kubernetes"
  url: "***"
  namespace: "dev1"
  deployment: "some-service"
  authorizationToken: "***"
  configPath: "c:\\Users\\user121\\.kube\\config"
```

Note that properties with "!!!" are required and "***" are optional.

## Prerequisites

* Install Docker (Docker desktop for Windows) and run it
* Add configuration file as described in *Configuration* section

## Additionally

You can additionally use tcp protocol. Use "tcp://127.0.0.1:2375" in `docker.host` property.

After that you must enable 'Docker without TLS' (`Docker` -> `Settings` -> `General` -> `Expose daemon on tcp://localhost:2375 without tls`) 
in Windows or something similar in macOS.
