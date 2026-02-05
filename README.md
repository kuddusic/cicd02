# Java Hello World CI/CD to OpenShift

This repository contains a minimal Spring Boot REST API packaged into a container and deployed through a Jenkins pipeline to OpenShift.

## Stack choices
- **Language & Framework:** Java 17 with Spring Boot 3.x for a popular enterprise stack.
- **Build tool:** Apache Maven 3.9.
- **Container base:** Eclipse Temurin for both build (Maven) and runtime images.
- **CI/CD:** Jenkins LTS running inside Docker, deploying with the `oc` CLI.

## Project layout
- `hello-service/` – Spring Boot application plus Dockerfile.
- `Jenkinsfile` – Declarative Jenkins pipeline.
- `jenkins/docker-compose.yml` – Local Jenkins instance with Docker socket access.

## Build & run locally
```bash
cd hello-service
mvn spring-boot:run
# or build the image
docker build -t hello-world-service:local .
docker run -p 8080:8080 hello-world-service:local
```
> The Dockerfile now pins Debian-based Temurin images and sets `--platform` args so `docker buildx` works on both amd64 and arm64 hosts. Override `DOCKER_DEFAULT_PLATFORM` if you need a specific architecture.

## Bring up Jenkins locally
1. Ensure Docker Desktop/Engine is running and your user can reach `/var/run/docker.sock`.
2. Start Jenkins:
   ```bash
   cd jenkins
   docker compose up -d
   ```
3. Jenkins UI becomes available at [http://localhost:8081](http://localhost:8081). Default admin user is created automatically (check container logs for the password via `docker logs jenkins-demo`).
4. Install suggested plugins, plus **Docker**, **Pipeline**, and **OpenShift Client**.
5. Configure **Global Tool Configuration**:
   - Add a JDK named `temurin17` that points to your preferred Temurin 17 installation (Adoptium). On the Docker agent this is already present under `/opt/java/openjdk`.
   - Add a Maven installation named `maven3` (download automatically or point to `/usr/share/maven`).
6. Install the `oc` CLI inside the Jenkins container (one-time):
   ```bash
   docker exec -it jenkins-demo bash -lc 'curl -L https://mirror.openshift.com/pub/openshift-v4/clients/oc/latest/linux/oc.tar.gz | tar -xz -C /usr/local/bin oc'
   ```

## Credentials expected by the pipeline
Create the following credentials in **Manage Jenkins → Credentials**:
- `openshift-token` – *Secret text* containing an OpenShift API token (typically created for a service account with `system:image-puller` permissions).
- `openshift-image-registry` – *Username with password* is not required; store the registry host in the **Username** field (e.g., `image-registry.openshift-image-registry.svc:5000`) and any placeholder password. The pipeline uses this credential to inject the registry hostname.
- `openshift-pull-secret` – *Secret file or text* containing the base64 decoded pull secret JSON. The pipeline logs into the registry by piping this value to `docker login`.

Update the credential IDs inside `Jenkinsfile` if you choose different names.

## Jenkins pipeline stages
1. **Checkout** – sync repository.
2. **Unit Tests** – run `mvn test` and publish surefire reports.
3. **Package JAR** – build the Spring Boot fat JAR and archive it.
4. **Build Container Image** – `docker build` using `hello-service/Dockerfile` and tag with the Jenkins build number.
5. **Push to OpenShift** – authenticate with OpenShift, push the image to the internal registry, and either update an existing Deployment or create a new app and expose a Route/Service.

### Parameterizing deployments
The pipeline parameters let you change cluster information at run time:
- `OC_SERVER` – API server URL (e.g., `https://api.crc.testing:6443`).
- `OC_PROJECT` – Project/namespace.
- `OC_INSECURE` – Skip TLS validation for local clusters like CodeReady Containers.
- `HELLO_MESSAGE` – Runtime greeting injected as env var.

## Connecting Jenkins to OpenShift
1. **Create a service account** in your OpenShift project:
   ```bash
   oc new-project demo
   oc create sa jenkins-deployer
   oc policy add-role-to-user edit -z jenkins-deployer
   oc policy add-role-to-user system:image-puller system:serviceaccount:demo:default
   TOKEN=$(oc sa get-token jenkins-deployer)
   echo $TOKEN
   ```
   Store `TOKEN` inside the `openshift-token` credential.
2. **Expose the internal registry** (needed when running Jenkins outside cluster):
   ```bash
   oc registry info
   oc registry login
   ```
   Capture the registry host and the pull-secret JSON; store them in `openshift-image-registry` and `openshift-pull-secret` respectively.
3. **Verify access** by logging in from your workstation or Jenkins container:
   ```bash
   oc login https://api.example.openshift.com:6443 --token=<TOKEN> --insecure-skip-tls-verify=true
   oc whoami
   oc get projects
   ```
4. **Seed the deployment (optional)** – You can pre-create the OpenShift Deployment/Service so that Jenkins only updates the image:
   ```bash
   IMAGE=image-registry.openshift-image-registry.svc:5000/demo/hello-world-service:0
   oc new-app --name=hello-world-service --image=$IMAGE -e HELLO_MESSAGE="Hello OpenShift"
   oc expose service/hello-world-service
   ```

## Running the pipeline
1. Push this repository to a Git server reachable by Jenkins.
2. In Jenkins, create a Multibranch Pipeline or Pipeline job pointing at this repo.
3. Configure job-level parameters as needed (pipeline parameters are defined inside `Jenkinsfile`).
4. Trigger the build; upon success you can access the Route printed in the OpenShift console/logs.

## Troubleshooting tips
- Ensure the Jenkins container user (root) can execute `docker` commands via the mounted socket.
- When using `oc new-app`, the first rollout may take extra time because the image has to be pulled from the registry; monitor with `oc get events -w`.
- If your OpenShift cluster enforces self-signed certificates, keep `OC_INSECURE=true` or import the CA bundle into Jenkins.
