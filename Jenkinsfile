pipeline {
    agent any

    environment { 
        REGISTRY = "registry-gorani.lab.terminal-lab.kr" 
        HARBOR_CREDENTIALS_ID = "harbor-auth" 
        PROJECT_NAME = "gorani" 
        IMAGE_NAME = "backend"
        DEV_SERVER = "10.0.1.79"
        REMOTE_USER = "rocky"
        FULL_IMAGE_TAG = "${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:${BUILD_NUMBER}"
        
    }

    stages {
        stage('1. Checkout') {
            steps { 
                git branch: 'master', url: 'https://github.com/ShinHanSWT1/DDEONAM_Be.git'
            }
        }

        stage('2. Build Image') {
            steps {
                script { 
                    sh "podman build -t ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:${BUILD_NUMBER} ." 
                    sh "podman tag ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:${BUILD_NUMBER} ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:latest"
                }
            }
        }

        stage('3. Push to Harbor') {
            steps { 
                withCredentials([usernamePassword(credentialsId: HARBOR_CREDENTIALS_ID, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "podman login ${REGISTRY} -u ${USER} -p ${PASS}"
                    sh "podman push ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:${BUILD_NUMBER} --tls-verify=false"
                    sh "podman push ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:latest --tls-verify=false"
                }
            }
        }

        stage('4. Cleanup') {
            steps { 
                sh "podman rmi ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:${BUILD_NUMBER}"
                sh "podman rmi ${REGISTRY}/${PROJECT_NAME}/${IMAGE_NAME}:latest"
            }
        }
        
        stage('5. Deploy Dev') {
            steps {
                sshagent(['dev-server-ssh']) {
                    withCredentials([usernamePassword(credentialsId: HARBOR_CREDENTIALS_ID, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        script { 
                            sh '''
                                ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${DEV_SERVER} << EOF 
                                    sudo podman login ${REGISTRY} -u "${USER}" -p "${PASS}" --tls-verify=false

                                    sudo podman pod exists gorani-pod || sudo podman pod create --name gorani-pod -p 8090:8090 -p 5432:5432

                                    if ! sudo podman ps -a --format "{{.Names}}" | grep -q "^dev-db$"; then
                                        sudo podman run -d --name db --pod gorani-pod \
                                        -e POSTGRES_DB=ddeonam_dev \
                                        -e POSTGRES_USER=postgres \
                                        -e POSTGRES_PASSWORD=gorani \
                                        docker.io/library/postgres:15
                                    fi

                                    if ! sudo podman ps -a --format "{{.Names}}" | grep -q "^dev-nginx$"; then
                                        sudo podman run -d --name dev-nginx --pod gorani-pod \
                                        -v /home/gorani/dev/nginx/nginx.conf:/etc/nginx/nginx.conf:Z \
                                        docker.io/library/nginx:latest
                                    fi

                                    sudo podman rm -f backend || true
                                    sudo podman pull ${FULL_IMAGE_TAG} --tls-verify=false
                                    
                                    sudo podman run -d --name backend \
                                        --pod gorani-pod \
                                        --restart always \
                                        -e SPRING_PROFILES_ACTIVE=dev \
                                        -e DB_URL=jdbc:postgresql://localhost:5432/ddeonam_dev \
                                        -e DB_HOST=localhost \
                                        -e DB_PORT=5432 \
                                        -e DB_NAME=ddeonam_dev \
                                        -e DB_USERNAME=postgres \
                                        -e DB_PASSWORD=gorani \
                                        ${FULL_IMAGE_TAG}
                                        
                                    sudo podman image prune -f
EOF
                            '''
                        }
                    }
                }
            }
        }
    }
}
