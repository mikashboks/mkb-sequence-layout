package com.transferwise.sequencelayout.sample

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.mikashboks.sequencelayout.sample.R
import com.transferwise.sequencelayout.SequenceLayout

class MainActivity : AppCompatActivity(R.layout.activity_main){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<SequenceLayout>(R.id.sequenceLayout).apply {
            setProgressDotStepIndex(1)
            setProgressDotStepMax(1000)
            setProgressDotStepCurrent(300)

            postDelayed({
                setProgressDotStepCurrent(700)
            }, 5000)
        }
    }
}
