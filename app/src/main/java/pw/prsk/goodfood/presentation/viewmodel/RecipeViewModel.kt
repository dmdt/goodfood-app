package pw.prsk.goodfood.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pw.prsk.goodfood.data.local.db.entity.IngredientWithMeta
import pw.prsk.goodfood.data.gateway.PhotoGateway
import pw.prsk.goodfood.data.local.db.entity.RecipeWithMeta
import pw.prsk.goodfood.data.repository.CartRepository
import pw.prsk.goodfood.data.repository.RecipeRepository
import pw.prsk.goodfood.utils.SingleLiveEvent
import java.time.LocalDateTime
import javax.inject.Inject

class RecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val cartRepository: CartRepository,
    private val photoGateway: PhotoGateway
) : ViewModel() {
    private val _recipe by lazy {
        MutableLiveData<RecipeWithMeta>()
    }
    val recipe: LiveData<RecipeWithMeta>
        get() = _recipe

    private val _recipePhoto by lazy {
        MutableLiveData<Bitmap?>()
    }
    val recipePhoto: LiveData<Bitmap?>
        get() = _recipePhoto

    private val _recipeIngredients by lazy {
        MutableLiveData<List<IngredientWithMeta>>()
    }
    val ingredientsList: LiveData<List<IngredientWithMeta>>
        get() = _recipeIngredients

    private val _servings by lazy {
        MutableLiveData(1)
    }
    val servingsNum: LiveData<Int>
        get() = _servings

    val recipeDeleteAction by lazy {
        SingleLiveEvent<Boolean>()
    }

    val cartEvent by lazy {
        SingleLiveEvent<Boolean>()
    }

    private var recipeFlowJob: Job? = null

    fun loadRecipe(recipeId: Int) {
        recipeFlowJob = viewModelScope.launch {
            recipeRepository.getFlowById(recipeId)
                .onEach { recipe ->
                    _recipe.postValue(recipe)

                    if (recipe.photoFilename != null) {
                        _recipePhoto.postValue(
                            photoGateway.loadScaledPhoto(
                                photoGateway.getUriForPhoto(recipe.photoFilename!!),
                                200,
                                200
                            )
                        )
                    }
                    _recipeIngredients.postValue(recipe.ingredientsList)
                    _servings.postValue(recipe.servingsNum)
                }
                .flowOn(Dispatchers.Default)
                .launchIn(this)
        }
    }

    fun markAsCooked() {
        viewModelScope.launch {
            val currentTime = LocalDateTime.now()
            recipeRepository.markAsCooked(recipe.value?.id!!, currentTime)
        }
    }

    fun onDecreaseClicked() {
        if (_servings.value == 1) return
        _servings.value = _servings.value?.minus(1)
    }

    fun onIncreaseClicked() {
        _servings.value = _servings.value?.plus(1)
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            recipeFlowJob?.cancel()
            recipeRepository.removeRecipeById(recipe.value?.id!!)
            recipeDeleteAction.value = true
        }
    }

    fun changeFavoriteState(state: Boolean) {
        viewModelScope.launch {
            if (recipe.value?.inFavorites != state) {
                recipeRepository.changeFavoritesMark(recipe.value?.id!!, state)
            }
        }
    }

    fun addIngredientsToCart() {
        viewModelScope.launch {
            cartRepository.addIngredientsToCart(
                recipe.value?.id!!,
                servingsNum.value!!.toFloat() / recipe.value?.servingsNum!!
            )
            cartEvent.value = true
        }
    }
}
