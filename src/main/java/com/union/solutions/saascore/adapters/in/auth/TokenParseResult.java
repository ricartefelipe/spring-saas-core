package com.union.solutions.saascore.adapters.in.auth;

/** Resultado do parsing de JWT com indicação se foi verificado com chave em rotação. */
public record TokenParseResult(TokenClaims claims, boolean verifiedWithPreviousKey) {

  public static TokenParseResult current(TokenClaims claims) {
    return new TokenParseResult(claims, false);
  }

  public static TokenParseResult previous(TokenClaims claims) {
    return new TokenParseResult(claims, true);
  }
}
