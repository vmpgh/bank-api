module "networking" {

  source = "./modules/networking"

  project_name = var.project_name

  environment = var.environment

  vpc_cidr = var.vpc_cidr

}
module "ecr" {

  source = "./modules/ecr"

  repository_name = var.ecr_repository_name

  tags = local.common_tags

}
module "eks" {

  source = "./modules/eks"


  cluster_name = "${local.name_prefix}-eks"


  vpc_id = module.networking.vpc_id


  private_subnet_ids = module.networking.private_subnets


  tags = local.common_tags

}
module "rds" {
  source = "./modules/rds"

  name = "${local.name_prefix}-postgres"

  database_name = "bankdb"

  username = "bankadmin"

  vpc_id = module.networking.vpc_id

  private_subnet_ids = module.networking.private_subnets

  tags = local.common_tags
}
module "msk" {
  source = "./modules/msk"

  name = "${local.name_prefix}-msk"

  vpc_id = module.networking.vpc_id

  vpc_cidr = var.vpc_cidr

  private_subnet_ids = module.networking.private_subnets

  tags = local.common_tags
}
module "redis" {
  source = "./modules/redis"

  name = "${local.name_prefix}-redis"

  vpc_id = module.networking.vpc_id

  private_subnet_ids = module.networking.private_subnets

  eks_node_security_group_id = module.eks.node_security_group_id

  tags = local.common_tags
}
