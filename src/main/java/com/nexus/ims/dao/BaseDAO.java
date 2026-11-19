package com.nexus.ims.dao;

import java.util.List;

/**
 * Common CRUD contract for JDBC-backed Data Access Objects.
 *
 * @param <T> domain model type
 */
public interface BaseDAO<T> {
    boolean save(T entity);

    boolean update(T entity);

    boolean delete(int id);

    T findById(int id);

    List<T> findAll();
}
