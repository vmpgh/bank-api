output "endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.rds.address
}

output "port" {
  description = "RDS PostgreSQL port"
  value       = aws_db_instance.rds.port
}

output "database_name" {
  description = "Database name"
  value       = aws_db_instance.rds.db_name
}

output "security_group_id" {
  description = "RDS security group ID"
  value       = aws_security_group.rds.id
}

output "secret_arn" {
  description = "Secrets Manager ARN containing the RDS master credentials"
  value       = aws_db_instance.rds.master_user_secret[0].secret_arn
}