pipeline {
  agent any
  tools {
    jdk 'temurin17'
    maven 'maven3'
  }
  environment {
    APP_NAME = 'hello-world-service'
    IMAGE_NAME = 'hello-world-service'
    JAVA_OPTS = '-Xms512m -Xmx512m'
  }
  parameters {
    string(name: 'OC_SERVER', defaultValue: 'https://api.ocp.local.kuddusi.cc:6443', description: 'OpenShift API URL')
    string(name: 'OC_PROJECT', defaultValue: 'demo', description: 'OpenShift project/namespace')
    booleanParam(name: 'OC_INSECURE', defaultValue: true, description: 'Skip TLS verify for self-signed clusters')
    choice(name: 'HELLO_MESSAGE', choices: ['Hello OpenShift', 'Hello from Jenkins', 'Hola Mundo'], description: 'Runtime greeting message')
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }
  stages {
    // stage('Checkout') {
    //   steps {
    //     checkout scm
    //   }
    // }
    stage('Unit Tests') {
      steps {
        dir('hello-service') {
          sh 'mvn -B test'
          archiveArtifacts artifacts: 'target/surefire-reports/*.xml', allowEmptyArchive: true
        }
      }
    }
    stage('Package JAR') {
      steps {
        dir('hello-service') {
          sh 'mvn -B package'
        }
        archiveArtifacts artifacts: 'hello-service/target/*.jar', fingerprint: true
      }
    }
    stage('Build Container Image') {
      steps {
        script {
          dockerImage = docker.build("${IMAGE_NAME}:${env.BUILD_NUMBER}", 'hello-service')
        }
      }
    }
    stage('Push to OpenShift') {
      environment {
        IMAGE_CREDENTIALS = credentials('openshift-pull-secret')
      }
      steps {
        withCredentials([
          string(credentialsId: 'openshift-token', variable: 'OC_TOKEN'),
          usernamePassword(credentialsId: 'openshift-image-registry', usernameVariable: 'REGISTRY_HOST', passwordVariable: 'REGISTRY_PASS')
        ]) {
          sh '''
          set -euo pipefail
          oc login ${OC_SERVER} --token=${OC_TOKEN} $( [ "${OC_INSECURE}" = "true" ] && echo '--insecure-skip-tls-verify=true' )
          oc project ${OC_PROJECT}
          IMAGE_TAG=${REGISTRY_HOST}/${IMAGE_NAME}:${BUILD_NUMBER}
          echo "Tagging ${IMAGE_NAME}:${BUILD_NUMBER} as ${IMAGE_TAG}"
          echo "${IMAGE_CREDENTIALS}" | docker login ${REGISTRY_HOST} --username unused --password-stdin
          docker tag ${IMAGE_NAME}:${BUILD_NUMBER} ${IMAGE_TAG}
          docker push ${IMAGE_TAG}
          if oc get deployment/${APP_NAME} >/dev/null 2>&1; then
            oc set image deployment/${APP_NAME} ${APP_NAME}=${IMAGE_TAG} --record=true
          else
            oc new-app --name=${APP_NAME} --image=${IMAGE_TAG} -e HELLO_MESSAGE="${HELLO_MESSAGE}"
            oc expose service/${APP_NAME}
          fi
          oc rollout status deployment/${APP_NAME}
          '''
        }
      }
    }
  }
  post {
    success {
      echo 'Hello World service deployed to OpenShift.'
    }
    failure {
      echo 'Build failed. Check stage logs.'
    }
  }
}
