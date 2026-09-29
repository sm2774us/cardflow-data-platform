# ADR-0005: OpenTofu (not source-available Terraform) for infrastructure as code
**Status:** Accepted

## Context
Terraform has been source-available under the Business Source License since 2023. For a personal, non-competing project the BSL would legally allow its use, so this is a values and long-term-risk decision, not a legal necessity. The project's stated goal is to be open source in spirit.

## Decision
Use **OpenTofu** (MPL-2.0, Linux Foundation) for everything under `infra/opentofu/`.
* The HCL stays Terraform-compatible for the features used here: files are still `*.tf`, and the `terraform {}` settings block and `hashicorp/aws` provider address are unchanged, so the same code should also run under Terraform. Re-verify this on major upgrades of either tool.
* CI uses `opentofu/setup-opentofu` pinned to a specific version; local commands are `make tofu-fmt` and `make tofu-validate`.

## Consequences
+ Community-governed licence with no dependence on one vendor's future licensing choices.
+ Access to OpenTofu-only features when useful (e.g. native state encryption, early variable evaluation).
+ One CLI (`tofu`) across work and personal repositories.
− Some Terraform-only features and third-party tooling assume the `terraform` binary; compatibility should be re-checked on major upgrades.
− Provider downloads come from the OpenTofu registry, which mirrors the same providers.
