variable "name" { type = string }
variable "vpc_id" { type = string }
variable "subnet_ids" { type = list(string) }
variable "allowed_cidr" { type = string }
variable "cluster" {
  type    = bool
  default = false
}
variable "instance_type" {
  type    = string
  default = "mq.t3.micro"
}

resource "random_password" "admin" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "mq" { name = "${var.name}/rabbitmq-admin" }

resource "aws_secretsmanager_secret_version" "mq" {
  secret_id     = aws_secretsmanager_secret.mq.id
  secret_string = jsonencode({ username = "cardflow", password = random_password.admin.result })
}

resource "aws_security_group" "mq" {
  name_prefix = "${var.name}-mq-"
  vpc_id      = var.vpc_id
  ingress {
    description = "AMQPS from VPC"
    from_port   = 5671
    to_port     = 5671
    protocol    = "tcp"
    cidr_blocks = [var.allowed_cidr]
  }
  lifecycle { create_before_destroy = true }
}

# Amazon MQ for RabbitMQ: managed broker; CLUSTER_MULTI_AZ in prod, single instance in dev.
resource "aws_mq_broker" "this" {
  broker_name                = "${var.name}-rabbitmq"
  engine_type                = "RabbitMQ"
  engine_version             = "3.13"
  host_instance_type         = var.instance_type
  deployment_mode            = var.cluster ? "CLUSTER_MULTI_AZ" : "SINGLE_INSTANCE"
  subnet_ids                 = var.cluster ? var.subnet_ids : [var.subnet_ids[0]]
  security_groups            = [aws_security_group.mq.id]
  publicly_accessible        = false
  auto_minor_version_upgrade = true

  user {
    username = "cardflow"
    password = random_password.admin.result
  }
  logs { general = true }
}

output "amqp_endpoint" { value = aws_mq_broker.this.instances[0].endpoints[0] }
output "credentials_secret_arn" { value = aws_secretsmanager_secret.mq.arn }
