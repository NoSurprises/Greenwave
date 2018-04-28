package nick.greenwave.settings

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ToggleButton
import nick.greenwave.DEBUG
import nick.greenwave.R
import nick.greenwave.data.dto.LightSettings
import utils.EXTRAS_LIGHT_INFO
import utils.SECOND_IN_MILLIS
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"

    private var lightSettingsInfo: LightSettings? = null
    private val greenCycle: EditText by lazy { findViewById<EditText>(R.id.input_green_cycle) }
    private val redCycle: EditText by lazy { findViewById<EditText>(R.id.input_red_cycle) }
    private val currentLight: ToggleButton by lazy { findViewById<ToggleButton>(R.id.current_light) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        intent ?: finish()

        val extras = intent.extras
        if (DEBUG) Log.d(TAG, "(14, SettingsActivity.kt) onCreate extras: $extras")
        extras ?: showErrorDialog()
        if (!extras.containsKey(EXTRAS_LIGHT_INFO)) {
            showErrorDialog()
            return
        }
        lightSettingsInfo = extras.getParcelable(EXTRAS_LIGHT_INFO)
        lightSettingsInfo ?: return

        if (lightSettingsInfo!!.isSet()) {
            setGreenCycle(lightSettingsInfo!!.greenCycle)
            setRedCycle(lightSettingsInfo!!.redCycle)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.done -> saveNewSettings()
            else -> return false
        }
        return false
    }

    private fun saveNewSettings() {
        if (DEBUG) Log.d(TAG, "(54, SettingsActivity.kt) saveNewSettings")
        val result = Intent()
        setNewSettings()
        result.putExtra(EXTRAS_LIGHT_INFO, lightSettingsInfo)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun setNewSettings() {
        lightSettingsInfo?.let {
            if (currentLight.isActivated)  {
                // red just started, so start of measurement was 1 green cycle ago
                it.startOfMeasurement = Date().time - it.greenCycle* SECOND_IN_MILLIS
            }
            else {
                it.startOfMeasurement = Date().time
            }

            it.greenCycle = greenCycle.text.toString().toInt()
            it.redCycle = redCycle.text.toString().toInt()
        }

    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this)
                .setTitle("No lighter information provided!")
                .setMessage("Settings will be closed")
                .setPositiveButton("OK", { _: DialogInterface, _: Int -> finish() })
                .show()
    }

    private fun setGreenCycle(v: Int) {
        greenCycle.setText(v.toString())
    }

    private fun setRedCycle(v: Int) {
        redCycle.setText(v.toString())
    }

}