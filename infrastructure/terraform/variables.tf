variable "project_name" {
  default = "bank-api"
}

variable "environment" {
  default = "prod"
}

variable "aws_region" {
  default = "eu-central-1"
}
variable "vpc_cidr" {
  description = "CIDR block for the VPC"

  type = string

  default = "10.0.0.0/16"
}
variable "ecr_repository_name" {
  description = "Name of the ECR repository"

  type = string

  default = "bank-api/backend"
}