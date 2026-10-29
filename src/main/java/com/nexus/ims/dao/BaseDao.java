package com.nexus.ims.dao;

import java.util.List;

/**
 * Generic Base Data Access Object (DAO) interface defining core CRUD operations.
 *
 * @param <T> Domain entity type
 */
public interface BaseDAO<T> {

    /**
     * Persists a new entity to the database.
     *
     * @param entity Entity to insert
     * @return true if persistence succeeded, false otherwise
     */
    boolean save(T entity);

    /**
     * Updates an existing entity in the database.
     *
     * @param entity Entity with updated values
     * @return true if updated successfully, false otherwise
     */
    boolean update(T entity);

    /**
     * Deletes an entity record by its unique integer identifier.
     *
     * @param id Primary key ID
     * @return true if deleted successfully, false otherwise
     */
    boolean delete(int id);

    /**
     * Retrieves an entity by its primary key ID.
     *
     * @param id Primary key ID
     * @return the Entity if found, or null if not found
     */
    T findById(int id);

    /**
     * Retrieves all records for the entity type from the database.
     *
     * @return List of all entities
     */
    List<T> findAll();
}
