variable "project" {
  type    = string
  default = "portfolio"
}

variable "service_name" {
  type    = string
  default = "order-service"
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "db_name" {
  type    = string
  default = "orderdb"
}

variable "db_username" {
  type    = string
  default = "orderuser"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro" # 프리티어/저비용 위주(계정/기간에 따라 프리티어 여부 다를 수 있음)
}

variable "desired_count" {
  type    = number
  default = 1
}

# ECR에 push한 이미지 태그(예: latest, v1 등)
variable "image_tag" {
  type    = string
  default = "latest"
}
