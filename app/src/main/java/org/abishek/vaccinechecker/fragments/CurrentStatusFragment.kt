package org.abishek.vaccinechecker.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.abishek.vaccinechecker.MainActivity
import org.abishek.vaccinechecker.R

class CurrentStatusFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_current_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.textview_status).text =
            (activity as MainActivity).getStatusText()
        val button_stop_searching = view.findViewById<MaterialButton>(R.id.button_stop_searching)
        button_stop_searching.setOnClickListener{
            (activity as MainActivity).cancelAllChecking()
            (activity as MainActivity).navigateAsSharedPreference()
        }
        val button_edit_instructions = view.findViewById<MaterialButton>(R.id.button_edit_instructions)
        button_edit_instructions.setOnClickListener {
            (activity as MainActivity).transitionToStartChecking()
        }
        val image_view_settings = view.findViewById<ImageView>(R.id.image_view_settings)
        image_view_settings.setOnClickListener {
            (activity as MainActivity).transitionToSettings()
        }
        val button_share_app = view.findViewById<MaterialButton>(R.id.button_share_app)
        button_share_app.setOnClickListener{
            (activity as MainActivity).shareApp()
        }
    }
}