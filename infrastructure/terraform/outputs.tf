output "vpc_id" {

  value = module.networking.vpc_id

}

output "private_subnets" {

  value = module.networking.private_subnets

}

output "public_subnets" {

  value = module.networking.public_subnets

}
output "ecr_repository_name" {

  value = module.ecr.repository_name

}


output "ecr_repository_url" {

  value = module.ecr.repository_url

}
output "eks_cluster_name" {

  value = module.eks.cluster_name

}


output "eks_cluster_endpoint" {

  value = module.eks.cluster_endpoint

}
output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_port" {
  value = module.rds.port
}

output "rds_database_name" {
  value = module.rds.database_name
}

output "rds_security_group_id" {
  value = module.rds.security_group_id
}

output "rds_secret_arn" {
  value = module.rds.secret_arn
}
output "msk_cluster_arn" {
  value = module.msk.cluster_arn
}

output "msk_cluster_uuid" {
  value = module.msk.cluster_uuid
}

output "msk_bootstrap_brokers_sasl_iam" {
  value = module.msk.bootstrap_brokers_sasl_iam
}

output "msk_security_group_id" {
  value = module.msk.security_group_id
}
output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = module.redis.endpoint
}

output "redis_port" {
  description = "Redis port"
  value       = module.redis.port
}

output "redis_security_group_id" {
  description = "Redis security group ID"
  value       = module.redis.security_group_id
}

