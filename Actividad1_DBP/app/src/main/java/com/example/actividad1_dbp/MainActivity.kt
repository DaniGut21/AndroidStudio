package com.example.vehiculos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigDecimal
import java.util.Calendar
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var db: VehiculoDbHelper

    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var etAnio: EditText
    private lateinit var etPrecio: EditText
    private lateinit var btnGuardar: Button
    private lateinit var tvEstado: TextView

    // Ejecuta las operaciones de SQLite fuera del hilo de la interfaz.
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Evita que el formulario quede debajo de las barras del sistema.
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.pantalla)
        ) { view, insets ->
            val espacio = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.ime()
            )

            view.setPadding(
                espacio.left,
                espacio.top,
                espacio.right,
                espacio.bottom
            )
            insets
        }

        db = VehiculoDbHelper(applicationContext)

        etMarca = findViewById(R.id.etMarca)
        etModelo = findViewById(R.id.etModelo)
        etAnio = findViewById(R.id.etAnio)
        etPrecio = findViewById(R.id.etPrecio)
        btnGuardar = findViewById(R.id.btnGuardar)
        tvEstado = findViewById(R.id.tvEstado)

        btnGuardar.setOnClickListener {
            guardarVehiculo()
        }

        cargarTotal()
    }

    private fun cargarTotal() {
        executor.execute {
            try {
                val total = db.contarVehiculos()

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        tvEstado.text = "Vehículos guardados: $total"
                    }
                }
            } catch (e: Exception) {
                Log.e("Vehiculos", "Error al consultar", e)

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        tvEstado.text = "No se pudo consultar la base de datos."
                    }
                }
            }
        }
    }

    private fun guardarVehiculo() {
        etMarca.error = null
        etModelo.error = null
        etAnio.error = null
        etPrecio.error = null

        val marca = etMarca.text.toString().trim()
        val modelo = etModelo.text.toString().trim()
        val anio = etAnio.text.toString().toIntOrNull()

        val precioTexto = etPrecio.text.toString()
            .trim()
            .replace(',', '.')

        val anioMaximo = Calendar.getInstance()
            .get(Calendar.YEAR) + 1

        if (marca.isBlank()) {
            etMarca.error = "Ingresa la marca"
            etMarca.requestFocus()
            return
        }

        if (modelo.isBlank()) {
            etModelo.error = "Ingresa el modelo"
            etModelo.requestFocus()
            return
        }

        if (anio == null || anio !in 1886..anioMaximo) {
            etAnio.error = "Ingresa un año entre 1886 y $anioMaximo"
            etAnio.requestFocus()
            return
        }

        if (!Regex("""\d+([.]\d{1,2})?""").matches(precioTexto)) {
            etPrecio.error = "Ingresa un precio con máximo 2 decimales"
            etPrecio.requestFocus()
            return
        }

        val precioCentavos = try {
            BigDecimal(precioTexto)
                .movePointRight(2)
                .longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }

        if (precioCentavos == null || precioCentavos <= 0) {
            etPrecio.error = "Ingresa un precio válido mayor que cero"
            etPrecio.requestFocus()
            return
        }

        habilitarFormulario(false)
        tvEstado.text = "Guardando…"

        executor.execute {
            try {
                val id = db.guardar(
                    marca,
                    modelo,
                    anio,
                    precioCentavos
                )

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        etMarca.text.clear()
                        etModelo.text.clear()
                        etAnio.text.clear()
                        etPrecio.text.clear()

                        habilitarFormulario(true)
                        tvEstado.text = "Vehículo guardado. ID: $id"
                        etMarca.requestFocus()
                    }
                }
            } catch (e: Exception) {
                Log.e("Vehiculos", "Error al guardar", e)

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        habilitarFormulario(true)
                        tvEstado.text =
                            "No se pudo guardar. Los datos siguen en el formulario."
                    }
                }
            }
        }
    }

    private fun habilitarFormulario(habilitado: Boolean) {
        etMarca.isEnabled = habilitado
        etModelo.isEnabled = habilitado
        etAnio.isEnabled = habilitado
        etPrecio.isEnabled = habilitado
        btnGuardar.isEnabled = habilitado
    }

    override fun onDestroy() {
        // Cierra la conexión después de las operaciones pendientes.
        executor.execute {
            db.close()
        }
        executor.shutdown()
        super.onDestroy()
    }
}