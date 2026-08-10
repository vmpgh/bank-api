pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build \
                        -t bank-api:${BUILD_NUMBER} \
                        .
                '''
            }

        }


        stage('Push to ECR') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-ecr']
                ]) {
                    sh '''
                        aws ecr get-login-password --region eu-central-1 \
                            | docker login \
                                --username AWS \
                                --password-stdin \
                                974284323479.dkr.ecr.eu-central-1.amazonaws.com

                        docker tag \
                            bank-api:${BUILD_NUMBER} \
                            974284323479.dkr.ecr.eu-central-1.amazonaws.com/bank-api/backend:${BUILD_NUMBER}

                        docker push \
                            974284323479.dkr.ecr.eu-central-1.amazonaws.com/bank-api/backend:${BUILD_NUMBER}
                    '''
                }
            }
        }

    }

    post {
        success {
            echo 'Build successful!'
        }

        failure {
            echo 'Build failed!'
        }
    }
}