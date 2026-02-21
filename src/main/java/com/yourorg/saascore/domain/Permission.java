package com.yourorg.saascore.domain;

public class Permission {

    private final String code;
    private final String description;

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return java.util.Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code);
    }
}
