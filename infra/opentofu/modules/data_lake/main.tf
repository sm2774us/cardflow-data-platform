variable "name" { type = string }
variable "snowflake_iam_user_arn" {
  type        = string
  default     = ""
  description = "STORAGE_AWS_IAM_USER_ARN from `DESC INTEGRATION CARDFLOW_S3`; empty disables the Snowflake role."
}
variable "snowflake_external_id" {
  type    = string
  default = ""
}
variable "force_destroy" {
  type    = bool
  default = false
}

resource "aws_kms_key" "lake" {
  description             = "${var.name} data lake"
  enable_key_rotation     = true
  deletion_window_in_days = 14
}

resource "aws_s3_bucket" "raw" {
  bucket        = "${var.name}-lake-raw"
  force_destroy = var.force_destroy
}

resource "aws_s3_bucket" "athena_results" {
  bucket        = "${var.name}-athena-results"
  force_destroy = var.force_destroy
}

resource "aws_s3_bucket_versioning" "raw" {
  bucket = aws_s3_bucket.raw.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "raw" {
  bucket = aws_s3_bucket.raw.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.lake.arn
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "raw" {
  bucket                  = aws_s3_bucket.raw.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "athena" {
  bucket                  = aws_s3_bucket.athena_results.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "raw" {
  bucket = aws_s3_bucket.raw.id
  rule {
    id     = "tiering"
    status = "Enabled"
    filter { prefix = "transactions/" }
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
    noncurrent_version_expiration { noncurrent_days = 90 }
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}

resource "aws_glue_catalog_database" "lake" {
  name = replace("${var.name}_lake", "-", "_")
}

# Partition projection: Athena derives dt/hour partitions from the key layout - no crawlers, no MSCK REPAIR.
resource "aws_glue_catalog_table" "transactions" {
  name          = "transactions"
  database_name = aws_glue_catalog_database.lake.name
  table_type    = "EXTERNAL_TABLE"

  parameters = {
    "projection.enabled"        = "true"
    "projection.dt.type"        = "date"
    "projection.dt.range"       = "2026-01-01,NOW"
    "projection.dt.format"      = "yyyy-MM-dd"
    "projection.hour.type"      = "integer"
    "projection.hour.range"     = "0,23"
    "projection.hour.digits"    = "2"
    "storage.location.template" = "s3://${aws_s3_bucket.raw.bucket}/transactions/dt=$${dt}/hour=$${hour}/"
    "classification"            = "json"
    "compressionType"           = "gzip"
  }

  partition_keys {
    name = "dt"
    type = "string"
  }
  partition_keys {
    name = "hour"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${aws_s3_bucket.raw.bucket}/transactions/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    ser_de_info {
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
      parameters            = { "ignore.malformed.json" = "true" }
    }
    dynamic "columns" {
      for_each = {
        event_id       = "string"
        event_type     = "string"
        card_token     = "string"
        merchant_name  = "string"
        mcc            = "string"
        amount         = "decimal(18,2)"
        currency       = "string"
        country        = "string"
        occurred_at    = "string"
        decision       = "string"
        decline_reason = "string"
        risk_score     = "decimal(5,4)"
        created_at     = "string"
      }
      content {
        name = columns.key
        type = columns.value
      }
    }
  }
}

resource "aws_athena_workgroup" "lake" {
  name          = "${var.name}-lake"
  force_destroy = var.force_destroy
  configuration {
    enforce_workgroup_configuration    = true
    publish_cloudwatch_metrics_enabled = true
    bytes_scanned_cutoff_per_query     = 10737418240
    result_configuration {
      output_location = "s3://${aws_s3_bucket.athena_results.bucket}/results/"
      encryption_configuration { encryption_option = "SSE_S3" }
    }
  }
}

# Role assumed by Snowflake's storage integration (read-only on transactions/).
data "aws_iam_policy_document" "snowflake_trust" {
  count = var.snowflake_iam_user_arn == "" ? 0 : 1
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "AWS"
      identifiers = [var.snowflake_iam_user_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "sts:ExternalId"
      values   = [var.snowflake_external_id]
    }
  }
}

resource "aws_iam_role" "snowflake" {
  count              = var.snowflake_iam_user_arn == "" ? 0 : 1
  name               = "${var.name}-snowflake-storage"
  assume_role_policy = data.aws_iam_policy_document.snowflake_trust[0].json
}

data "aws_iam_policy_document" "snowflake_read" {
  statement {
    actions   = ["s3:GetObject", "s3:GetObjectVersion"]
    resources = ["${aws_s3_bucket.raw.arn}/transactions/*"]
  }
  statement {
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [aws_s3_bucket.raw.arn]
    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["transactions/*"]
    }
  }
  statement {
    actions   = ["kms:Decrypt"]
    resources = [aws_kms_key.lake.arn]
  }
}

resource "aws_iam_role_policy" "snowflake" {
  count  = var.snowflake_iam_user_arn == "" ? 0 : 1
  name   = "read-lake"
  role   = aws_iam_role.snowflake[0].id
  policy = data.aws_iam_policy_document.snowflake_read.json
}

output "raw_bucket" { value = aws_s3_bucket.raw.bucket }
output "raw_bucket_arn" { value = aws_s3_bucket.raw.arn }
output "kms_key_arn" { value = aws_kms_key.lake.arn }
output "glue_database" { value = aws_glue_catalog_database.lake.name }
output "athena_workgroup" { value = aws_athena_workgroup.lake.name }
output "snowflake_storage_role_arn" { value = try(aws_iam_role.snowflake[0].arn, null) }
