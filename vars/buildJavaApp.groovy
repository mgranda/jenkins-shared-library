def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
  agent {
    kubernetes {
      containerTemplate {
        name 'gradle'
        image 'gradle:4.10.0-jdk8'
        ttyEnabled true
        command 'cat'
      }
    }
  }
  
  /*triggers {
        bitbucketPush()
  }*/
parameters {
        choice(choices: 'US-EAST-1\nUS-WEST-2', description: 'What AWS region?', name: 'choiceExample')
    }

options {
	disableConcurrentBuilds()
	buildDiscarder(logRotator(numToKeepStr:'3', artifactNumToKeepStr: '3'))
	timeout(time: 30, unit: 'MINUTES')
}
  
  stages {
    stage('Pre Build') {
      steps {
        echo "Rama: ${env.BRANCH_NAME},  codigo de construccion: ${env.BUILD_ID} en ${env.JENKINS_URL}"
        echo 'Iniciando limpieza'
        sh 'gradle --console=plain clean -x check -x test'
      }
    }

    stage('Build') {
      steps {
        echo 'Iniciando construccion'
        script {
          if ( env.BRANCH_NAME == 'master' ) {
            echo "Iniciando construccion master"
            sh 'gradle --console=plain build --refresh-dependencies -x check -x test -Penv=dev'
          }else {
            echo "Iniciando construccion develop"
            sh 'gradle --console=plain build --refresh-dependencies -x check -x test -Penv=pro'
          }
        }

      }
    }

    stage('Test') {
      parallel {
        stage('Unit Test') {
          steps {
            script {
              try {
                echo "Iniciando analisis de calidad del repositorio de la rama ${env.BRANCH_NAME}"
                sh 'gradle test --no-daemon' //run a gradle task
              } catch(Exception e) {
                echo "Error en el analisis de calidad del repositorio de la rama ${env.BRANCH_NAME}"
              }
            }
          }
        }

        stage('SonarQube') {
          steps {
            echo "Iniciando Despliegue del repositorio de la rama ${env.BRANCH_NAME}"
          }
        }

      }
    }

    stage('Pre Build Docker') {
      steps {
        echo "Rama: ${env.BRANCH_NAME},  codigo de construccion: ${env.BUILD_ID} en ${env.JENKINS_URL}"
        echo 'Iniciando limpieza'
      }
    }

    stage('Build Docker') {
      steps {
        echo "Rama: ${env.BRANCH_NAME},  codigo de construccion: ${env.BUILD_ID} en ${env.JENKINS_URL}"
        echo 'Iniciando limpieza '
      }
    }

    stage('Deploy registry') {
      steps {
        echo "Rama: ${env.BRANCH_NAME},  codigo de construccion: ${env.BUILD_ID} en ${env.JENKINS_URL}"
        echo 'Iniciando limpieza '
      }
    }

    /*stage('Deploy stagging enviroment') {
      input {
        message 'Should we deploy the project?'
      }
      steps {
        echo 'Desplegando entorno'
      }
    }*/

  }
  post {
    always {
      echo "Pipeline finalizado del repositorio de la rama ${env.BRANCH_NAME} con el codigo de construccion ${env.BUILD_ID} en ${env.JENKINS_URL}"
      archiveArtifacts(artifacts: 'build/libs/**/*.jar', fingerprint: true)
      junit '**/build/test-results/test/*.xml'
    }

    success {
      echo 'La linea de construccion finalizo exitosamente'
    }

    failure {
      echo "La linea de construccion finalizo con errores ${currentBuild.fullDisplayName} with ${env.BUILD_URL}"
    }

    unstable {
      echo 'La linea de construccion finalizo de forma inestable'
    }

  }
}
}
