variable "name" {
  description = "RDS instance name"
  type        = string
}

variable "database_name" {
  description = "Initial PostgreSQL database name"
  type        = string
}

variable "username" {
  description = "Master username"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the RDS subnet group"
  type        = list(string)
}

variable "tags" {
  description = "Common tags"
  type        = map(string)
}