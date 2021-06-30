package org.abishek.vaccinechecker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.abishek.vaccinechecker.Constants
import org.abishek.vaccinechecker.MainActivity
import org.abishek.vaccinechecker.R

class StartCheckingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_checking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radio_group_search_method = view.findViewById<RadioGroup>(R.id.radio_group_search_method)
        radio_group_search_method.setOnCheckedChangeListener { group, checkedId ->
            if(checkedId == R.id.radio_button_district) {
                view.findViewById<TextInputLayout>(R.id.outlined_textfield_layout_pincode).visibility = View.GONE
                view.findViewById<TextInputLayout>(R.id.menu_district_layout).visibility = View.VISIBLE
            } else {
                view.findViewById<TextInputLayout>(R.id.menu_district_layout).visibility = View.GONE
                view.findViewById<TextInputLayout>(R.id.outlined_textfield_layout_pincode).visibility = View.VISIBLE
            }
        }

        val button_start_notify = view.findViewById<Button>(R.id.button_start_notify)
        val district_picker = view.findViewById<AutoCompleteTextView>(R.id.auto_district_picker)
        val adapter = ArrayAdapter(requireContext(), R.layout.layout_adapter_item, Constants.district_names)
        district_picker.setAdapter(adapter)
        val dose_category = view.findViewById<RadioGroup>(R.id.radio_group_dose)
        val button_cancel_notify = view.findViewById<Button>(R.id.button_cencel_notify)

        button_start_notify.setOnClickListener {
            val age_category_45 = view.findViewById<Chip>(R.id.chip_45).isChecked
            val age_category_18 = view.findViewById<Chip>(R.id.chip_18).isChecked
            val price_free = view.findViewById<Chip>(R.id.chip_free).isChecked
            val price_paid = view.findViewById<Chip>(R.id.chip_paid).isChecked
            val checkedDose = dose_category.checkedRadioButtonId
            val dose: Int
            if(checkedDose == R.id.radio_button_dose2) {
                dose = Constants.ConstantSharedPreferences.dose2
            } else {
                dose = Constants.ConstantSharedPreferences.dose1
            }
            if(age_category_18 || age_category_45) {
                if(price_free || price_paid) {
                    val checkedId = radio_group_search_method.checkedRadioButtonId
                    if (checkedId == R.id.radio_button_district) {
                        val district_name = district_picker.text.toString()
                        if(district_name in Constants.district_names) {
                            val district_position = Constants.district_names.indexOf(district_name)
                            val district_code =
                                Constants.district_ids.get(district_position)
                            (activity as MainActivity).startChecking(
                                district_code,
                                age_category_18,
                                age_category_45,
                                Constants.SearchWith.district,
                                dose,
                                price_free,
                                price_paid
                            )
                        } else {
                            Snackbar.make(view, "Please enter a valid district name", Snackbar.LENGTH_LONG)
                                .show()
                        }
                    } else {
                        val pincode =
                            view.findViewById<TextInputEditText>(R.id.textfield_pincode).text.toString()
                        if ((pincode.isEmpty()) || (pincode.length != 6)) {
                            Snackbar.make(view, "Please enter a valid pincode", Snackbar.LENGTH_LONG)
                                .show()
                        } else {
                            (activity as MainActivity).startChecking(
                                pincode,
                                age_category_18,
                                age_category_45,
                                Constants.SearchWith.pincode,
                                dose,
                                price_free,
                                price_paid
                            )
                        }
                    }
                } else {
                    Snackbar.make(view, "Please select a price category", Snackbar.LENGTH_LONG)
                        .show()
                }
            } else {
                Snackbar.make(view, "Please select at least one age category", Snackbar.LENGTH_LONG)
                    .show()
            }
        }

        button_cancel_notify.setOnClickListener {
            (activity as MainActivity).navigateAsSharedPreference()
        }
    }
}