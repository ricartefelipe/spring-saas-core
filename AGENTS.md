# AGENTS.md — Diretrizes para assistentes no spring-saas-core

Regras para quem altera este repositório (humanos ou assistentes automatizados).

---

## Git Flow

- Branches: `master` (produção), `develop` (staging), `feature/*`, `fix/*`, `docs/*`.
- Trabalho novo: criar `feature/...` ou `fix/...` a partir de `develop` atualizada; **nunca** commit direto em `develop` ou `master`.
- Integração em `develop`: via PR (preferencial) ou merge local equivalente; **CI verde** antes de mergear.
- **Release** `develop` → `master`: só quando o responsável pedir; não abrir PR de release por iniciativa própria.

---

## Qualidade e verificação

- Java 21, Spring Boot; formatação **Spotless** (`./mvnw spotless:check`, `spotless:apply`).
- Antes de concluir: `./mvnw test` (e `spotless:check` no mínimo).
- Contratos e integrações: ver `docs/PROMPT-EVOLUCAO.md`, `docs/contracts/`, alinhamento com node-b2b-orders e py-payments-ledger.

---

## Commits e documentação

- Mensagens de commit **claras**, em português ou inglês consistente com o histórico.
- **Não** incluir marcas comerciais de IDEs ou assistentes em commits, PRs ou documentação (nem rodapés automáticos de ferramentas).

---

## Papel do agente (delegação)

- Pode executar no repo: branches, implementação, testes, formatação, commit, push, PR e merge em `develop` após CI verde.
- **Limites:** sem acesso a painéis cloud (Railway, etc.) nem credenciais; sem `sudo` na máquina do utilizador — apenas preparar variáveis e checklists.

---

## Referências

- Ambientes: `docs/CONFIG-AMBIENTES.md`, `docs/CONVITE-EMAIL-DEPLOY.md`
- Git Flow / pipeline (canónico, multi-repo): [PIPELINE-ESTEIRAS.md](https://github.com/ricartefelipe/fluxe-b2b-suite/blob/develop/docs/PIPELINE-ESTEIRAS.md) no **fluxe-b2b-suite**
