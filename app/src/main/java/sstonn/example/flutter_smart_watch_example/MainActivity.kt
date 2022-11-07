package sstonn.example.flutter_smart_watch_example

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.CapabilityApi.FILTER_ALL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import sstonn.example.flutter_smart_watch_example.databinding.ActivityMainBinding
import java.io.InputStream
import java.lang.Exception
import java.util.*

class MainActivity : Activity(), MessageClient.OnMessageReceivedListener, DataClient.OnDataChangedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient
    private lateinit var dataClient: DataClient
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAllClients()
        messageClient.addListener(this)
        dataClient.addListener(this)
        binding.sendMessageBtn.setOnClickListener {
            coroutineScope.launch {
                nodeClient.connectedNodes.await().forEach { node ->
                    sendExampleMessage(node)
                }
            }
        }
        binding.sendMapDataBtn.setOnClickListener {
            sendExampleData()
        }
        addNewCapacity()
    }

    private fun initAllClients() {
        capabilityClient = Wearable.getCapabilityClient(this)
        messageClient = Wearable.getMessageClient(this)
        nodeClient = Wearable.getNodeClient(this)
        dataClient = Wearable.getDataClient(this)
    }

    private fun addNewCapacity() {

        coroutineScope.launch {
            try {
                capabilityClient.addLocalCapability("flutter_smart_watch_connected_nodes")
            } catch (e: Exception) {
                print(e.message)
            }
        }
    }

    private fun sendExampleMessage(node: Node) {
        messageClient.sendMessage(
            node.id,
            "/wearos-message-path",
            "Sample message from WearOS app at ${Date().time}".toByteArray()
        )
    }

    private fun sendExampleData() {
        val dataMap = DataMap()
        dataMap.putString("message", "Sample map data from WearOS app at ${Date().time}")
        val putDataRequest: PutDataRequest =
            PutDataMapRequest.create("/wearos-data-path").run {
                this.dataMap.putAll(dataMap)
                asPutDataRequest()
            }
        coroutineScope.launch {
            try {
                dataClient.putDataItem(putDataRequest).await()
            } catch (e: Exception) {
                print(e.localizedMessage)
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        binding.messageText.text = event.data.decodeToString()
    }

    override fun onDataChanged(event: DataEventBuffer) {
        val dataItem =event[0].dataItem
        if (dataItem.uri.path == "/data-image-path"){
            DataMapItem.fromDataItem(dataItem)
                .dataMap.getAsset("sample-image")?.let {
                    binding.sampleImageView.setImageBitmap(loadBitmapFromAsset(it))
                }
            return
        }
        binding.dataText.text = event[0].dataItem.data?.decodeToString()
    }

    fun loadBitmapFromAsset(asset: Asset): Bitmap? {
        // convert asset into a file descriptor and block until it's ready
        val assetInputStream: InputStream? =
            Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
                ?.inputStream

        return assetInputStream?.let { inputStream ->
            // decode the stream into a bitmap
            BitmapFactory.decodeStream(inputStream)
        } ?: run {
            null
        }
    }
}

