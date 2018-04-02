package nick.greenwave.settings

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import nick.greenwave.DEBUG
import nick.greenwave.R
import nick.greenwave.dto.Light
import utils.EXTRAS_LIGHT_INFO

class SettingsActivity : AppCompatActivity(), SettingsView {
    val TAG = "SettingsActivity"

    val greenCycle: EditText by lazy { findViewById<EditText>(R.id.input_green_cycle) }
    val redCycle: EditText by lazy { findViewById<EditText>(R.id.input_red_cycle) }


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
        val lightInfo: Light = extras.getParcelable(EXTRAS_LIGHT_INFO)

        setGreenCycle(lightInfo.greenCycle)
        setRedCycle(lightInfo.redCycle)
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
        return true
    }

    private fun saveNewSettings() {
        if (DEBUG) Log.d(TAG, "(54, SettingsActivity.kt) saveNewSettings")
        finish()
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this)
                .setTitle("No lighter information provided!")
                .setMessage("Settings will be closed")
                .setPositiveButton("OK", { _: DialogInterface, _: Int -> finish() })
                .show()
    }

    override fun setGreenCycle(v: Int) {
        greenCycle.setText(v.toString())
    }

    override fun setRedCycle(v: Int) {
        redCycle.setText(v.toString())
    }

}