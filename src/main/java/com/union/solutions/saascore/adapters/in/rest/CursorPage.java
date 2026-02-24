package com.union.solutions.saascore.adapters.in.rest;

import java.util.List;

public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {}
