package co.edu.uniandes.twnel.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import co.edu.uniandes.twnel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}