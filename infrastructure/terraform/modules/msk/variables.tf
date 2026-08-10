variable "name" {
  description = "MSK Serverless cluster name"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for MSK"
  type        = list(string)
}

variable "tags" {
  description = "Common tags"
  type        = map(string)
}
