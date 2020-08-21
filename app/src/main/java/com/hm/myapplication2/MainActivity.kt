package com.hm.myapplication2

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.SimpleArrayMap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.hm.myapplication2.adapter.DeviceListAdapter
import com.hm.myapplication2.databinding.ActivityMainBinding
import com.hm.myapplication2.server.DeviceData
import com.hm.myapplication2.utils.Utils
import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.text.Charsets.UTF_8


class MainActivity : AppCompatActivity(), View.OnClickListener, DeviceListAdapter.onItemClick {

    lateinit var binding: ActivityMainBinding

    lateinit var connectionsClient: ConnectionsClient

    var deviceList: ArrayList<DeviceData> = ArrayList()

    var deviceAdapter: DeviceListAdapter? = null

    var onItemClickListener: DeviceListAdapter.onItemClick? = null

    lateinit var endPointID: String
    lateinit var deviceName: String

    lateinit var notificationManager: NotificationManager

    lateinit var payloadCallback: ReceivedPayloadCallBack


    companion object {

        val REQUIRED_PERMISSIONS = arrayOf<String>(

            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE

        )

        val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
        val STRATEGY: Strategy = Strategy.P2P_CLUSTER

        val READ_REQUEST_CODE = 42
        val ENDPOINT_ID_EXTRA = "com.hm.myapplication2.dev.endpointid";
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        onItemClickListener = this

        setData()

    }

    /**
     *  set Data
     *
     */
    private fun setData() {

        connectionsClient = Nearby.getConnectionsClient(this)
        binding.rvDevices.layoutManager = LinearLayoutManager(this)

        notificationManager =
            this@MainActivity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create or update.
            val channel = NotificationChannel(
                "channel_1",
                "Channel human readable title",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }

        payloadCallback = ReceivedPayloadCallBack()

        binding.btnAdvt.setOnClickListener(this)
        binding.btnDiscover.setOnClickListener(this)
        binding.btnSend.setOnClickListener(this)
        binding.btnSendMsg.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when (v!!.id) {

            R.id.btn_advt -> {
                connectionsClient.startAdvertising(
                    Utils.getDeviceName(this), packageName, connectionLifeCycleCallBack,
                    AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
                ).addOnSuccessListener {

                    Toast.makeText(this, "Advertise successfull", Toast.LENGTH_LONG).show()
                    binding.btnDiscover.visibility = View.GONE
                    binding.btnAdvt.isEnabled = false
                    binding.btnAdvt.isClickable = false
                    // binding.btnSend.visibility = View.GONE

                }.addOnFailureListener {

                    Toast.makeText(this, "Advertise Error", Toast.LENGTH_LONG).show()

                }
            }
            R.id.btn_discover -> {

                connectionsClient.startDiscovery(packageName, object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {

                        if (deviceList.isEmpty() || (deviceList.filter { it.endpoint.contains(p0) }).isEmpty()) {

                            deviceList.add(DeviceData(p0, p1.endpointName))

                        }

                        if (!deviceList.isEmpty()) {

                            deviceAdapter = DeviceListAdapter(deviceList, onItemClickListener)

                            deviceAdapter?.let {
                                binding.rvDevices.adapter = deviceAdapter
                            }
                        }


                    }

                    override fun onEndpointLost(p0: String) {
                        println("Endpoint lost :  ${p0}")
                    }

                }, DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                    .addOnSuccessListener {

                        Toast.makeText(this, "Discovery successfull", Toast.LENGTH_LONG).show()
                        binding.btnAdvt.visibility = View.GONE
                        binding.btnDiscover.isEnabled = false
                        binding.btnDiscover.isClickable = false

                    }.addOnFailureListener {

                        Toast.makeText(this, it!!.localizedMessage, Toast.LENGTH_LONG).show()

                    }

            }
            R.id.btn_send -> {
                endPointID?.let {

                    /* val fileToSend = File(Environment.getExternalStorageDirectory(), "shiva.jpg")
                     try {
                         val filePayload: Payload = Payload.fromFile(fileToSend)
                         connectionsClient.sendPayload(endPointID, filePayload)
                     } catch (e: FileNotFoundException) {
                         Log.e("MyApp", "File not found", e)
                     }*/
                    showImageChooser(endPointID)
                    //val bytesPayload = Payload.fromBytes(byteArrayOf(0xa, 0xb, 0xc, 0xd))
                    // connectionsClient.sendPayload(endPointID,bytesPayload)
                }
            }
            R.id.btn_send_msg -> {
                val bytesPayload =
                    Payload.fromBytes("this is test ".toByteArray(UTF_8))
                connectionsClient.sendPayload(endPointID, bytesPayload)
            }


        }
    }

