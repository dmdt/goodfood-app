package pw.prsk.goodfood.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_categories")
data class RecipeCategory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Int? = null,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "reference_count") var referenceCount: Int = 0
)
