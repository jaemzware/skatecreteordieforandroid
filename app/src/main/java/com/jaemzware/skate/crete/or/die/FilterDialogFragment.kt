import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.jaemzware.skate.crete.or.die.CustomSpinnerAdapter
import com.jaemzware.skate.crete.or.die.MapsActivity
import com.jaemzware.skatecreteordie.R

class FilterDialogFragment : DialogFragment() {
    private var initialFilter: String? = null

    fun setInitialFilter(filter: String) {
        initialFilter = filter
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.filter_dialog, null)

            val spinnerPinType = view.findViewById<Spinner>(R.id.spinnerPinType)
            val pinImageMap = (activity as MapsActivity).pinImageMap

            val pinTypes = mutableListOf<String>("All").apply { addAll(pinImageMap.keys) }
            val adapter = CustomSpinnerAdapter(requireContext(), pinTypes, pinImageMap)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPinType.adapter = adapter

            // Set spinner selection based on initialFilter
            initialFilter?.let { filter ->
                val position = adapter.getPosition(filter)
                if (position != -1) {
                    spinnerPinType.setSelection(position)
                }
            }

            val applyButton = view.findViewById<Button>(R.id.btnApplyFilters)
            applyButton.setOnClickListener {
                val selectedFilter = spinnerPinType.selectedItem.toString()
                (activity as MapsActivity).applyFilters(selectedFilter)
                dismiss()
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}