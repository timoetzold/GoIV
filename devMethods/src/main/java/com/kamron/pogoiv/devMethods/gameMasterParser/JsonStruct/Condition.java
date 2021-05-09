package com.kamron.pogoiv.devMethods.gameMasterParser.JsonStruct;

import com.google.gson.annotations.Expose;

public class Condition {
    @Expose
    private String type;
    @Expose
    private WithThrowType withThrowType;
    @Expose
    private WithPokemonType withPokemonType;

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public WithThrowType getWithThrowType() { return withThrowType; }

    public void setWithThrowType(WithThrowType withThrowType) { this.withThrowType = withThrowType; }

    public WithPokemonType getWithPokemonType() {
        return withPokemonType;
    }

    public void setWithPokemonType(WithPokemonType withPokemonType) {
        this.withPokemonType = withPokemonType;
    }
}
