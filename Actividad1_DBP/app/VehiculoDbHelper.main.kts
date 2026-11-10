package com.example.vehiculos

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class VehiculoDbHelper(context: Context) :
    SQLiteOpenHelper(context, "vehiculos.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE vehiculos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                marca TEXT NOT NULL,
                modelo TEXT NOT NULL,
                anio INTEGER NOT NULL,
                precio_centavos INTEGER NOT NULL
                    CHECK (precio_centavos > 0)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Al cambiar la versión, agrega aquí una migración
        // que conserve los registros existentes.
        error("Falta implementar la migración $oldVersion → $newVersion")
    }

    fun guardar(
        marca: String,
        modelo: String,
        anio: Int,
        precioCentavos: Long
    ): Long {
        val valores = ContentValues().apply {
            put("marca", marca)
            put("modelo", modelo)
            put("anio", anio)
            put("precio_centavos", precioCentavos)
        }

        return writableDatabase.insertOrThrow(
            "vehiculos",
            null,
            valores
        )
    }

    fun contarVehiculos(): Long {
        return DatabaseUtils.queryNumEntries(
            readableDatabase,
            "vehiculos"
        )
    }
}