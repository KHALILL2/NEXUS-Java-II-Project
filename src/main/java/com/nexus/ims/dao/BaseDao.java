package com.nexus.ims.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object contract for CRUD persistence operations.
 *
 * @param <T> Entity type
 * @param <ID> Primary key identifier type
 */
public interface BaseDao<T, ID> {
    Optional<T> findById(ID id) throws SQLException;
    List<T> findAll() throws SQLException;
    boolean save(T entity) throws SQLException;
    boolean update(T entity) throws SQLException;
    boolean deleteById(ID id) throws SQLException;
}

