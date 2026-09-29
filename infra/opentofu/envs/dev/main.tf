# dev: separate remote state per environment so an apply here can never touch the other environment.
terraform {
  required_version = ">= 1.7"
  required_providers {
    aws    = { source = "hashicorp/aws", version = "~> 5.70" }
    random = { source = "hashicorp/random", version = "~> 3.6" }
  }
  backend "s3" {
    bucket         = "cardflow-showcase-tfstate-dev" # create once, out-of-band
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "cardflow-tf-locks"
    encrypt        = true
  }
}

variable "region" {
  type    = string
  default = "us-east-1"
}
variable "snowflake_iam_user_arn" {
  type    = string
  default = ""
}
variable "snowflake_external_id" {
  type    = string
  default = ""
}

provider "aws" {
  region = var.region
  default_tags {
    tags = { Project = "cardflow", Environment = "dev", ManagedBy = "opentofu" }
  }
}

locals { name = "cardflow-dev" }

module "network" {
  source = "../../modules/network"
  name   = local.name
}

module "data_lake" {
  source                 = "../../modules/data_lake"
  name                   = local.name
  force_destroy          = true
  snowflake_iam_user_arn = var.snowflake_iam_user_arn
  snowflake_external_id  = var.snowflake_external_id
}

module "mq" {
  source        = "../../modules/mq"
  name          = local.name
  vpc_id        = module.network.vpc_id
  subnet_ids    = module.network.private_subnet_ids
  allowed_cidr  = module.network.vpc_cidr
  cluster       = false
  instance_type = "mq.t3.micro"
}

module "eks" {
  source     = "../../modules/eks"
  name       = local.name
  subnet_ids = module.network.private_subnet_ids
  node_count = 2
}

resource "aws_ecr_repository" "app" {
  for_each             = toset(["backend", "frontend"])
  name                 = "${local.name}/${each.key}"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

output "lake_bucket" { value = module.data_lake.raw_bucket }
output "athena_workgroup" { value = module.data_lake.athena_workgroup }
output "rabbitmq_endpoint" { value = module.mq.amqp_endpoint }
output "eks_cluster" { value = module.eks.cluster_name }
output "snowflake_storage_role_arn" { value = module.data_lake.snowflake_storage_role_arn }
