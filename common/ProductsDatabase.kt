/*
 * Copyright (c) 2021.
 */

package com.unicorns.core.database.room.products

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kmgi.unicorns.core.models.Product
import com.unicorns.core.database.room.FileInfoSerializationUtility
import com.unicorns.core.database.room.IndustrySerializationUtility
import com.unicorns.core.database.room.ProductsSerializationUtility
import com.unicorns.core.database.room.UserSerializationUtility

@Database(
    entities = [
        Product::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(
    ProductsSerializationUtility::class,
    FileInfoSerializationUtility::class,
    UserSerializationUtility::class,
    IndustrySerializationUtility::class,
)
abstract class ProductsDatabase : RoomDatabase() {

    abstract fun products(): RoomProductsDao

    companion object {
        fun newInstance(context: Context) = Room
            .databaseBuilder(
                context,
                ProductsDatabase::class.java,
                "products_db"
            )
            .build()
    }
}

