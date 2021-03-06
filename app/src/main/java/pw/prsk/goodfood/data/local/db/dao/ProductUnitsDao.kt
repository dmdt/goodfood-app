package pw.prsk.goodfood.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import pw.prsk.goodfood.data.local.db.entity.ProductUnit

@Dao
interface ProductUnitsDao: BaseDao<ProductUnit> {
    @Query("SELECT * FROM product_units")
    fun getAll(): List<ProductUnit>

    @Query("SELECT * FROM product_units WHERE id = :id")
    fun getById(id: Int): ProductUnit
}