.PHONY: help up down backend-test backend-verify frontend-install frontend-all api-tests tofu-fmt tofu-validate k8s-render clean

help:
	@echo "make up              # full stack via docker compose (UI :8081, API :8080, RabbitMQ UI :15672)"
	@echo "make backend-test    # unit tests (no Docker needed)"
	@echo "make backend-verify  # unit + Testcontainers integration tests (Docker required)"
	@echo "make frontend-all    # install, lint, typecheck, test, build"
	@echo "make api-tests       # Karate + Newman against a running stack"
	@echo "make tofu-validate   # OpenTofu fmt-check + validate for all envs"
	@echo "make k8s-render      # kustomize build for dev/prod/openshift"

up:            ; docker compose up --build
down:          ; docker compose down -v
backend-test:  ; cd backend && mvn -B test
backend-verify:; cd backend && mvn -B verify
frontend-install: ; cd frontend && npm ci --no-audit --no-fund
frontend-all: frontend-install
	cd frontend && npm run lint && npm run typecheck && npm test && npm run build
api-tests:
	cd api-tests/karate && mvn -B test -Dkarate.baseUrl=$${API:-http://localhost:8080}
	npx --yes newman run api-tests/postman/cardflow.postman_collection.json --env-var baseUrl=$${API:-http://localhost:8080}
tofu-fmt:      ; tofu -chdir=infra/opentofu fmt -recursive
tofu-validate:
	tofu -chdir=infra/opentofu fmt -check -recursive
	for e in dev prod; do tofu -chdir=infra/opentofu/envs/$$e init -backend=false -input=false && tofu -chdir=infra/opentofu/envs/$$e validate; done
k8s-render:
	for o in dev prod openshift; do kubectl kustomize k8s/overlays/$$o > /dev/null && echo "$$o ok"; done
clean:         ; rm -rf backend/target frontend/dist frontend/node_modules
