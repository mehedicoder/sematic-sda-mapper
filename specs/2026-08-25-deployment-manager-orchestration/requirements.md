# Requirements — Phase 17: Deployment Manager — actual orchestration

## Scope
Paper §4.3 / Figure 6: Deployment Manager "orchestrates the selected services as Kubernetes
workloads according to infrastructure-level deployment constraints."

Currently only Docker Compose / Kubernetes **manifest generation** exists
(`DockerComposeManifestService`, `KubernetesManifestService`). Nothing applies them to a cluster.

## BLOCKED — needs approval
- `specs/mission.md` lists "Full Kubernetes deployment automation to a real cluster" as
  **Out of Scope**. Implementing live apply requires infrastructure (kind / minikube / real
  cluster) and violates the AGENTS rule "no new infrastructure without explicit sign-off".
- **Status: [BLOCKED]** — do not implement until the human architect approves the target env.

## Decisions (when unblocked)
- Choose orchestration target (kind / minikube / real cluster).
- Apply generated manifest via Kubernetes client; wire `R-Deploy` `boundContext` → nodeSelector.
- Dry-run + rollback, health-gated rollout; tests with mock API server.

## Artifact placement
`specs/2026-08-25-deployment-manager-orchestration/`.
