package com.robin.interview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    fun onLocalModelClick(view: View) {
        startActivity(
            Intent(
                this,
                SceneViewActivity::class.java
            ).putExtra(SceneViewActivity.EXTRA_MODEL_TYPE, "local")
        )

    }

}
