pipeline {
    agent any

    environment {
        RELEASE_TAG = "v0.${env.BUILD_NUMBER}"
    }

    stages {
        stage('Setup') {
             steps {
                 sh 'chmod +x mvnw'
             }
        }

        stage('Build') {
            steps {
                sh './mvnw clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                withCredentials([
                    string(credentialsId: 'JWT_SECRET_KEY', variable: 'JWT_SECRET_KEY'),
                    string(credentialsId: 'JWT_EXPIRATION_TIME', variable: 'JWT_EXPIRATION_TIME')
                ]) {
                    sh './mvnw test'
                }
            }
        }

        stage('Tag Release') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                withCredentials([string(credentialsId: 'github-pat', variable: 'GITHUB_TOKEN')]) {
                    sh 'git config user.email "jenkins@ci.local"'
                    sh 'git config user.name "Jenkins"'

                    sh 'git tag -a ${RELEASE_TAG} -m \'Release ${RELEASE_TAG} by Jenkins\''

                    sh 'git push https://x-oauth-basic:${GITHUB_TOKEN}@github.com/andrziv/JobHunt-Modulith.git --tags'
                }
            }
        }
    }

    post {
        success {
            echo 'Setting GitHub commit status to SUCCESS'
            step([
                $class: 'GitHubCommitStatusSetter',
                contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'Jenkins'],
                statusResultSource: [
                    $class: 'ConditionalStatusResultSource',
                    results: [
                        [$class: 'AnyBuildResult', state: 'SUCCESS', message: 'Build and tests passed! ✅']
                    ]
                ]
            ])
        }
        failure {
            echo 'Setting GitHub commit status to FAILURE'
            step([
                $class: 'GitHubCommitStatusSetter',
                contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'Jenkins'],
                statusResultSource: [
                    $class: 'ConditionalStatusResultSource',
                    results: [
                        [$class: 'AnyBuildResult', state: 'FAILURE', message: 'Build is on fire! 🔥🔥🔥 👎😤👎 🔥🔥🔥']
                    ]
                ]
            ])
        }
        always {
            cleanWs()
        }
    }
}