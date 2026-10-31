package com.reschange.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.reschange.app.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private val REQ_SHIZUKU = 100

    private val shizukuPermListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ_SHIZUKU && result == PackageManager.PERMISSION_GRANTED) {
            bindShizuku()
        } else {
            Toast.makeText(this, "Shizuku permission denied — trying root fallback", Toast.LENGTH_SHORT).show()
            vm.refresh()
        }
    }

    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        initShizuku()
    }

    private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
        ShizukuBridge.unbind()
        vm.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupToolbar()
        setupRecycler()
        setupButtons()
        setupObservers()

        // Shizuku lifecycle hooks
        Shizuku.addRequestPermissionResultListener(shizukuPermListener)
        Shizuku.addBinderReceivedListenerSticky(shizukuBinderListener)
        Shizuku.addBinderDeadListener(shizukuDeadListener)

        ShizukuBridge.onBindStateChange = { bound ->
            runOnUiThread { vm.refresh() }
        }

        // Initial refresh (will try root if Shizuku not available yet)
        vm.refresh()
    }

    private fun setupToolbar() {
        setSupportActionBar(b.toolbar)
        supportActionBar?.title = "ResChange"
    }

    private fun setupRecycler() {
        b.recyclerPresets.adapter = PresetAdapter(
            presets  = PresetRepository.presets,
            onApply  = { preset -> vm.applyPreset(preset) },
            onCustom = { showCustomDialog() }
        )
    }

    private fun setupButtons() {
        b.btnReset.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Reset Resolution?")
                .setMessage("This will restore your display to its original resolution and DPI.")
                .setPositiveButton("Reset") { _, _ -> vm.reset() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        b.btnAdbHelp.setOnClickListener { showAdbHelp() }
    }

    private fun setupObservers() {
        vm.state.observe(this) { state ->
            // Loading overlay
            b.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Status card
            if (state.currentW > 0 && state.currentH > 0) {
                b.tvCurrentRes.text = "${state.currentW}×${state.currentH}"
                b.tvCurrentDpi.text = "${state.currentDpi} dpi"
            } else {
                b.tvCurrentRes.text = "—"
                b.tvCurrentDpi.text = "No shell access"
            }

            // Shell mode indicator
            b.tvShellMode.text = when (state.shellMode) {
                ResolutionManager.ShellMode.SHIZUKU -> "Shizuku ✓"
                ResolutionManager.ShellMode.ROOT    -> "Root ✓"
                ResolutionManager.ShellMode.NONE    -> "No Access ✗"
            }
            b.tvShellMode.setTextColor(
                getColor(
                    when (state.shellMode) {
                        ResolutionManager.ShellMode.NONE -> android.R.color.holo_red_light
                        else                            -> android.R.color.holo_green_light
                    }
                )
            )

            // Applied badge
            b.chipApplied.visibility = if (state.isApplied) View.VISIBLE else View.GONE
            b.btnReset.isEnabled = state.isApplied

            // Snackbar message
            state.message?.let { msg ->
                Snackbar.make(b.root, msg, Snackbar.LENGTH_SHORT).show()
                vm.clearMessage()
            }
        }
    }

    private fun initShizuku() {
        if (!ShizukuBridge.isShizukuAvailable()) return
        if (ShizukuBridge.hasPermission()) {
            bindShizuku()
        } else {
            ShizukuBridge.requestPermission(REQ_SHIZUKU)
        }
    }

    private fun bindShizuku() {
        ShizukuBridge.bind(packageName, BuildConfig.VERSION_CODE)
    }

    private fun showCustomDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_custom, null)
        val etWidth  = view.findViewById<TextInputEditText>(R.id.etWidth)
        val etHeight = view.findViewById<TextInputEditText>(R.id.etHeight)
        val etDpi    = view.findViewById<TextInputEditText>(R.id.etDpi)

        // Pre-fill with Pixel Mode values as a starting point
        etWidth.setText("480")
        etHeight.setText("854")
        etDpi.setText("120")

        MaterialAlertDialogBuilder(this)
            .setTitle("Custom Resolution")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val w   = etWidth.text.toString().toIntOrNull()
                val h   = etHeight.text.toString().toIntOrNull()
                val dpi = etDpi.text.toString().toIntOrNull()
                if (w != null && h != null && dpi != null && w > 0 && h > 0 && dpi > 0) {
                    vm.applyPreset(DevicePreset("Custom", w, h, dpi))
                } else {
                    Toast.makeText(this, "Enter valid positive numbers", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAdbHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle("ADB Commands")
            .setMessage(
                "If you don't have Shizuku or root, you can use ADB:\n\n" +
                "# Minecraft/pixel look:\n" +
                "adb shell wm size 480x854\n" +
                "adb shell wm density 120\n\n" +
                "# Reset:\n" +
                "adb shell wm size reset\n" +
                "adb shell wm density reset\n\n" +
                "Connect via USB with USB Debugging enabled."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermListener)
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeBinderDeadListener(shizukuDeadListener)
        ShizukuBridge.unbind()
        super.onDestroy()
    }
}
