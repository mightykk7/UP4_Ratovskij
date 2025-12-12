package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var crimeTitle: EditText
    private lateinit var crimeSolved: CheckBox
    private lateinit var crimeDate: Button
    private lateinit var suspectButton: Button
    private lateinit var reportButton: Button
    private lateinit var callButton: Button
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crimeId = arguments?.getSerializable("crime_id") as UUID? ?: UUID.randomUUID()
        crime = Crime(id = crimeId)

        if (arguments == null || arguments?.getSerializable("crime_id") == null) {
            crime.date = Date()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        crimeTitle = view.findViewById(R.id.crime_title)
        crimeSolved = view.findViewById(R.id.crime_solved)
        crimeDate = view.findViewById(R.id.crime_date)
        suspectButton = view.findViewById(R.id.crime_suspect)
        reportButton = view.findViewById(R.id.crime_report)
        callButton = view.findViewById(R.id.callBtn)

        crimeTitle.setText(crime.title)
        crimeSolved.isChecked = crime.isSolved
        crimeDate.text = SimpleDateFormat("EEEE dd MMMM yyyy", Locale("ru", "RU")).format(crime.date)
        crimeDate.isEnabled = crimeSolved.isChecked

        if (crime.suspect.isBlank()) {
            suspectButton.text = getString(R.string.crime_suspect_text)
        } else {
            suspectButton.text = crime.suspect
        }

        reportButton.isEnabled = crimeTitle.text.toString().isNotBlank()
        callButton.isEnabled = phoneNumber.isNotBlank() && crime.suspect.isNotBlank()

        crimeTitle.addTextChangedListener {
            crime.title = it.toString()
            reportButton.isEnabled = it.toString().isNotBlank()
        }

        crimeSolved.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
            crimeDate.isEnabled = isChecked
        }

        suspectButton.setOnClickListener {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            if (requireActivity().packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivityForResult(pickContactIntent, 1)
                } else {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(android.Manifest.permission.READ_CONTACTS),
                        100
                    )
                }
            }
        }

        reportButton.setOnClickListener {
            val solvedString = if (crime.isSolved) getString(R.string.crime_report_solved) else getString(R.string.crime_report_unsolved)
            val suspect = if (crime.suspect.isBlank()) getString(R.string.crime_report_no_suspect) else getString(R.string.crime_report_suspect, crime.suspect)

            // Используем русский формат даты
            val russianDate = SimpleDateFormat("EEE, MMM dd", Locale("ru", "RU")).format(crime.date)
            val report = getString(R.string.crime_report, crime.title, russianDate, solvedString, suspect)

            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, report)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { startActivity(Intent.createChooser(it, getString(R.string.send_report))) }
        }

        callButton.setOnClickListener {
            if (phoneNumber.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.replace("[^0-9+]".toRegex(), "")}")))
            }
        }

        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                requireActivity().contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        crime.suspect = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        suspectButton.text = crime.suspect

                        val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            requireActivity().contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                phoneNumber = if (phoneCursor.moveToFirst()) phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: "" else ""
                                callButton.isEnabled = phoneNumber.isNotBlank() && crime.suspect.isNotBlank()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(crimeId: UUID) = CrimeFragment().apply {
            arguments = Bundle().apply { putSerializable("crime_id", crimeId) }
        }
    }
}