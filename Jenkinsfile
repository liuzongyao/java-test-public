pipeline {
    agent {
        label "jenkins-maven"
    }
    
    environment {
      MAVEN_OPTS="-Xms1024m -Xmx1024m"
    }
    
    stages{
        stage('Test Memory'){
            steps{
                sh 'java -XX:+PrintFlagsFinal -version | grep HeapSize'
                
                container('maven'){
                    sh 'mvn clean package'
                    
                    sh 'mvn -DjvmArgs="-Xmx1025m" clean'
                }
            }
        }
    }
}
