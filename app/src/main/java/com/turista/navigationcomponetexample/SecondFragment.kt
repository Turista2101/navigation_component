package com.turista.navigationcomponetexample

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.turista.navigationcomponetexample.databinding.FragmentSecondBinding
import java.io.File
import java.io.IOException

class SecondFragment : Fragment() {

    private lateinit var binding: FragmentSecondBinding
    private var grabadora: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ruta: String = ""
    private var isRecording = false
    private var handler = Handler()
    private var startTime = 0L
    private val recordingList = mutableListOf<String>()
    private var recordingCount = 0
    private val args: SecondFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = args.name
        binding.tvName.text = name

        checkPermissions()

        // Configurar botones de la interfaz
        binding.btnGrabar.setOnClickListener { toggleRecording() }
        binding.btnCancelar.setOnClickListener { cancelRecording() }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 0)
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        recordingCount++
        ruta = "${requireContext().externalCacheDir?.absolutePath}/grabacion_$recordingCount.mp3"
        grabadora = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(ruta)
            try {
                prepare()
                start()
                isRecording = true
                startTime = System.currentTimeMillis()
                handler.post(updateTimer)

                binding.btnCancelar.visibility = View.VISIBLE
                binding.statusText.text = "Grabando..."
                Toast.makeText(requireContext(), "Grabación iniciada", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al iniciar la grabación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        grabadora?.apply {
            stop()
            release()
        }
        grabadora = null
        isRecording = false
        handler.removeCallbacks(updateTimer)

        binding.btnCancelar.visibility = View.GONE
        recordingList.add(ruta)
        binding.statusText.text = "Grabación guardada"
        Toast.makeText(requireContext(), "Grabación detenida y guardada", Toast.LENGTH_SHORT).show()
        updateRecordingListView()
    }

    private fun cancelRecording() {
        if (isRecording) {
            grabadora?.apply {
                stop()
                release()
            }
            grabadora = null
            isRecording = false
            handler.removeCallbacks(updateTimer)
            File(ruta).delete()

            binding.btnCancelar.visibility = View.GONE
            binding.statusText.text = "Grabación cancelada"
            binding.timerText.text = "00:00"
            Toast.makeText(requireContext(), "Grabación cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private val updateTimer = object : Runnable {
        override fun run() {
            val elapsedTime = System.currentTimeMillis() - startTime
            val seconds = (elapsedTime / 1000).toInt() % 60
            val minutes = (elapsedTime / (1000 * 60) % 60).toInt()
            binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateRecordingListView() {
        // Actualizar la lista de grabaciones en el ListView
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            recordingList.map { "Grabación ${recordingList.indexOf(it) + 1}" }
        )
        binding.listViewGrabaciones.adapter = adapter

        binding.listViewGrabaciones.setOnItemClickListener { _, _, position, _ ->
            playRecording(recordingList[position])
        }
    }

    private fun playRecording(ruta: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(ruta)
                prepare()
                start()
                Toast.makeText(requireContext(), "Reproduciendo grabación", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al reproducir la grabación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        grabadora?.release()
    }
}
