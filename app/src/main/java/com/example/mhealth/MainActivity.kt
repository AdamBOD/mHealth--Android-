package com.example.mhealth

import android.app.Service
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.content.Intent
import android.widget.Toast
import com.androidnetworking.AndroidNetworking


class MainActivity : AppCompatActivity() {
    private val homeFragment = HomeFragment.newInstance()
    private val heartFragment = HeartFragment.newInstance()

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
                return@OnNavigationItemSelectedListener true
            }

            R.id.navigation_calories -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_sleep -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BackgroundService.serviceRunning) {
            val intent = Intent(this, BackgroundService::class.java)
            startService(intent)
        }

        setContentView(R.layout.activity_main)

        openFragment(homeFragment)

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        AndroidNetworking.initialize(getApplicationContext());
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}
