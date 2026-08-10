resource "aws_security_group" "msk" {
  name        = "${var.name}-msk-sg"
  description = "Security group for ${var.name} MSK"
  vpc_id      = var.vpc_id

  tags = merge(
    var.tags,
    {
      Name = "${var.name}-msk-sg"
    }
  )
}

resource "aws_vpc_security_group_egress_rule" "msk" {
  security_group_id = aws_security_group.msk.id

  ip_protocol = "-1"
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "msk" {
  security_group_id = aws_security_group.msk.id

  ip_protocol = "tcp"
  from_port   = 9098
  to_port     = 9098

  cidr_ipv4 = var.vpc_cidr
}

resource "aws_msk_serverless_cluster" "this" {
  cluster_name = var.name

  vpc_config {
    subnet_ids = var.private_subnet_ids


    security_group_ids = [
      aws_security_group.msk.id
    ]


  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

  tags = var.tags
}
