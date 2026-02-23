package com.union.solutions.saascore.adapters.in.auth;

import java.util.Optional;

/** Interface para parsing de tokens JWT (HS256 ou OIDC). */
public interface TokenParser {

  Optional<TokenClaims> parse(String token);
}
