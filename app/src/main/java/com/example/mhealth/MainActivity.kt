package com.example.mhealth

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import androidx.annotation.RequiresApi
import com.androidnetworking.AndroidNetworking
import com.example.mhealth.BackgroundService.logData
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.app.ActivityManager
import android.content.Context


class MainActivity : AppCompatActivity() {
    private val homeFragment = HomeFragment.newInstance()
    private val heartFragment = HeartFragment.newInstance()
    private val stepsFragment = StepsFragment.newInstance()
    private val caloriesFragment = CaloriesFragment.newInstance()
    private val sleepFragment = SleepFragment.newInstance()
    private var backgroundService: BackgroundService? = null
    private lateinit var interpreter: Interpreter
    private lateinit var healthOutput: Array<String>

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                openFragment(homeFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_heart -> {
                openFragment(heartFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_steps -> {
                openFragment(stepsFragment)
                return@OnNavigationItemSelectedListener true
            }

            R.id.navigation_calories -> {
                openFragment(caloriesFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_sleep -> {
                openFragment(sleepFragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    companion object {
        var healthDataResults: RealmResults<HealthDataObject>? = null
        lateinit var healthRecommendations: Array<String>

        private fun fetchHistoricalData() {
            val realmConfiguration = RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .name("mHealth.realm")
                    .schemaVersion(0)
                    .build()
            Realm.setDefaultConfiguration(realmConfiguration)
            val realm = Realm.getDefaultInstance()
            var healthDataObjects: RealmResults<HealthDataObject>? = null

            try {
                healthDataObjects = realm.where(HealthDataObject::class.java).sort("date").findAll()
                healthDataResults = healthDataObjects
            } catch (err: RuntimeException) {
                //("Error getting historical data (" + err.message + ")")
            }
        }

        fun getHistoricalData() : RealmResults<HealthDataObject>? {
            return healthDataResults
        }

        fun getReccomendation (): Array<String> {
            return healthRecommendations
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        interpreter = Interpreter(loadModelFile())

        if (!checkServiceRunning("com.example.mhealth.BackgroundService")) {
            val intent = Intent(this, BackgroundService::class.java)
            startService(intent)
        }

        setContentView(R.layout.activity_main)

        val animatedDrawable: Animatable = splash_Icon.drawable as Animatable
        animatedDrawable.start()

        AnimatedVectorDrawableCompat.registerAnimationCallback(splash_Icon.drawable,
                object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        Thread.sleep(500)
                        hideSplash()
                    }
                })

        openFragment(homeFragment)

        Realm.init(applicationContext)
        val realmConfiguration = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("mHealth.realm")
                .schemaVersion(0)
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)

        fetchHistoricalData()
        val realmData = getHistoricalData()
        if (realmData != null) {
            healthRecommendations = getMLRecommendation(realmData.get(realmData.size - 1)!!)
        }

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        //logData ("Started App")
        AndroidNetworking.initialize(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        BackgroundService.updateAppState(false)
    }

    // checkServiceEunning code adapted from https://stackoverflow.com/questions/17588910/check-if-service-is-running-on-android?lq=1
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkServiceRunning(serviceClass: String): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun hideSplash() {
        splash_Container.visibility = View.GONE
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }

    fun getMLRecommendation (healthData: HealthDataObject): Array<String> {
        val inputValues = FloatArray(11)

        inputValues[0] = healthData.minimumHeartrate.toFloat()
        inputValues[1] = healthData.maximumHeartrate.toFloat()
        inputValues[2] = healthData.averageHeartrate.toFloat()
        inputValues[3] = healthData.stepsTaken.toFloat()
        inputValues[4] = healthData.caloriesBurned.toInt().toFloat()
        inputValues[5] = healthData.sleep.toFloat()
        inputValues[6] = (if (healthData.sleep >= 450) 1 else 0).toFloat()
        inputValues[7] = (if (healthData.stepsTaken >= 6000) 1 else 0).toFloat()
        inputValues[8] = inputValues[4] / healthData.stepsTaken
        inputValues[9] = inputValues[5] / inputValues[2]
        inputValues[10] = inputValues[3] / inputValues[2]

        val outputArray = getMLOutput (inputValues)
        var outputRating = ""
        var outputRecommendation = ""

        if (outputArray[1] > outputArray[0]) {
            outputRating = "Unhealthy"
            if (healthData.stepsTaken < 6000 || healthData.caloriesBurned < 255) {
                outputRecommendation = "Exercise more"
                if (healthData.sleep < 450) {
                    outputRecommendation = "Exercise and sleep more"
                }

                if (healthData.averageHeartrate > 65) {
                    outputRecommendation = "Exercise more and reduce stress"
                }
            } else if (healthData.sleep < 450) {
                outputRecommendation = "Sleep more"

                if (healthData.averageHeartrate > 65) {
                    outputRecommendation = "Sleep more and reduce stress"
                }
            } else if (healthData.stepsTaken >= 6000 && healthData.caloriesBurned >= 255 && healthData.sleep >= 450) {
                if (healthData.averageHeartrate > 65) {
                    outputRating = "Unhealthy"
                    outputRecommendation = "Reduce stress"
                } else {
                    outputRating = "Somewhat Unhealthy"
                    outputRecommendation = "Exercise and sleep more"
                }
            }
        } else if (outputArray[1] < outputArray[0]) {
            outputRating = "Healthy"
            outputRecommendation = "Keep it up"
        }

        val outputValues = arrayOf<String>(outputRating, outputRecommendation)

        return outputValues
    }

    fun getMLOutput(inputArray: FloatArray): FloatArray {
        val outputArray = Array(1) { FloatArray(2) }

        interpreter.run(inputArray, outputArray)

        return outputArray[0]
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("mHealth_Model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.getChannel()
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
