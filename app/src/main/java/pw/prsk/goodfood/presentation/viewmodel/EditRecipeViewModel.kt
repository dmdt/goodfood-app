package pw.prsk.goodfood.presentation.viewmodel

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pw.prsk.goodfood.data.gateway.PhotoGateway
import pw.prsk.goodfood.data.local.db.entity.*
import pw.prsk.goodfood.data.local.prefs.RecipePreferences
import pw.prsk.goodfood.data.repository.RecipeCategoryRepository
import pw.prsk.goodfood.data.repository.RecipeRepository
import pw.prsk.goodfood.data.repository.ProductRepository
import pw.prsk.goodfood.data.repository.ProductUnitsRepository
import pw.prsk.goodfood.presentation.ui.EditRecipeFlowType
import pw.prsk.goodfood.utils.SingleLiveEvent
import java.lang.IllegalStateException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class EditRecipeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val productUnitsRepository: ProductUnitsRepository,
    private val recipeRepository: RecipeRepository,
    private val recipeCategoryRepository: RecipeCategoryRepository,
    private val photoGateway: PhotoGateway,
    private val recipePreferences: RecipePreferences
) : ViewModel() {
    private val ingredientsList: MutableList<IngredientWithMeta> = mutableListOf()
    private val _ingredients = MutableLiveData<List<IngredientWithMeta>>(ingredientsList)
    val ingredients: LiveData<List<IngredientWithMeta>>
        get() = _ingredients

    private val _productsList = MutableLiveData<List<Product>>()
    val productsList: LiveData<List<Product>>
        get() = _productsList

    private val _unitsList = MutableLiveData<List<ProductUnit>>()
    val unitsList: LiveData<List<ProductUnit>>
        get() = _unitsList

    private val _recipeCategories = MutableLiveData<List<RecipeCategory>>()
    val recipeCategories: LiveData<List<RecipeCategory>>
        get() = _recipeCategories

    private val _editRecipeData = MutableLiveData<RecipeWithMeta>()
    val editRecipeData: LiveData<RecipeWithMeta>
        get() = _editRecipeData

    private var photoUri: Uri? = null
    private var photoFilename: String? = null
    private var photoFromCamera: Boolean = false
    private val photoDrawable = MutableLiveData<Drawable?>()
    val photo: LiveData<Drawable?>
        get() = photoDrawable
    val photoStatus: Boolean
        get() = photoDrawable.value != null

    val saveStatus: SingleLiveEvent<Boolean> = SingleLiveEvent()

    private lateinit var flowType: EditRecipeFlowType

    init {
        viewModelScope.launch {
            _productsList.value = productRepository.getProducts()
            _unitsList.value = productUnitsRepository.getUnits()
            _recipeCategories.value = recipeCategoryRepository.getCategories()
        }
    }

    fun setPhotoUri(photo: Uri) {
        photoUri = photo
        loadPhoto()
    }

    fun removePhoto() {
        if (photoFromCamera) {
            photoGateway.removePhoto(photoUri!!)
            photoFromCamera = false
        }
        photoDrawable.value = null
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            photoDrawable.value = photoGateway.loadPhoto(photoUri!!)
        }
    }

    fun getPhotoUri(): Uri {
        photoFromCamera = true
        photoFilename = photoGateway.createNewPhotoFile()
        photoUri = photoGateway.getUriForPhoto(photoFilename!!)
        return photoUri!!
    }

    fun addIngredient(item: IngredientWithMeta) {
        ingredientsList.add(item)
        _ingredients.value = ingredientsList
    }

    fun saveRecipe(name: String, description: String?, servingsCount: Int, selectedCategory: RecipeCategory?) {
        viewModelScope.launch {
            when (flowType) {
                EditRecipeFlowType.FLOW_ADD_RECIPE -> addRecipe(name, description, servingsCount, selectedCategory)
                EditRecipeFlowType.FLOW_EDIT_RECIPE -> updateRecipe(name, description, servingsCount, selectedCategory)
            }
        }
    }

    private suspend fun addRecipe(name: String, description: String?, servingsCount: Int, selectedCategory: RecipeCategory?) {
        // Copy photo to app folder if it was picked
        if (!photoFromCamera && photoStatus) {
            photoFilename = photoGateway.createNewPhotoFile()
            val internalUri = photoGateway.getUriForPhoto(photoFilename!!)
            val newPhoto = photoGateway.copyPhoto(photoUri!!, internalUri)
            Log.d(TAG, "Filename where chosen photo will be copied: '${newPhoto.toString()}'.")
        }

        val category = selectedCategory
            ?: RecipeCategory(recipePreferences.getValue(RecipePreferences.FIELD_NO_CATEGORY, 0), "")

        val recipe = RecipeWithMeta(
            null,
            name,
            description,
            photoFilename,
            servingsCount,
            false,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault()),
            0,
            ingredientsList,
            category
        )
        recipeRepository.addRecipe(recipe)

        photoFromCamera = false // Prevent photo deletion on exit
        saveStatus.value = true
    }

    private suspend fun updateRecipe(name: String, description: String?, servingsCount: Int, selectedCategory: RecipeCategory?) {
        val recipe = editRecipeData.value ?: throw IllegalStateException("Opened recipe not found.")
        // Copy photo to app folder if it was picked
        if (!photoFromCamera && photoStatus) {
            photoFilename = photoGateway.createNewPhotoFile()
            val internalUri = photoGateway.getUriForPhoto(photoFilename!!)
            val newPhoto = photoGateway.copyPhoto(photoUri!!, internalUri)
            Log.d(TAG, "Filename where chosen photo will be copied: '${newPhoto.toString()}'.")
        }

        val category = selectedCategory
            ?: RecipeCategory(recipePreferences.getValue(RecipePreferences.FIELD_NO_CATEGORY, 0), "")

        val updatedRecipe = recipe.copy(
            name = name,
            description = description,
            photoFilename = photoFilename,
            servingsNum = servingsCount,
            ingredientsList = ingredientsList,
            category = category
        )
        recipeRepository.updateRecipe(updatedRecipe)
    }

    override fun onCleared() {
        super.onCleared()
        removePhoto()
    }

    fun setFlowType(fragmentFlowType: EditRecipeFlowType, editRecipeId: Int) {
        flowType = fragmentFlowType
        if (flowType == EditRecipeFlowType.FLOW_EDIT_RECIPE) {
            viewModelScope.launch {
                // Get recipe to edit
                val recipe = recipeRepository.getRecipeById(editRecipeId)
                // Set ingredients list
                ingredientsList.clear()
                ingredientsList.addAll(recipe.ingredientsList)
                _ingredients.value = ingredientsList
                // Set recipe data
                _editRecipeData.value = recipe
                // Set recipe photo
                recipe.photoFilename?.let {
                    val photoUri = photoGateway.getUriForPhoto(it)
                    photoDrawable.value = photoGateway.loadPhoto(photoUri)
                }
            }
        }
    }

    companion object {
        private const val TAG = "EditMealViewModel"
    }
}
