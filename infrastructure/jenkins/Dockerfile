FROM jenkins/jenkins:lts-jdk21

USER root

# Install base packages
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    git \
    ca-certificates \
 && rm -rf /var/lib/apt/lists/*
 
#################################################
# Maven
#################################################

ARG MAVEN_VERSION=3.9.9

RUN curl -fsSL \
https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
-o maven.tar.gz \
 && tar -xzf maven.tar.gz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
 && rm maven.tar.gz


#################################################
# Docker CLI
#################################################

RUN curl -fsSL https://download.docker.com/linux/static/stable/x86_64/docker-28.3.3.tgz \
    -o docker.tgz \
 && tar -xzf docker.tgz \
 && mv docker/docker /usr/local/bin/ \
 && rm -rf docker docker.tgz

#################################################
# kubectl
#################################################

RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
 && install -m 0755 kubectl /usr/local/bin/kubectl \
 && rm kubectl

#################################################
# AWS CLI v2
#################################################

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip \
 && unzip awscliv2.zip \
 && ./aws/install \
 && rm -rf aws awscliv2.zip

#################################################
# Terraform
#################################################

ARG TERRAFORM_VERSION=1.13.1

RUN curl -Lo terraform.zip \
https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
 && unzip terraform.zip \
 && mv terraform /usr/local/bin/ \
 && rm terraform.zip

USER jenkins