variable "cluster_name" {

  description = "EKS cluster name"

  type = string

}


variable "cluster_version" {

  description = "Kubernetes version"

  type = string

  default = "1.33"

}


variable "vpc_id" {

  description = "VPC ID"

  type = string

}


variable "private_subnet_ids" {

  description = "Private subnet IDs"

  type = list(string)

}


variable "tags" {

  description = "Common tags"

  type = map(string)

}