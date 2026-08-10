variable "repository_name" {

  description = "ECR repository name"

  type = string

}
variable "tags" {

  type = map(string)

}