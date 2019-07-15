pipeline {
  agent {
    label 'golang'
  }
  stages {
    stage('clone') {
      steps {
        script {
          env.CODE_REPO = "http://10.0.0.15:31101/root/go-test-public"
          env.CREDENTIAL_ID = "global-credentials-gitlab"
          env.RELATIVE_DIRECTORY = "src"
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
    stage('golang') {
      steps {
        script {
          env.GOPATH = WORKSPACE
          dir(RELATIVE_DIRECTORY) {
            container('golang') {
              sh """go build"""
            }
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
    stage('deployService') {
      steps {
        script {
          env.CREDENTIAL_ID = "devops-dockercfg--devops--harbor"
          env.CREDENTIAL_ID = env.CREDENTIAL_ID.replaceFirst("devops-","")
          def tagwithcomma = "latest"
          def tags = tagwithcomma.split(",")
          env.NEW_IMAGE = "10.0.0.7:31104/library/helloworld1:${tags[0]}"
          container('tools') {
            timeout(time:300, unit: "SECONDS") {
              alaudaDevops.withCluster("devops") {
                alaudaDevops.withProject("devops-devops") {
                  def p = alaudaDevops.selector('deployment', 'helloworld').object()
                  p.metadata.labels['BUILD_ID']=env.BUILD_ID
                  for(container in p.spec.template.spec.containers) {
                    if(container.name == "helloworld") {
                      container.image = "${NEW_IMAGE}"
                      def cmd = ""
                      def args = ""
                      if(cmd!="") {
                        container.command = [cmd]
                      }
                      if(args!="") {
                        container.args = [args]
                      }
                      break
                    }
                  }
                  if(env.CREDENTIAL_ID != "") {
                    if(p.spec.template.spec.imagePullSecrets != null) {
                      def notFound = true
                      for(secret in p.spec.template.spec.imagePullSecrets) {
                        if(secret == env.CREDENTIAL_ID) {
                          notFound = false
                          break
                        }
                      }
                      if(notFound) {
                        p.spec.template.spec.imagePullSecrets[p.spec.template.spec.imagePullSecrets.size()] = [name: env.CREDENTIAL_ID]
                      }
                    }
                    else {
                      p.spec.template.spec.imagePullSecrets = [[name: env.CREDENTIAL_ID]]
                    }
                  }
                  alaudaDevops.apply(p, "--validate=false")
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
    buildDiscarder(logRotator(numToKeepStr: '200'))
  }
}
