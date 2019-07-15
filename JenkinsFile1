pipeline {
  agent {
    label 'java'
  }
  stages {
    stage('clone') {
      steps {
        script {
          env.CODE_REPO = "https://github.com/liuzongyao/java-test-public"
          env.CREDENTIAL_ID = "devops-github"
          env.RELATIVE_DIRECTORY = "."
          env.BRANCH = "master"
          def scmVars = checkout([
            $class: 'GitSCM',
            branches: [[name: "${BRANCH}"]],
            extensions: [[
              $class: 'SubmoduleOption',
              recursiveSubmodules: true,
              reference: '',
            ],[
              $class: 'RelativeTargetDirectory',
              relativeTargetDir: "${RELATIVE_DIRECTORY}"
            ]],
            userRemoteConfigs: [[
              credentialsId: "${CREDENTIAL_ID}",
              url: "${CODE_REPO}"
            ]]
          ])
          env.GIT_COMMIT = scmVars.GIT_COMMIT
          env.GIT_BRANCH = scmVars.GIT_BRANCH
          env.GIT_BRANCH_AS_TAG = scmVars.GIT_BRANCH.replaceFirst("origin/","").replaceAll("/","-")
        }

      }
    }
    stage('maven') {
      steps {
        script {
          container('java') {
            sh """mvn clean package"""
          }
        }

      }
    }
    stage('build-docker') {
      steps {
        script {
          def retryCount = 3
          def repositoryAddr = '10.0.0.7:31104/library/helloworld1'.replace("http://","").replace("https://","")
          env.IMAGE_REPO = repositoryAddr
          def credentialId = ''
          credentialId = "devops-dockercfg--devops--harbor"
          dir(RELATIVE_DIRECTORY) {
            container('tools') {
              retry(retryCount) {
                try {
                  if (credentialId != '') {
                    withCredentials([usernamePassword(credentialsId: "${credentialId}", passwordVariable: 'PASSWD', usernameVariable: 'USER')]) {
                      sh "docker login ${IMAGE_REPO} -u ${USER} -p ${PASSWD}"
                    }
                  }
                }
                catch(err) {
                  echo err.getMessage()
                  alaudaDevops.withCluster() {
                    def secretNamespace = "devops"
                    def secretName = "dockercfg--devops--harbor"
                    def secret = alaudaDevops.selector( "secret/${secretName}" )
                    alaudaDevops.withProject( "${secretNamespace}" ) {
                      def secretjson = secret.object().data['.dockerconfigjson']
                      def dockerconfigjson = base64Decode("${secretjson}");
                      writeFile file: 'config.json', text: dockerconfigjson
                      sh """
                      set +x
                      mkdir -p ~/.docker
                      mv -f config.json ~/.docker/config.json
                      """
                    }
                  }
                }
                def tagswithcomma = "latest"
                def tags = tagswithcomma.split(",")
                def incubatorimage = "${IMAGE_REPO}:${tags[0]}"
                sh " docker build -t ${incubatorimage} -f Dockerfile  ."
                tags.each {
                  tag ->
                  sh """
                  docker tag ${incubatorimage} ${IMAGE_REPO}:${tag}
                  docker push ${IMAGE_REPO}:${tag}
                  """
                }
                if (credentialId != '') {
                  sh "docker logout ${IMAGE_REPO}"
                }
              }
            }
          }
        }

      }
    }
  }
  environment {
    ALAUDA_PROJECT = 'devops'
  }
  post {
    always {
      script {
        echo "clean up workspace"
        deleteDir()
      }


    }

  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '300'))
  }
}
