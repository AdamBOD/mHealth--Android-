package com.example.mhealth

import android.app.Service
import android.content.ComponentName
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
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
        val realmConfiguration = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("mHealth.realm")
                .schemaVersion(0)
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)
        val realm = Realm.getDefaultInstance()

        val r = realm.where(SleepObject::class.java)
                .findAll()

        logData("Sleep Data: " + r.toString())

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
