output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "ecr_repo_url" {
  value = aws_ecr_repository.this.repository_url
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}

output "db_secret_arn" {
  value = aws_secretsmanager_secret.db.arn
}
