resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.name}-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = var.tags
}

resource "aws_security_group" "redis" {
  name        = "${var.name}-sg"
  description = "Security group for ${var.name} Redis"
  vpc_id      = var.vpc_id

  tags = var.tags
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_eks" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = var.eks_node_security_group_id

  from_port = 6379
  to_port   = 6379
  ip_protocol = "tcp"

  description = "Allow Redis access from EKS nodes"
}

resource "aws_vpc_security_group_egress_rule" "redis" {
  security_group_id = aws_security_group.redis.id

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = var.name
  description          = "Redis for Bank API"

  engine         = "valkey"
  engine_version = "8.2"

  node_type            = "cache.t4g.micro"
  num_cache_clusters   = 1

  port = 6379

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  automatic_failover_enabled = false
  multi_az_enabled            = false

  at_rest_encryption_enabled = true
  transit_encryption_enabled  = true

  tags = var.tags
}