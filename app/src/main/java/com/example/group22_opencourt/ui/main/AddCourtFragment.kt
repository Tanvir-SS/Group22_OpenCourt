package com.example.group22_opencourt.ui.main

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import android.widget.Button
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.CourtBase
import com.example.group22_opencourt.model.ImagesRepository
import com.example.group22_opencourt.model.TennisCourt
import kotlinx.coroutines.launch
import java.io.File
import kotlin.text.clear

class AddCourtFragment : Fragment() {
    private lateinit var courtNameEditText: android.widget.EditText
    private lateinit var clearCourtName: ImageView
    private lateinit var courtTypeSpinner: Spinner
    private lateinit var addressEditText: android.widget.EditText
    private lateinit var clearAddress: ImageView
    private lateinit var numCourtsEditText: android.widget.EditText
    private lateinit var clearNumCourts: ImageView

    private lateinit var checkboxLights: CheckBox
    private lateinit var checkboxIndoor: CheckBox
    private lateinit var checkboxWashroom: CheckBox
    private lateinit var checkboxAccessibility: CheckBox

    private lateinit var layoutTennisAmenities: LinearLayout
    private lateinit var layoutBasketballAmenities: LinearLayout
    private lateinit var checkboxPracticeWall: CheckBox
    private lateinit var checkboxNets: CheckBox

    private var allowAdd = true

    // Photo UI
    private lateinit var addPhotoButton: Button
    private lateinit var courtPhotoImageView: ImageView

    // Image state
    private var selectedImageUri: Uri? = null
    private var currentPhotoUri: Uri? = null

