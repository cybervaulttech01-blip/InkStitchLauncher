package com.example.inkstitchlauncher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 120)
        }

        val btnSetup = Button(this).apply { text = "1. SETUP (Install Inkscape + Ink/Stitch)" }
        val btnStart = Button(this).apply { text = "2. START Ink/Stitch" }
        val btnStop = Button(this).apply { text = "3. STOP Server" }
        val btnRestart = Button(this).apply { text = "4. RESTART Server" }

        layout.addView(btnSetup)
        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(btnRestart)
        setContentView(layout)

        btnSetup.setOnClickListener {
            val setupScript = """
                export DEBIAN_FRONTEND=noninteractive
                yes | pkg update
                pkg upgrade -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold"
                pkg install -y x11-repo
                pkg install -y tigervnc proot-distro wget tar xz-utils openbox
                proot-distro install debian
                proot-distro login debian -- bash -c "
                    apt update && DEBIAN_FRONTEND=noninteractive apt install -y inkscape wget tar xz-utils
                    mkdir -p ~/.config/inkscape/extensions
                    cd /tmp
                    wget -O inkstitch.tar.xz https://github.com/inkstitch/inkstitch/releases/download/v3.3.0/inkstitch-v3.3.0-linux-aarch64.tar.xz
                    tar -xf inkstitch.tar.xz -C ~/.config/inkscape/extensions/
                "
                mkdir -p ~/.vnc
                cat << 'EOF' > ~/.vnc/xstartup
#!/bin/sh
openbox &
proot-distro login debian --shared-tmp -- env DISPLAY=:1 inkscape &
EOF
                chmod +x ~/.vnc/xstartup
                echo "=========================================="
                echo "SETUP FINISHED! Enter a VNC password below:"
                vncserver :1
            """.trimIndent()
            runTermuxCommand(setupScript, background = false)
        }

        btnStart.setOnClickListener {
            runTermuxCommand("vncserver -kill :1 2>/dev/null; vncserver :1 -geometry 1280x720", background = true)
            Toast.makeText(this, "Launching Ink/Stitch... Connect to 127.0.0.1:5901", Toast.LENGTH_LONG).show()
            val vncIntent = packageManager.getLaunchIntentForPackage("com.realvnc.viewer.android")
            if (vncIntent != null) startActivity(vncIntent)
        }

        btnStop.setOnClickListener {
            runTermuxCommand("vncserver -kill :1", background = true)
            Toast.makeText(this, "Server Stopped", Toast.LENGTH_SHORT).show()
        }

        btnRestart.setOnClickListener {
            runTermuxCommand("vncserver -kill :1; vncserver :1 -geometry 1280x720", background = true)
            Toast.makeText(this, "Server Restarted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runTermuxCommand(cmd: String, background: Boolean) {
        val intent = Intent().apply {
            action = "com.termux.RUN_COMMAND"
            component = ComponentName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", background)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: Enable allow-external-apps=true in Termux first!", Toast.LENGTH_LONG).show()
        }
    }
}
