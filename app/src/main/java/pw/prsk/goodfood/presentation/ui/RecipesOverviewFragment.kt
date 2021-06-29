package pw.prsk.goodfood.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import pw.prsk.goodfood.R
import pw.prsk.goodfood.presentation.adapter.RecipeCardAdapter
import pw.prsk.goodfood.databinding.FragmentRecipesOverviewBinding
import pw.prsk.goodfood.presentation.viewmodel.RecipesOverviewViewModel
import java.lang.IllegalStateException
import javax.inject.Inject

class RecipesOverviewFragment : Fragment() {
    private var _binding: FragmentRecipesOverviewBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var vmFactory: ViewModelProvider.Factory
    private lateinit var viewModel: RecipesOverviewViewModel

    private val recipeCallback = object : RecipeClickCallback {
        override fun onFavoriteToggle(recipeId: Int, state: Boolean) {
            viewModel.changeFavoriteState(recipeId, state)
        }

        override fun onRecipeClicked(recipeId: Int) {
            val args = Bundle().apply {
                putInt(RecipeFragment.RECIPE_ID_KEY, recipeId)
            }
            Navigation.findNavController(requireActivity(), R.id.fcvContainer).navigate(R.id.actionNavigateToRecipe, args)
        }

        override fun onMoreButtonClick(listType: Int) {
            val listTypeKey = when(listType) {
                LIST_ALL_RECIPES -> RecipeListFragment.LIST_TYPE_ALL
                LIST_FAVORITE_RECIPES -> RecipeListFragment.LIST_TYPE_FAVORITES
                LIST_FREQUENT_RECIPES -> RecipeListFragment.LIST_TYPE_FREQUENT
                else -> {
                    throw IllegalStateException("Unknown list type $listType")
                }
            }
            val args = Bundle().apply {
                putInt(RecipeListFragment.LIST_TYPE_KEY, listTypeKey)
            }
            Navigation.findNavController(requireActivity(), R.id.fcvContainer).navigate(R.id.actionNavigateToRecipeList, args)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
        viewModel = ViewModelProvider(this, vmFactory).get(RecipesOverviewViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclers()

        viewModel.isDataPresence.observe(viewLifecycleOwner) {
            if (it) {
                binding.nsvContent.visibility = View.VISIBLE
                binding.tvNoDataPlaceholder.visibility = View.GONE
            } else {
                binding.nsvContent.visibility = View.GONE
                binding.tvNoDataPlaceholder.visibility = View.VISIBLE
            }
        }

        binding.fabAddMeal.setOnClickListener {
            val args = Bundle().apply {
                putString(EditRecipeFragment.FLOW_TYPE_KEY, EditRecipeFlowType.FLOW_ADD_RECIPE.name)
            }
            Navigation.findNavController(requireActivity(), R.id.fcvContainer).navigate(R.id.actionNavigateToEditRecipe, args)
        }

        binding.bAllRecipes.setOnClickListener {
            val args = Bundle().apply {
                putInt(RecipeListFragment.LIST_TYPE_KEY, RecipeListFragment.LIST_TYPE_ALL)
            }
            Navigation.findNavController(requireActivity(), R.id.fcvContainer).navigate(R.id.actionNavigateToRecipeList, args)
        }

        binding.tbToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionOpenSettings -> {
                    val fragment = SettingsBottomFragment()
                    fragment.show(childFragmentManager, null)
                    true
                }
                else -> false
            }
        }
    }

    private fun initRecyclers() {
        val frequentAdapter = RecipeCardAdapter(recipeCallback, LIST_FREQUENT_RECIPES)
        (requireActivity().application as MyApplication).appComponent.inject(frequentAdapter)
        viewModel.frequentRecipes.observe(viewLifecycleOwner) {
            frequentAdapter.setList(it)
            binding.groupFrequentRecipes.visibility = when {
                it.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.rvFrequentRecipesList.apply {
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = frequentAdapter
            isNestedScrollingEnabled = false
        }

        val favoriteAdapter = RecipeCardAdapter(recipeCallback, LIST_FAVORITE_RECIPES)
        (requireActivity().application as MyApplication).appComponent.inject(favoriteAdapter)
        viewModel.favoriteRecipes.observe(viewLifecycleOwner) {
            favoriteAdapter.setList(it)
            binding.groupFavoriteRecipes.visibility = when {
                it.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.rvFavoritesList.apply {
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoriteAdapter
            isNestedScrollingEnabled = false
        }

        val allRecipesAdapter = RecipeCardAdapter(recipeCallback, LIST_ALL_RECIPES)
        (requireActivity().application as MyApplication).appComponent.inject(allRecipesAdapter)
        viewModel.allRecipes.observe(viewLifecycleOwner) {
            allRecipesAdapter.setList(it)
            binding.groupAllRecipes.visibility = when {
                it.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.rvAllRecipesList.apply {
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = allRecipesAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun showSnackbar(text: String) {
        Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface RecipeClickCallback {
        fun onFavoriteToggle(recipeId: Int, state: Boolean)
        fun onRecipeClicked(recipeId: Int)
        fun onMoreButtonClick(listType: Int)
    }

    companion object {
        private const val LIST_ALL_RECIPES = 0
        private const val LIST_FAVORITE_RECIPES = 1
        private const val LIST_FREQUENT_RECIPES = 2
    }
}
