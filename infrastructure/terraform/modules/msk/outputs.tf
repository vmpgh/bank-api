output "cluster_arn" {
  description = "MSK cluster ARN"
  value       = aws_msk_serverless_cluster.this.arn
}

output "cluster_uuid" {
  description = "MSK cluster UUID"
  value       = aws_msk_serverless_cluster.this.cluster_uuid
}

output "bootstrap_brokers_sasl_iam" {
  description = "MSK IAM bootstrap brokers"
  value       = aws_msk_serverless_cluster.this.bootstrap_brokers_sasl_iam
}

output "security_group_id" {
  description = "MSK security group ID"
  value       = aws_security_group.msk.id
}
