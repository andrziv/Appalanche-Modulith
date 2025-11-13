pipeline {
    agent any

    environment {
        RELEASE_TAG = "v0.${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'git@github.com:andrziv/JobHunt-Modulith.git',
                    credentialsId: 'github-repo-jobhunt-backend-key'

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
                sh './mvnw test'
            }
        }

        stage('Integration Test') {
            steps {
                script {
                    try {
                        sh 'docker-compose up -d db'

                        sh './mvnw verify'
                    } finally {
                        sh 'docker-compose down'
                    }
                }
            }
        }

        stage('Tag Release') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([string(credentialsId: 'github-pat', variable: 'GITHUB_TOKEN')]) {
                    sh 'git config user.email "jenkins@ci.local"'
                    sh 'git config user.name "Jenkins"'

                    sh "git tag -a ${RELEASE_TAG} -m 'Release ${RELEASE_TAG} by Jenkins'"

                    sh "git push https://x-oauth-basic:${GITHUB_TOKEN}@github.com/andrziv/JobHunt-Modulith.git --tags"
                }
            }
        }
    }

    post {
        success {
            setGitHubCommitStatus context: 'Jenkins', state: 'SUCCESS', message: 'Build and tests passed! ✅'
        }
        failure {
            setGitHubCommitStatus context: 'Jenkins', state: 'FAILURE', message: 'Build is on fire! 🔥🔥🔥 👎😤👎 🔥🔥🔥'
        }
        always {
            cleanWs()
        }
    }
}