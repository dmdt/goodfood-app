package pw.prsk.goodfood.presentation.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pw.prsk.goodfood.data.local.db.entity.RecipeWithMeta
import pw.prsk.goodfood.data.repository.RecipeRepository
import javax.inject.Inject


class RecipesOverviewViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    val allRecipes by lazy {
        MutableLiveData<List<RecipeWithMeta>>()
    }
    val favoriteRecipes by lazy {
        MutableLiveData<List<RecipeWithMeta>>()
    }
    val frequentRecipes by lazy {
        MutableLiveData<List<RecipeWithMeta>>()
    }

    val isDataPresence by lazy {
        MutableLiveData(true)
    }

    init {
        viewModelScope.launch {
            recipeRepository.getAllRecipesPreview()
                .onEach { allRecipes.postValue(it) }
                .launchIn(this)

            recipeRepository.getFavoriteRecipesPreview()
                .onEach { favoriteRecipes.postValue(it) }
                .launchIn(this)

            recipeRepository.getFrequentRecipesPreview()
                .onEach { frequentRecipes.postValue(it) }
                .launchIn(this)

            recipeRepository.isDatabaseEmpty()
                .onEach { isDataPresence.value = it }
                .launchIn(this)
        }
    }

    fun changeFavoriteState(recipeId: Int, state: Boolean) {
        viewModelScope.launch {
            recipeRepository.changeFavoritesMark(recipeId, state)
        }
    }
}