package pw.prsk.goodfood

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pw.prsk.goodfood.data.Meal
import pw.prsk.goodfood.databinding.FragmentAddMealBinding
import pw.prsk.goodfood.utils.InputValidator
import pw.prsk.goodfood.viewmodels.MealsViewModel
import java.time.LocalDateTime

class AddMealBottomFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddMealBinding

    // TODO: Mb refactor sharedViewModel
    private val viewModel: MealsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet!!).state = BottomSheetBehavior.STATE_EXPANDED
        }
        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddMealBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nameValidator = InputValidator(binding.tilMealName, resources.getString(R.string.label_name_error))

        binding.bAddMeal.setOnClickListener {
            if (nameValidator.validate()) {
                viewModel.addMeal(
                    Meal(
                        null,
                        binding.tilMealName.editText?.text.toString(),
                        binding.tilDescription.editText?.text.toString(),
                        LocalDateTime.now(),
                        0,
                        0
                    )
                )
                dismiss()
            }
        }
    }
}