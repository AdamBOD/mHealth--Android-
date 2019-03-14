package com.example.mhealth

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.androidnetworking.AndroidNetworking
import com.example.mhealth.BackgroundService.logData
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BackgroundService.serviceRunning) {
            logData(BackgroundService.serviceRunning.toString())
            val intent = Intent(this, BackgroundService::class.java)
            startService(intent)
        }

        setContentView(R.layout.activity_main)

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

    override fun onDestroy() {
        super.onDestroy()
        BackgroundService.updateAppState(false)
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }
}