    /**
     *   connection lifecycle callback
     */
    private val connectionLifeCycleCallBack: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionResult(endpoint: String, p1: ConnectionResolution) {


                when (p1.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        println("device Connected")

                        binding.tvStatus.text = "Connected to Device : ${deviceName}"
                        endPointID = endpoint
                        println("endpoint " + endPointID)

                        binding.btnSend.visibility = View.VISIBLE

                        connectionsClient.stopDiscovery()
                        connectionsClient.stopAdvertising()

                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        println("device Connection Rejected")
                    }
                    else -> {
                        println("device Broken Connection")
                    }
                }
            }

            override fun onDisconnected(p0: String) {

            }

            override fun onConnectionInitiated(p0: String, info: ConnectionInfo) {


                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Accept connection to " + info.getEndpointName())
                    .setMessage("Confirm the code matches on both devices: " + info.getAuthenticationToken())
                    .setPositiveButton("Accept") { dialog, which ->
                        connectionsClient.acceptConnection(p0, payloadCallback)
                        deviceName = info.endpointName
                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        connectionsClient.rejectConnection(p0)
                    }
                    .setIcon(R.mipmap.ic_launcher_round)
                    .show();

            }

        }


    /**
     * received payload callback
     *
     */

    inner class ReceivedPayloadCallBack : PayloadCallback() {

        private val incomingFilePayloads =
            SimpleArrayMap<Long, Payload>()
        private val completedFilePayloads =
            SimpleArrayMap<Long, Payload>()
        private val filePayloadFilenames =
            SimpleArrayMap<Long, String>()

        private val incomingPayloads: SimpleArrayMap<Long, NotificationCompat.Builder> =
            SimpleArrayMap<Long, NotificationCompat.Builder>()
        private val outgoingPayloads: SimpleArrayMap<Long, NotificationCompat.Builder> =
            SimpleArrayMap<Long, NotificationCompat.Builder>()

        public fun sendPayload(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                // No need to track progress for bytes.
                return
            }

            // Build and start showing the notification.
            val notification: NotificationCompat.Builder? =
                buildNotification(payload,  /*isIncoming=*/false)
            notificationManager.notify(payload.id.toInt(), notification!!.build())

            println("payload to send ${payload.id}")
            // Add it to the tracking list so we can update it.
            outgoingPayloads.put(payload.id, notification)
            println("payload outgoing ${outgoingPayloads}")
        }

        private fun buildNotification(
            payload: Payload,
            isIncoming: Boolean
        ): NotificationCompat.Builder? {
            val notification = NotificationCompat.Builder(this@MainActivity, "channel_1")
                .setContentTitle(if (isIncoming) "Receiving..." else "Sending...")
                .setSmallIcon(R.mipmap.ic_launcher_round)


            var indeterminate = false
            if (payload.type == Payload.Type.STREAM) {
                // We can only show indeterminate progress for stream payloads.
                indeterminate = true
            }
            notification.setProgress(100, 0, indeterminate)
            return notification
        }

        override fun onPayloadReceived(p0: String, payload: Payload) {
            println("${payload.type}  payload received")

            if (payload.getType() == Payload.Type.BYTES) {
                val payloadFilenameMessage =
                    String(payload.asBytes()!!, StandardCharsets.UTF_8)
                println("file name ${payloadFilenameMessage}")
                val payloadId: Long = addPayloadFilename(payloadFilenameMessage)
                processFilePayload(payloadId)
            } else if (payload.getType() == Payload.Type.FILE) {


                // Build and start showing the notification.
                val notification =
                    buildNotification(payload, true /*isIncoming*/)
                notificationManager.notify(payload.id.toInt(), notification!!.build())

                // Add it to the tracking list so we can update it.
                incomingPayloads.put(payload.id, notification)
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingFilePayloads.put(payload.getId(), payload)
            }
            /* if(p1.type == Payload.Type.BYTES) {
                 val b: ByteArray? = p1.asBytes()
                 val content = String(b!!)
                 println("Content [$content]")
                 binding.tvMsg.append( content)
             }else {
                 val payloadFile: File? = p1.asFile()?.asJavaFile()

                 // Rename the file.
                 payloadFile?.renameTo(File(payloadFile?.parentFile, "received.jpg"))
             }*/
        }

        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private fun addPayloadFilename(payloadFilenameMessage: String): Long {
            val parts =
                payloadFilenameMessage.split(":".toRegex()).toTypedArray()
            val payloadId = parts[0].toLong()
            val filename: String


            if (parts!!.size == 4) {
                filename =
                    "received_" + System.currentTimeMillis() + "." + Utils.getExtensionFromMimeType(parts[3])
            } else {
                filename = if (parts[1]
                        .equals("image", true)
                ) "received_" + System.currentTimeMillis() + ".jpg" else parts[1]
            }

            /* val filename = if (parts[1].toString()
                     .equals("image", true)
             ) "received_" + System.currentTimeMillis() + ".jpg" else parts[1]*/
            filePayloadFilenames.put(payloadId, filename)
            return payloadId
        }

        private fun processFilePayload(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename = filePayloadFilenames[payloadId]
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                    // allowed to access filepaths from another process directly. Instead, we must open the
                    // uri using our ContentResolver.
                    val uri: Uri = Uri.fromFile(filePayload.asFile()?.asJavaFile())
                    try {
                        // Copy the file to a new location.
                        val inn: InputStream? =
                            this@MainActivity.getContentResolver().openInputStream(uri)
                        copyStream(
                            inn!!,
                            FileOutputStream(File(this@MainActivity.getCacheDir(), filename))
                        )
                    } catch (e: IOException) {
                        // Log the error.
                    } finally {
                        // Delete the original file.
                        this@MainActivity.getContentResolver().delete(uri, null, null)
                    }
                } else {
                    val payloadFile: File? = filePayload.asFile()!!.asJavaFile()

                    // Rename the file.
                    var success = payloadFile?.renameTo(File(payloadFile.parentFile, filename))
                    println("File renamed :${success}")
                }
            }
        }

        /** Copies a stream from one location
         * to another.
         *  */
        @Throws(IOException::class)
        private fun copyStream(inn: InputStream, out: OutputStream) {
            try {
                val buffer = ByteArray(1024)
                var read: Int
                while (inn.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.flush()
            } finally {
                inn.close()
                out.close()
            }
        }

        override fun onPayloadTransferUpdate(p0: String, update: PayloadTransferUpdate) {
            println("${update.status}  payload transfer update ")
            val payloadId = update.payloadId
            var notification: NotificationCompat.Builder? = null
            if (incomingPayloads.containsKey(payloadId)) {
                notification = incomingPayloads[payloadId]
                if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                    // This is the last update, so we no longer need to keep track of this notification.
                    incomingPayloads.remove(payloadId)
                }
            } else if (outgoingPayloads.containsKey(payloadId)) {
                notification = outgoingPayloads[payloadId]
                if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                    // This is the last update, so we no longer need to keep track of this notification.
                    outgoingPayloads.remove(payloadId)
                }
            }

            if (notification == null) {
                return
            }

            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val size: Long = update.totalBytes
                    if (size == -1L) {
                        // This is a stream payload, so we don't need to update anything at this point.
                        return
                    }
                    val percentTransferred =
                        (100.0 * (update.bytesTransferred / update.totalBytes
                            .toDouble())).toInt()
                    notification.setProgress(100, percentTransferred,  /* indeterminate= */false)
                    notification.setContentText("${percentTransferred} %")
                }
                PayloadTransferUpdate.Status.SUCCESS -> {     // SUCCESS always means that we transferred 100%.

                    val payloadId: Long = update.getPayloadId()
                    val payload = incomingFilePayloads.remove(payloadId)
                    completedFilePayloads.put(payloadId, payload)
                    if (payload?.type == Payload.Type.FILE) {
                        processFilePayload(payloadId)
                    }
                    notification
                        .setProgress(100, 100,  /* indeterminate= */false)
                        .setContentText("Transfer complete!")
                }
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED -> notification.setProgress(
                    0,
                    0,
                    false
                ).setContentText("Transfer failed")
                else -> {
                }

            }

            notificationManager.notify(payloadId.toInt(), notification.build())


            /*if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId: Long = update.getPayloadId()
                val payload = incomingFilePayloads.remove(payloadId)
                completedFilePayloads.put(payloadId, payload)
                if (payload?.type == Payload.Type.FILE) {
                    processFilePayload(payloadId)
                }
            }*/
        }

    }

    /**
     *  on item click of recyclerview
     *
     * @param data
     */
    override fun onClick(data: DeviceData) {

        connectionsClient.requestConnection(
            Utils.getDeviceName(this),
            data.endpoint,
            connectionLifeCycleCallBack
        )
    }

    /**
     *
     * Returns true if the app was granted all
     * the permissions. Otherwise, returns false.
     *
     * */
    private fun hasPermissions(
        context: Context,
        permissions: Array<String>
    ): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
            }
        }
    }

    override fun onStop() {
        //connectionsClient.stopAllEndpoints()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return
        }

        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {

                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show()
                return

            }
        }
    }

    /**
     * Fires an intent to spin up the file chooser UI and select an image for sending to endpointId.
     */
    private fun showImageChooser(endpointId: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null
        ) {
            val endpointId: String = endPointID  //resultData.getStringExtra(ENDPOINT_ID_EXTRA)

            // The URI of the file selected by the user.
            val uri: Uri? = resultData.getData()
            val filePayload: Payload
            filePayload = try {
                // Open the ParcelFileDescriptor for this URI with read access.
                val pfd = contentResolver.openFileDescriptor(uri!!, "r")
                Payload.fromFile(pfd!!)
            } catch (e: FileNotFoundException) {
                Log.e("MyApp", "File not found", e)
                return
            }

            /*
             *  Get the file's content URI from the incoming Intent, then
             *  get the file's MIME type
             */
            val mimeType: String? = resultData.data?.let { returnUri ->
                contentResolver.getType(returnUri)
            }

            println("file type : ${mimeType} ")

            // Construct a simple message mapping the ID of the file payload to the desired filename.
            val filenameMessage =
                filePayload.id.toString() + ":" + uri.getLastPathSegment() + ":" + mimeType

            println("file name ${filenameMessage}")

            // Send the filename message as a bytes payload.
            val filenameBytesPayload =
                Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))
            println("endpoint " + endpointId)


            runOnUiThread(Runnable {
                connectionsClient.sendPayload(endpointId, filenameBytesPayload)
                    .addOnSuccessListener {

                    }.addOnFailureListener {
                        println(it.localizedMessage)
                    }
            })

            payloadCallback.sendPayload(endpointId, filePayload)
            // Finally, send the file payload.
            connectionsClient.sendPayload(endpointId, filePayload)
        }
    }


}