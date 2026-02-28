package com.univerliga.gateway.model;

import java.util.List;

public record CategoryRecord(String id, String name, List<SubcategoryRecord> subcategories) {
    public record SubcategoryRecord(String id, String name) {
    }
}
