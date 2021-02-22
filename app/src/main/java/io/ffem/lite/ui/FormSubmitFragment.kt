package io.ffem.lite.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import io.ffem.lite.R
import io.ffem.lite.common.Constants
import io.ffem.lite.data.TestResult
import io.ffem.lite.databinding.FragmentFormSubmitBinding
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.WidgetUtil.setStatusColor
import io.ffem.lite.util.setMultiLineCapSentencesAndDoneAction
import io.ffem.lite.util.snackBar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class FormSubmitFragment : Fragment() {
    private var _binding: FragmentFormSubmitBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()
    private lateinit var form: TestResult
    private var submitted: Boolean = false
    private val mainScope = MainScope()
    private var locationProgressDialog: Dialog? = null
    private var requestingLocationUpdates: Boolean = false
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var settingsClient: SettingsClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = false
                    activity?.onBackPressed()
                    isEnabled = true
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormSubmitBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            if (isDiagnosticMode()) {
                toolbar.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.diagnostic
                    )
                )
            } else {
                toolbar.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    )
                )
            }
        }
        super.onViewCreated(view, savedInstanceState)

        form = model.form

        showHideControls()

        b.submitBtn.setOnClickListener {
            when {
                b.sourceDescEdit.text.toString().isEmpty() -> {
                    b.sourceDescEdit.error = requireContext().getString(R.string.required_error)
                    b.sourceDescEdit.requestFocus()
                }
                b.sourceSelect.text.toString().isEmpty() -> {
                    b.sourceSelect.error = requireContext().getString(R.string.required_error)
                    b.sourceSelect.requestFocus()
                }
                else -> {
                    submitForm()
                }
            }
        }

        b.commentEdit.setText(model.form.comment)
        b.commentEdit.setHorizontallyScrolling(false)
        b.commentEdit.maxLines = Integer.MAX_VALUE
        b.commentEdit.setMultiLineCapSentencesAndDoneAction()
        b.commentEdit.doOnTextChanged { _, _, _, _ ->
            validateComment()
        }
        validateComment()

        if (form.longitude != null && !form.longitude!!.isNaN()) {
            val gpsLocation = form.latitude.toString() + " / " + form.longitude.toString()
            b.latitudeText.text = gpsLocation
            val accuracyText = "${form.geoAccuracy} m"
            b.accuracyText.text = accuracyText
        }

        b.locationButton.setOnClickListener {
            startLocation()
        }

        b.redoButton.setOnClickListener {
            startLocation()
        }

        b.sourceDescEdit.setText(model.form.source)

        b.sourceDescEdit.doAfterTextChanged {
            validateDescription()
        }

        b.sourceDescEdit.error = null

        validateDescription()

        b.sourceSelect.doAfterTextChanged {
            validateDescription()
        }

        val waterSource = resources.getStringArray(R.array.WaterSource)

        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_list_item_1, waterSource
        )
        b.sourceSelect.setAdapter(adapter)

        createLocationCallback()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)

        if (menuVisible && b.sourceDescEdit.requestFocus()) {
            val imm =
                b.sourceDescEdit.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        }
    }

    private fun validateDescription() {
        b.descLayout.setStatusColor(
            !b.sourceDescEdit.text.isNullOrEmpty()
                    && !b.sourceSelect.text.isNullOrEmpty(),
            true
        )
    }

    private fun validateComment() {
        b.commentLayout.setStatusColor(!b.commentEdit.text.isNullOrEmpty(), false)
    }

    private fun submitForm() {
        saveData()
        submitted = true
        (requireActivity() as TestActivity).submitResult()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.details)
    }

    override fun onPause() {
        saveData()
        LocalBroadcastManager.getInstance(requireContext())
            .sendBroadcast(Intent(Constants.BROADCAST_HIDE_KEYBOARD))
        super.onPause()
    }

    private fun saveData() {
        if (!b.commentEdit.text.isNullOrEmpty()) {
            model.form.comment = b.commentEdit.text.toString().trim()
        }
        model.form.source = b.sourceDescEdit.text.toString().trim()
        model.form.sourceType = b.sourceSelect.text.toString().trim()
        val dao = model.db.resultDao()
        dao.update(model.form)
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                if (!location.longitude.isNaN()) {
                    form.latitude = location.latitude
                    form.longitude = location.longitude
                    form.geoAccuracy = location.accuracy

                    b.locationLayout.visibility = VISIBLE
                    val gpsLocation =
                        location.latitude.toString() + " / " + location.longitude.toString()
                    val gpsAccuracy = "${location.accuracy} m"

                    b.latitudeText.text = gpsLocation
                    b.accuracyText.text = gpsAccuracy

                    locationProgressDialog?.findViewById<TextView>(R.id.location_text)
                        ?.text = gpsLocation
                    locationProgressDialog?.findViewById<TextView>(R.id.accuracy_text)
                        ?.text = gpsAccuracy
                    locationProgressDialog?.findViewById<TextView>(R.id.accept_button)
                        ?.isEnabled = true
                    locationProgressDialog?.findViewById<TextView>(R.id.accept_button)
                        ?.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.link_green
                            )
                        )
                    val dao = model.db.resultDao()
                    dao.update(form)
                }
            }
        }
    }

    private fun checkLocationIsTurnedOn() {
        buildLocationSettingsRequest()
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(requireActivity()) {
                startLocationUpdates()
            }
            .addOnFailureListener(requireActivity()) { status ->
                when ((status as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            startLocationSetting.launch(
                                IntentSenderRequest.Builder((status as ResolvableApiException).resolution.intentSender)
                                    .build()
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            showHideControls()
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        requestingLocationUpdates = false
                        if (isAirplaneModeOn(requireContext())) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.airplane_mode_off),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            requestLocationPermission()
                            startLocationUpdates()
                        }
                        locationProgressDialog?.dismiss()
                    }
                }
            }
    }

    private fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
    }

    private fun requestLocationPermission() {
        Toast.makeText(requireContext(), getString(R.string.location_permission), Toast.LENGTH_LONG)
            .show()
        val onGPSIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(onGPSIntent)
    }

    private val startLocationSetting =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                startLocationUpdates()
            } else {
                activity?.findViewById<CoordinatorLayout>(R.id.coordinator_lyt)
                    ?.snackBar(getString(R.string.location_permission))
                showHideControls()
            }
        }

    private fun showHideControls() {
        if (locationProgressDialog != null) {
            locationProgressDialog!!.dismiss()
        }
        if (!b.latitudeText.text.isNullOrEmpty()) {
            b.locationButton.visibility = GONE
            b.redoButton.visibility = VISIBLE
            b.locationView.setStatusColor(true, required = false)
        } else {
            b.locationButton.visibility = VISIBLE
            b.redoButton.visibility = GONE
            b.locationView.setStatusColor(false, required = false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            fusedLocationClient!!.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: Exception) {
            showHideControls()
        }
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted) {
                mainScope.launch {
                    showLocationProgressDialog()
                    delay(100)
                    checkLocationIsTurnedOn()
                }
            } else {
                showHideControls()
                activity?.findViewById<CoordinatorLayout>(R.id.coordinator_lyt)
                    ?.snackBar(getString(R.string.location_permission))
            }
        }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        createLocationRequest()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
    }

    private fun startLocation() {
        cancelLocation()
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        settingsClient = LocationServices.getSettingsClient(requireContext())
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun showLocationProgressDialog() {

        locationProgressDialog = Dialog(requireContext())
        locationProgressDialog!!.setCancelable(false)

        locationProgressDialog?.also { dialog ->
            dialog.setContentView(R.layout.fragment_location_progress)

            dialog.findViewById<TextView>(R.id.accept_button).setOnClickListener {
                dialog.dismiss()
                cancelLocation()
                showHideControls()
            }

            dialog.findViewById<TextView>(R.id.cancel_txt).setOnClickListener {
                dialog.dismiss()
                cancelLocation()
                showHideControls()
            }
            dialog.show()
            dialog.findViewById<Chronometer>(R.id.chronometer).start()
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 10000
        locationRequest.numUpdates = 5
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun cancelLocation() {
        if (fusedLocationClient != null) {
            fusedLocationClient!!.removeLocationUpdates(locationCallback)
        }
        fusedLocationClient = null
    }

    override fun onDestroy() {
        mainScope.cancel()
        cancelLocation()
        super.onDestroy()
    }
}
