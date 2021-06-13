package org.abishek.vaccinechecker.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.abishek.vaccinechecker.MainActivity
import org.abishek.vaccinechecker.R

class onBoardingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_on_boarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button_get_started: Button = view.findViewById(R.id.button_get_started)
        button_get_started.setOnClickListener {
            (activity as MainActivity).transitionToStartChecking();
        }
    }
}