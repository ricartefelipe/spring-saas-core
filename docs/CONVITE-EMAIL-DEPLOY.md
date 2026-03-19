# E-mail de convite — Fluxe B2B Suite e troca obrigatória de senha

## Resend: domínio e tier gratuito

- **Tier gratuito Resend:** só permite enviar para o teu próprio e-mail verificado. Para enviar a qualquer destinatário, verifica um domínio em [resend.com/domains](https://resend.com/domains) e usa `EMAIL_FROM` com esse domínio (ex.: `noreply@seudominio.com.br`).
- **Staging sem domínio:** com perfil `staging`, `app.email.fail-on-delivery-error=false` por defeito — o convite cria o utilizador mesmo quando o Resend falha (403 domain not verified). Usa "Reenviar convite" depois de verificar o domínio, ou convida apenas para o teu e-mail em testes.

## Comportamento atual (a partir de `develop` após PR #42)

- **Nome no e-mail:** Para o tenant de plataforma (UUID `00000000-0000-0000-0000-000000000001`), o convite mostra sempre **"Fluxe B2B Suite"**, nunca "System".
- **Troca de senha:** O texto do e-mail deixa claro que é **obrigatório** alterar a senha no primeiro acesso; o login retorna `must_change_password` e o JWT inclui o claim `mcp` para o front bloquear o uso da app até a troca.

## Se ainda recebes "System" ou "Recomendamos alterar a senha"

Isso indica que o **servidor que envia os e-mails está a correr uma versão antiga** do spring-saas-core.

### O que fazer

1. **Staging (branch `develop`)**  
   - No Railway, o deploy do spring-saas-core deve ser feito a partir da branch **`develop`**.  
   - Confirma em **Settings → Source** que a **Production Branch** do serviço spring-saas-core é `develop`.  
   - Se já estiver correto, dispara um **redeploy** (ex.: "Redeploy" no dashboard ou novo push em `develop`).  
   - Garante que o **Admin Console** (e qualquer front que use convites) está a usar a **URL do Core** desse ambiente (staging), não de outro.

2. **Produção (branch `master`)**  
   - Faz **merge de `develop` em `master`** no repositório spring-saas-core.  
   - Depois do deploy em produção, o Core de produção passará a enviar o e-mail novo.

3. **Local**  
   - Corre o JAR ou `mvn spring-boot:run` a partir do código atual em `develop` (ou `master` após merge).

Após o deploy da versão correta, **novos convites** passam a sair com "Fluxe B2B Suite" e com o texto de obrigatoriedade de troca de senha. Utilizadores já convidados antes podem usar **"Reenviar convite"** no Admin para receber o e-mail actualizado e o fluxo de troca obrigatória.

## Troca obrigatória de senha no primeiro uso

**O que fazemos hoje**

1. No convite, o utilizador é criado com `must_change_password = true`.
2. No login, a API devolve `must_change_password` e o JWT inclui o claim `mcp`.
3. O front (ops-portal / admin-console) redireciona para `/change-password` e o **guard** impede navegar no resto da app até trocar a senha.
4. Após trocar, o Core devolve um **novo token** sem `mcp`.

**Sugestões extra (opcional, futuro)**

- Expirar senhas temporárias após N dias (obrigar reenvio de convite).
- Notificar por e-mail se alguém tentar entrar várias vezes sem completar a troca.

Se alguém convidado **antes** deste fluxo ainda entra sem ser forçado a trocar: usar **Reenviar convite** ou corrigir o flag na base de dados.

## Testes

- **`EmailTemplatesTest`** — Garante que o corpo do e-mail de convite mostra "Fluxe B2B Suite" quando o nome do tenant é "System", "Sistema" ou vazio; e mostra o nome real (ex.: "Acme Distribuidora") nos outros casos. Inclui cenário com senha temporária (bloco "Obrigatório").
- **`UserManagementUseCaseTest`** — Garante que `invite()` envia e-mail com assunto e corpo contendo "Fluxe B2B Suite" quando o tenant é o de plataforma ou o nome na BD é "System"/"Sistema"; e com nome do tenant nos outros casos. Inclui testes de utilizador já existente e de `mustChangePassword = true` no convite.
