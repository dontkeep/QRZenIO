package com.al.qrzen.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

object UsbSerialManager {
    var usbSerialPort: UsbSerialPort? = null

    fun initUsbSerial(context: Context) {
        val usbManager =
            context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager

        if (usbManager == null) {
            Log.d("USB", "UsbManager is null")
            return
        }

        usbManager.deviceList
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Log.d("USB", "No USB drivers found")
            return
        }

        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            Log.d("USB", "USB permission not granted for device")
            val ACTION_USB_PERMISSION = "com.al.qrzen.USB_PERMISSION"
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )

            val usbPermissionReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    if (p1?.action == ACTION_USB_PERMISSION) {
                        synchronized(this) {
                            if (p1.getBooleanExtra(
                                    android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED,
                                    false
                                )
                            ) {
                                // Permission granted; try opening the connection again.
                                val connection = usbManager.openDevice(driver.device)
                                if (connection != null) {
                                    val port = driver.ports[0]
                                    try {
                                        port.open(connection)

                                        port.setParameters(
                                            115200,
                                            8,
                                            UsbSerialPort.STOPBITS_1,
                                            UsbSerialPort.PARITY_NONE
                                        )
                                        usbSerialPort = port
                                        Log.d(
                                            "USB",
                                            "USB port opened successfully after permission granted"
                                        )
                                    } catch (e: IOException) {
                                        Log.e("USB", "Error opening USB port", e)
                                    }
                                } else {
                                    Log.d(
                                        "USB",
                                        "Connection still null even after permission granted"
                                    )
                                }
                            } else {
                                Log.d("USB", "Permission denied for USB device")
                            }
                        }
                        context.unregisterReceiver(this)
                    }
                }
            }

            ContextCompat.registerReceiver(
                context,
                usbPermissionReceiver,
                IntentFilter(ACTION_USB_PERMISSION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            usbManager.requestPermission(driver.device, permissionIntent)
            return
        }
        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            Log.d("USB", "USB port opened successfully")
        } catch (e: IOException) {
            Log.e("USB", "Error opening USB port", e)
        }
    }

    fun sendDataToArduino(data: String, onResult: (Boolean, String) -> Unit) {
        val port = usbSerialPort
        if (port == null) {
            onResult(false, "USB port is not available")
            return
        }
        try {
            val dataBytes = data.toByteArray(Charsets.UTF_8)
            val dataLength = dataBytes.size
            val outputBuffer = ByteArray(4 + dataLength)

            outputBuffer[0] = (dataLength ushr 24).toByte()
            outputBuffer[1] = (dataLength ushr 16).toByte()
            outputBuffer[2] = (dataLength ushr 8).toByte()
            outputBuffer[3] = dataLength.toByte()

            System.arraycopy(dataBytes, 0, outputBuffer, 4, dataLength)

            Log.d("USB", "Sending data: $data (Length: $dataLength)")
            port.write(outputBuffer, 1000)

            Thread.sleep(100)
            onResult(true, "Wrote ${outputBuffer.size} bytes (Data: $data, Length: $dataLength)")
        } catch (e: IOException) {
            onResult(false, e.message ?: "IOException occurred")
        } catch (e: InterruptedException) {
            onResult(false, "Thread interrupted")
        }
    }
}