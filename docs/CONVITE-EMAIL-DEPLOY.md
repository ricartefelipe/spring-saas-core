# E-mail de convite — Fluxe B2B Suite e troca obrigatória de senha

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
