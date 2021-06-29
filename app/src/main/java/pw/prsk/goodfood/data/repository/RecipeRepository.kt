package pw.prsk.goodfood.data.repository

import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pw.prsk.goodfood.data.gateway.PhotoGateway
import pw.prsk.goodfood.data.local.db.AppDatabase
import pw.prsk.goodfood.data.local.db.entity.*
import pw.prsk.goodfood.data.local.prefs.RecipePreferences
import java.lang.IllegalStateException
import java.time.LocalDateTime

class RecipeRepository(
    private val dbInstance: AppDatabase,
    private val photoGateway: PhotoGateway,
    private val recipePreferences: RecipePreferences
) {
    suspend fun markAsCooked(id: Int, date: LocalDateTime) = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().markAsCooked(id, date)
    }

    suspend fun changeFavoritesMark(id: Int, state: Boolean) = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().changeFavoriteMark(id, state)
    }

    suspend fun isDatabaseEmpty() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().isDatabaseEmpty().map { it != 0 }
    }

    suspend fun getAllRecipes() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getAllRecipes()
            .map {
                it.map { recipe ->
                    getRecipeWithMeta(recipe)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getFrequentRecipes() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getFrequentRecipes()
            .map {
                it.map { recipe ->
                    getRecipeWithMeta(recipe)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getFavoriteRecipes() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getFavoriteRecipes()
            .map {
                it.map { recipe ->
                    getRecipeWithMeta(recipe)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getAllRecipesPreview() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getAllRecipesPreview()
            .map {
                it.map { recipe ->
                    getRecipeWithMeta(recipe)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getFrequentRecipesPreview() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getFrequentRecipesPreview()
            .map {
                it.map { recipe ->
                    getRecipeWithMeta(recipe)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getFavoriteRecipesPreview() = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getFavoriteRecipesPreview()
            .map {
                it.map { recipe ->
                    getRecipeWithMeta(recipe)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getFlowById(recipeId: Int) = withContext(Dispatchers.IO) {
        dbInstance.recipeDao().getFlowableById(recipeId)
            .map {
                getRecipeWithMeta(it)
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getRecipeById(recipeId: Int) = withContext(Dispatchers.IO) {
        val recipe = dbInstance.recipeDao().getById(recipeId)
        getRecipeWithMeta(recipe)
    }

    suspend fun updateRecipe(recipe: RecipeWithMeta) = withContext(Dispatchers.IO) {
        val recipeId = recipe.id ?: throw IllegalStateException("Trying update recipe without id.")
        val oldRecipe = getRecipeById(recipeId)

        if (oldRecipe.category != recipe.category) {
            removeCategory(oldRecipe.category.id!!)
            processCategory(recipe.category)
        }

        if (oldRecipe.photoFilename != null && oldRecipe.photoFilename != recipe.photoFilename) {
            val uri = photoGateway.getUriForPhoto(oldRecipe.photoFilename!!)
            photoGateway.removePhoto(uri)
        }

        val oldIngredients = processIngredients(oldRecipe.ingredientsList)
        val newIngredients = processIngredients(recipe.ingredientsList)
        val ingredientsDiffResult = DiffUtil.calculateDiff(IngredientsDiffCallback(oldIngredients, newIngredients))
        ingredientsDiffResult.dispatchUpdatesTo()
    }

    private fun getRecipeWithMeta(recipe: Recipe): RecipeWithMeta {
        val ingredientList = getIngredients(recipe.ingredientsList)
        val category = dbInstance.recipeCategoryDao().getById(recipe.categoryId)

        return RecipeWithMeta(
            recipe.id,
            recipe.name,
            recipe.instructions,
            recipe.photoFilename,
            recipe.servingsNum,
            recipe.inFavorites,
            recipe.lastCooked,
            recipe.cookCount,
            ingredientList,
            category
        )
    }

    private fun getIngredients(ingredientList: List<Ingredient>): List<IngredientWithMeta> =
        ingredientList.map {
            val product = dbInstance.productDao().getById(it.productId)
            val amountUnit = dbInstance.productUnitsDao().getById(it.amountUnitId)
            IngredientWithMeta(product, it.amount, amountUnit)
        }

    suspend fun removeRecipe(recipe: Recipe) = withContext(Dispatchers.IO) {
        removeRecipeById(recipe.id!!)
    }

    suspend fun removeRecipeById(id: Int) = withContext(Dispatchers.IO) {
        val recipe = dbInstance.recipeDao().getById(id)
        removeIngredients(recipe.ingredientsList)
        removeCategory(recipe.categoryId)
        if (recipe.photoFilename != null) {
            val uri = photoGateway.getUriForPhoto(recipe.photoFilename!!)
            photoGateway.removePhoto(uri)
        }
        dbInstance.recipeDao().deleteById(recipe.id!!)
    }

    private fun removeIngredients(ingredients: List<Ingredient>) {
        ingredients.forEach {
            // Get product for each ingredient
            val product = dbInstance.productDao().getById(it.productId)
            // If this is the only recipe where this product is used - delete it
            if (product.referenceCount == 1) {
                dbInstance.productDao().delete(product)
            } else {
                dbInstance.productDao().decreaseUsages(it.productId)
            }
        }
    }

    private fun removeCategory(category_id: Int) {
        val category = dbInstance.recipeCategoryDao().getById(category_id)
        // If this is the only recipe where this category is used - delete it
        if (category.referenceCount == 1) {
            if (category_id != recipePreferences.getValue(RecipePreferences.FIELD_NO_CATEGORY,1)) {
                dbInstance.recipeCategoryDao().delete(category)
            } else {
                dbInstance.recipeCategoryDao().decreaseUsages(category_id)
            }
        } else {
            dbInstance.recipeCategoryDao().decreaseUsages(category_id)
        }
    }

    suspend fun addRecipe(recipe: RecipeWithMeta) = withContext(Dispatchers.IO) {
        val convertedIngredients = processIngredients(recipe.ingredientsList)
        processCategory(recipe.category)
        dbInstance.recipeDao().insert(
            Recipe(
                recipe.id,
                recipe.name,
                recipe.description,
                recipe.photoFilename,
                recipe.servingsNum,
                recipe.inFavorites,
                recipe.lastCooked,
                recipe.cookCount,
                convertedIngredients,
                recipe.category.id!!
            )
        )
    }

    private fun processIngredients(ingredients: List<IngredientWithMeta>): List<Ingredient> {
        for (ingredient in ingredients) {
            handleIngredient(ingredient)
        }
        return ingredients.map {
            Ingredient(it.product.id!!, it.amount, it.unit.id!!)
        }
    }

    private fun handleIngredient(ingredient: IngredientWithMeta) {
        // Create product if it is not exists
        if (ingredient.product.id == null) {
            val newId = dbInstance.productDao().insert(ingredient.product)
            ingredient.product.id = newId.toInt()
        }
        // Increase product usage count
        dbInstance.productDao().increaseUsages(ingredient.product.id!!)
    }

    private fun processCategory(category: RecipeCategory?) {
        if (category != null) {
            // Create category if it is not exists
            if (category.id == null) {
                val newId = dbInstance.recipeCategoryDao().insert(category)
                category.id = newId.toInt()
            }
            // Increase category usages
            dbInstance.recipeCategoryDao().increaseUsages(category.id!!)
        }
    }

    private class IngredientsDiffCallback(
        private val oldList: List<Ingredient>,
        private val newList: List<Ingredient>
    ): DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size
    }
}
