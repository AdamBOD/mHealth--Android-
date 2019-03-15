package com.example.mhealth

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.androidnetworking.AndroidNetworking
import com.example.mhealth.BackgroundService.logData
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*
import android.R.attr.delay




class MainActivity : AppCompatActivity() {
    private val homeFragment = HomeFragment.newInstance()
    private val heartFragment = HeartFragment.newInstance()
    private val stepsFragment = StepsFragment.newInstance()
    private val caloriesFragment = CaloriesFragment.newInstance()
    private val sleepFragment = SleepFragment.newInstance()
    private var backgroundService: BackgroundService? = null

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
                logData("Error getting historical data (" + err.message + ")")
            }
        }

        fun getHistoricalData() : RealmResults<HealthDataObject>? {
            return healthDataResults
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BackgroundService.serviceRunning) { //TODO CHANGE THIS CHECK - ALWAYS FALSE ATM
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
        /*val realmConfiguration = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("mHealth.realm")
                .schemaVersion(0)
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)
        val realm = Realm.getDefaultInstance()

        val r = realm.where(HealthDataObject::class.java)
                .findAll()

        logData("Health Data: " + r.toString())*/

        //healthDataObjects = fetchHistoricalData()
        fetchHistoricalData()

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        logData ("Started App")
        AndroidNetworking.initialize(getApplicationContext());
    }

    override fun onCreateView(name: String?, context: Context?, attrs: AttributeSet?): View? {
        return super.onCreateView(name, context, attrs)
    }

    override fun onDestroy() {
        super.onDestroy()
        BackgroundService.updateAppState(false)
    }

    private fun hideSplash() {
        splash_Container.visibility = View.GONE
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }
}
