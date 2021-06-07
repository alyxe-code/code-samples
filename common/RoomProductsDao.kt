/*
 * Copyright (c) 2021.
 */

package com.unicorns.core.database.room.products

import androidx.room.*
import com.kmgi.unicorns.core.models.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomProductsDao {

    @Query("SELECT * FROM products")
    fun allAsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun all(): List<Product>

    @Query("SELECT * FROM products LIMIT :count OFFSET :offset")
    fun select(offset: Int, count: Int): List<Product>

    @Query("SELECT * FROM products ORDER BY updatedAt LIMIT :count OFFSET :offset")
    fun selectNewest(offset: Int, count: Int): List<Product>

    @Query("SELECT * FROM products ORDER BY updatedAt DESC LIMIT :count OFFSET :offset")
    fun selectOldest(offset: Int, count: Int): List<Product>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products WHERE id=:id")
    suspend fun one(id: Int): Product?

    @Query("SELECT * FROM products WHERE id=:id")
    fun oneAsFlow(id: Int): Flow<Product?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(products: List<Product>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(products: List<Product>)

    @Query("DELETE FROM products WHERE id=:id")
    suspend fun delete(id: String)

    @Query("DELETE FROM products")
    suspend fun clear()
}