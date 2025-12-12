package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

class MainFragment(crime : Crime) : Fragment() {
    var newCrime = crime
    lateinit var newView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        newView=view
        return view
    }

    override fun onStart() {
        super.onStart()
        if (newCrime.isSolved){
            Snackbar.make(newView, R.string.first_message, Snackbar.LENGTH_LONG).show()
        }
    }
}
