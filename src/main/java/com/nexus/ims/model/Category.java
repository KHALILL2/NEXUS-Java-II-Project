package com.nexus.ims.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an inventory classification category.
 */
public class Category {
    private int id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public Category() {}

    public Category(String name, String description) {
        setName(name);
        this.description = description;
    }

    public Category(int id, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        setName(name);
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;
        return id == category.id && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return String.format("Category [id=%d, name='%s']", id, name);
    }
}