    // Activity result launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    private fun showKeyboard(editText: android.widget.EditText) {
        editText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initActivityResultLaunchers() {
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                currentPhotoUri?.let { uri ->
                    selectedImageUri = uri
                    courtPhotoImageView.visibility = View.VISIBLE
                    courtPhotoImageView.setImageURI(uri)
                }
            } else {
                Toast.makeText(requireContext(), "Failed to take photo", Toast.LENGTH_SHORT).show()
            }
        }

        pickMediaLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                courtPhotoImageView.visibility = View.VISIBLE
                courtPhotoImageView.setImageURI(uri)
            }
        }
    }

    private fun showPhotoSourceChooser() {
        val options = arrayOf("Take photo", "Use gallery")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startCameraFlow()
                    1 -> launchGalleryPicker()
                }
            }
            .show()
    }

    private fun startCameraFlow() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchGalleryPicker() {
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val photoFile = File.createTempFile(
            "court_photo_",
            ".jpg",
            ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        )
        val authority = ctx.packageName + ".fileprovider"
        val uri = FileProvider.getUriForFile(ctx, authority, photoFile)
        currentPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun initViews(view: View) {
        courtNameEditText = view.findViewById(R.id.editTextCourtName)
        clearCourtName = view.findViewById(R.id.clearCourtName)
        courtTypeSpinner = view.findViewById(R.id.spinnerCourtType)
        addressEditText = view.findViewById(R.id.editTextAddress)
        clearAddress = view.findViewById(R.id.clearAddress)
        numCourtsEditText = view.findViewById(R.id.editTextNumCourts)
        clearNumCourts = view.findViewById(R.id.clearNumCourts)

        checkboxLights = view.findViewById(R.id.checkboxLights)
        checkboxIndoor = view.findViewById(R.id.checkboxIndoor)
        checkboxWashroom = view.findViewById(R.id.checkboxWashroom)
        checkboxAccessibility = view.findViewById(R.id.checkboxAccessibility)

        layoutTennisAmenities = view.findViewById(R.id.layoutTennisAmenities)
        layoutBasketballAmenities = view.findViewById(R.id.layoutBasketballAmenities)
        checkboxPracticeWall = view.findViewById(R.id.checkboxPracticeWall)
        checkboxNets = view.findViewById(R.id.checkboxNets)

        // Photo views
        addPhotoButton = view.findViewById(R.id.buttonAddPhoto)
        courtPhotoImageView = view.findViewById(R.id.imageViewCourtPhoto)

        // Set up spinner adapter explicitly (even with entries) to ensure control over items
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.court_type_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            courtTypeSpinner.adapter = adapter
        }

        courtTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> { // Tennis
                        layoutTennisAmenities.visibility = View.VISIBLE
                        layoutBasketballAmenities.visibility = View.GONE
                        checkboxNets.isChecked = false
                    }
                    1 -> { // Basketball
                        layoutTennisAmenities.visibility = View.GONE
                        layoutBasketballAmenities.visibility = View.VISIBLE
                        checkboxPracticeWall.isChecked = false
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Default to tennis section visible
                layoutTennisAmenities.visibility = View.VISIBLE
                layoutBasketballAmenities.visibility = View.GONE
            }
        }

        addPhotoButton.setOnClickListener {
            showPhotoSourceChooser()
        }

        // Existing layout click listeners now only for text inputs
        view.findViewById<View>(R.id.layoutCourtName).setOnClickListener {
            showKeyboard(courtNameEditText)
        }
        view.findViewById<View>(R.id.layoutAddress).setOnClickListener {
            showKeyboard(addressEditText)
        }
        view.findViewById<View>(R.id.layoutNumCourts).setOnClickListener {
            showKeyboard(numCourtsEditText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivityResultLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_court, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        clearCourtName.setOnClickListener { courtNameEditText.text.clear() }
        clearAddress.setOnClickListener { addressEditText.text.clear() }
        clearNumCourts.setOnClickListener { numCourtsEditText.text.clear() }

        // Apply button: construct proper Court object (Tennis or Basketball)
        view.findViewById<View>(R.id.buttonApply).setOnClickListener {
            if (!allowAdd) {
                return@setOnClickListener
            }
            allowAdd = false
            val name = courtNameEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()
            val totalCourts = numCourtsEditText.text.toString().toIntOrNull() ?: 1

            val washroom = checkboxWashroom.isChecked
            val lights = checkboxLights.isChecked
            val accessibility = checkboxAccessibility.isChecked
            val indoor = !checkboxIndoor.isChecked // map from outdoor checkbox

            val base = CourtBase(
                name = name,
                address = address,
                washroom = washroom,
                indoor = indoor,
                lights = lights,
                accessibility = accessibility,
                totalCourts = totalCourts,
                courtsAvailable = totalCourts,
                lastUpdate = System.currentTimeMillis()
            )
            val repository = CourtRepository.instance
            lifecycleScope.launch {
                val result = ImagesRepository.instance.getCourtDetailsFromAddress(requireContext().applicationContext, name + " " + address)
                if (result != null) {
                    val (geoPoint, placeId, formatted) = result
                    base.address = formatted
                    base.placesId = placeId
                    base.geoPoint = geoPoint
                    when (courtTypeSpinner.selectedItemPosition) {
                        0 -> {
                            val court = TennisCourt(base = base, practiceWall = checkboxPracticeWall.isChecked)
                            repository.addCourt(court) {
                                if (it) {
                                    Toast.makeText(requireContext(), "Court uploaded", Toast.LENGTH_SHORT).show()
                                    resetForm()

                                } else {
                                    Toast.makeText(requireContext(), "Court failed to uploaded", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        1 -> {
                            val court = BasketballCourt(base = base, nets = checkboxNets.isChecked)
                            repository.addCourt(court) {
                                if (it) {
                                    Toast.makeText(requireContext(), "Court uploaded", Toast.LENGTH_SHORT).show()
                                    resetForm()
                                } else {
                                    Toast.makeText(requireContext(), "Court failed to uploaded", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
                }
                allowAdd = true
            }

        }
    }

    private fun resetForm() {
        // Text fields
        courtNameEditText.text.clear()
        addressEditText.text.clear()
        numCourtsEditText.text.clear()

        // Spinner: go back to first option (e.g. Tennis)
        courtTypeSpinner.setSelection(0)

        // Shared amenities
        checkboxLights.isChecked = false
        checkboxIndoor.isChecked = false
        checkboxWashroom.isChecked = false
        checkboxAccessibility.isChecked = false

        // Tennis vs Basketball sections
        checkboxPracticeWall.isChecked = false
        checkboxNets.isChecked = false
        layoutTennisAmenities.visibility = View.VISIBLE
        layoutBasketballAmenities.visibility = View.GONE

        // Image state
        selectedImageUri = null
        currentPhotoUri = null
        courtPhotoImageView.setImageDrawable(null)
        courtPhotoImageView.visibility = View.GONE
    }



}