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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.CourtBase
import com.example.group22_opencourt.model.CourtStatus
import com.example.group22_opencourt.model.ImagesRepository
import com.example.group22_opencourt.model.TennisCourt
import kotlinx.coroutines.launch
import java.io.File
import kotlin.text.clear

class AddCourtFragment : Fragment() {
    // initialize views
    private lateinit var courtNameEditText: android.widget.EditText
    private lateinit var clearCourtName: ImageView
    private lateinit var courtTypeSpinner: Spinner
    private lateinit var addressEditText: android.widget.EditText
    private lateinit var clearAddress: ImageView
    private lateinit var numCourtsEditText: android.widget.EditText
    private lateinit var clearNumCourts: ImageView

    // initialize checkboxes
    private lateinit var checkboxLights: CheckBox
    private lateinit var checkboxIndoor: CheckBox
    private lateinit var checkboxWashroom: CheckBox
    private lateinit var checkboxAccessibility: CheckBox

    // initialize tennis/basketball specific views
    private lateinit var layoutTennisAmenities: LinearLayout
    private lateinit var layoutBasketballAmenities: LinearLayout
    private lateinit var checkboxPracticeWall: CheckBox
    private lateinit var checkboxNets: CheckBox

    // Remove photo button
    private lateinit var buttonRemovePhoto : Button
    private var allowAdd = true

    // Photo UI
    private lateinit var addPhotoButton: Button
    private lateinit var courtPhotoImageView: ImageView

    // ViewModel to hold image state
    private val viewModel: AddCourtViewModel by viewModels()

    // Activity result launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    // URI to upload to Firestore
    private var uriToFireStore : Uri? = null

    private fun showKeyboard(editText: android.widget.EditText) {
        // Focus the EditText and show the keyboard
        editText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initActivityResultLaunchers() {
        // Camera permission request launcher
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                // launch camera if permission granted
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // register camera launcher for taking picture
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                // Notify ViewModel that camera photo was captured
                viewModel.onCameraPhotoCaptured()
            } else {
                Toast.makeText(requireContext(), "Failed to Take Photo", Toast.LENGTH_SHORT).show()
            }
        }

        // register gallery picker launcher for taking image from photo gallery
        pickMediaLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                // Update ViewModel with picked image
                viewModel.onGalleryImagePicked(uri)
            }
        }
    }

    private fun showPhotoSourceChooser() {
        // show dialog to choose between camera and gallery
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
        // launch camera if permission granted
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
        // launch gallery picker for images
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun launchCamera() {
        // Prepare file and URI for camera output
        val ctx = requireContext()
        val photoFile = File(
            ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "court_photo_"
        )
        val authority = ctx.packageName + ".fileprovider"
        val uri = FileProvider.getUriForFile(ctx, authority, photoFile)

        // Store prepared URI in ViewModel
        viewModel.onCameraPhotoPrepared(uri)
        // Launch camera to take picture
        takePictureLauncher.launch(uri)
    }

    private fun initViews(view: View) {
        // initialize all views
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
        buttonRemovePhoto = view.findViewById(R.id.buttonRemovePhoto)

        // Set up spinner adapter explicitly (even with entries) to ensure control over items
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.court_type_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            courtTypeSpinner.adapter = adapter
        }

        // Spinner selection listener to toggle tennis/basketball specific sections
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

        // show photo source chooser on button click
        addPhotoButton.setOnClickListener {
            showPhotoSourceChooser()
        }

        // clear photo on remove button click
        buttonRemovePhoto.setOnClickListener {
            viewModel.clearImage()
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
        // Clear text fields on clear icon click
        clearCourtName.setOnClickListener { courtNameEditText.text.clear() }
        clearAddress.setOnClickListener { addressEditText.text.clear() }
        clearNumCourts.setOnClickListener { numCourtsEditText.text.clear() }

        // Observe selected image URI; user will handle UI logic
        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
             courtPhotoImageView.setImageDrawable(null)
            uriToFireStore = uri
            // Update UI based on whether an image is selected
            if (uri != null) {
                 courtPhotoImageView.visibility = View.VISIBLE
                 courtPhotoImageView.setImageURI(uri)
                 addPhotoButton.text = "Update Photo"
                 buttonRemovePhoto.visibility = View.VISIBLE


            } else {
                 courtPhotoImageView.visibility = View.GONE
                 addPhotoButton.text = "Add Photo"
                 buttonRemovePhoto.visibility = View.GONE
            }
        }

        // Apply button: construct proper Court object (Tennis or Basketball)
        view.findViewById<View>(R.id.buttonApply).setOnClickListener {
            if (!allowAdd) {
                return@setOnClickListener
            }
            allowAdd = false
            // get the values from the form
            val name = courtNameEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()
            val totalCourts = numCourtsEditText.text.toString().toIntOrNull() ?: 1
            // get checkbox values
            val washroom = checkboxWashroom.isChecked
            val lights = checkboxLights.isChecked
            val accessibility = checkboxAccessibility.isChecked
            val indoor = !checkboxIndoor.isChecked // map from outdoor checkbox
            // create CourtBase object
            val base = CourtBase(
                name = name,
                address = address,
                washroom = washroom,
                indoor = indoor,
                lights = lights,
                accessibility = accessibility,
                totalCourts = totalCourts,
                courtsAvailable = totalCourts,
                lastUpdate = System.currentTimeMillis(),
                courtStatus = ArrayList(List(totalCourts) { CourtStatus() })
            )

            val repository = CourtRepository.instance
            lifecycleScope.launch {
                // get court details from address
                val result = ImagesRepository.instance.getCourtDetailsFromAddress(requireContext().applicationContext, name + " " + address)
                if (result != null) {
                    val uri = uriToFireStore
                    if (uri != null) {
                        base.photoUri = ImagesRepository.instance.uploadPhotoToFirebase(base.name, uri)
                        if (base.photoUri == ImagesRepository.FAIl) {
                            Toast.makeText(requireContext(), "Court Failed to Upload", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                    val (geoPoint, placeId, formatted) = result
                    base.address = formatted
                    base.placesId = placeId
                    base.geoPoint = geoPoint
                    when (courtTypeSpinner.selectedItemPosition) {
                        0 -> {
                            // store courtBase of tennis type in database
                            val court = TennisCourt(base = base, practiceWall = checkboxPracticeWall.isChecked)
                            repository.addCourt(court) {
                                if (it) {
                                    Toast.makeText(requireContext(), "Court Uploaded", Toast.LENGTH_SHORT).show()
                                    resetForm()

                                } else {
                                    Toast.makeText(requireContext(), "Court Failed to Upload", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        1 -> {
                            // store courtBase of basketball type in database
                            val court = BasketballCourt(base = base, nets = checkboxNets.isChecked)
                            repository.addCourt(court) {
                                if (it) {
                                    Toast.makeText(requireContext(), "Court Uploaded", Toast.LENGTH_SHORT).show()
                                    resetForm()
                                } else {
                                    Toast.makeText(requireContext(), "Court Failed to Upload", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to get Location", Toast.LENGTH_SHORT).show()
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

        // Reset image state via ViewModel
        viewModel.clearImage()

        // Optionally reset ImageView UI here (user may prefer to rely on observer)
        courtPhotoImageView.setImageDrawable(null)
        courtPhotoImageView.visibility = View.GONE
    }



}