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
                withCredentials([
                    string(credentialsId: 'PG_DB_NAME', variable: 'PG_DB_NAME'),
                    string(credentialsId: 'PG_USERNAME', variable: 'PG_USERNAME'),
                    string(credentialsId: 'PG_PASSWORD', variable: 'PG_PASSWORD')
                ]) {
                    script {
                        try {
                            sh 'sudo docker compose up -d db'

                            sh """
                                ./mvnw verify \
                                -Dspring.datasource.url=jdbc:postgresql://localhost:8082/${PG_DB_NAME} \
                                -Dspring.datasource.username=${PG_USERNAME} \
                                -Dspring.datasource.password=${PG_PASSWORD}
                            """
                        } finally {
                            sh 'sudo docker compose down'
                        }
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