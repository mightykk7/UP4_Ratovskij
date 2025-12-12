package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val REQUEST_CONTACT = 1
private const val REQUEST_READ_CONTACTS = 100
private const val DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy"

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var crimeTitle: EditText
    private lateinit var crimeSolved: CheckBox
    private lateinit var crimeDate: Button
    private lateinit var suspectButton: Button
    private lateinit var reportButton: Button
    private lateinit var callButton: Button
    private lateinit var crimeRepository: CrimeRepository
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crimeRepository = CrimeRepository.getInstance(requireContext())
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID? ?: UUID.randomUUID()
        crime = Crime(id = crimeId)

        if (arguments == null || arguments?.getSerializable(ARG_CRIME_ID) == null) {
            crime.date = Date()
        }

        lifecycleScope.launch {
            val existing = crimeRepository.getCrime(crimeId)
            if (existing != null) {
                crime = existing
            } else {
                crimeRepository.insertCrime(crime)
            }
            updateUI()
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

        val outputFormat = SimpleDateFormat("EEEE dd MMMM yyyy", Locale("ru", "RU"))
        crimeDate.text = outputFormat.format(crime.date)

        crimeTitle.setText(crime.title)
        crimeSolved.isChecked = crime.isSolved
        updateUI()

        crimeTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                crime.title = s.toString()
                lifecycleScope.launch { crimeRepository.updateCrime(crime) }
                updateButtonStates()
            }
        })

        crimeSolved.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
            lifecycleScope.launch { crimeRepository.updateCrime(crime) }
            updateButtonStates()
        }

        return view
    }

    private fun updateButtonStates() {
        crimeDate.isEnabled = crimeSolved.isChecked
        reportButton.isEnabled = crimeTitle.text.toString().isNotBlank()
        callButton.isEnabled = phoneNumber.isNotBlank() && crime.suspect.isNotBlank()
    }

    override fun onStart() {
        super.onStart()

        suspectButton.apply {
            // Проверяем разрешение на чтение контактов
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            val packageManager = requireActivity().packageManager
            val resolvedActivity = packageManager.resolveActivity(
                pickContactIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            isEnabled = resolvedActivity != null

            setOnClickListener {
                // Проверяем разрешение перед открытием контактов
                if (checkContactsPermission()) {
                    startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                }
            }
        }

        reportButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }
            val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.send_report))
            startActivity(chooserIntent)
        }

        callButton.setOnClickListener {
            if (phoneNumber.isNotBlank()) {
                val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
                startActivity(dialIntent)
            }
        }
    }

    // Проверяем и запрашиваем разрешение на чтение контактов
    private fun checkContactsPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                REQUEST_READ_CONTACTS
            )
            false
        }
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение получено, можно открыть контакты
                    val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                } else {
                    // Пользователь отказал в разрешении
                    // Можно показать сообщение
                }
            }
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(crime.date)
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    private fun updateUI() {
        crimeTitle.setText(crime.title)
        crimeSolved.isChecked = crime.isSolved

        val outputFormat = SimpleDateFormat("EEEE dd MMMM yyyy", Locale("ru", "RU"))
        crimeDate.text = outputFormat.format(crime.date)

        if (crime.suspect.isBlank()) {
            suspectButton.text = getString(R.string.crime_suspect_text)
        } else {
            suspectButton.text = crime.suspect
        }

        updateButtonStates()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) return

        if (requestCode == REQUEST_CONTACT) {
            val contactUri: Uri? = data.data
            val queryFields = arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID
            )

            val cursor = requireActivity().contentResolver.query(
                contactUri!!,
                queryFields,
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val suspectName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))

                    getPhoneNumberFromContact(contactId)

                    crime.suspect = suspectName
                    lifecycleScope.launch {
                        crimeRepository.updateCrime(crime)
                        updateUI()
                    }
                }
            }
        }
    }

    private fun getPhoneNumberFromContact(contactId: String) {
        // Для получения номера тоже нужно разрешение READ_CONTACTS
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            phoneNumber = ""
            return
        }

        val phoneCursor = requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        phoneCursor?.use {
            if (it.moveToFirst()) {
                phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
            } else {
                phoneNumber = ""
            }
        }

        updateButtonStates()
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply { arguments = args }
        }
    }
}