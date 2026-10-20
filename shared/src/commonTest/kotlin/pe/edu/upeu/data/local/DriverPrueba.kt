package pe.edu.upeu.data.local

import app.cash.sqldelight.db.SqlDriver

/** Base SQLDelight en memoria con el esquema PharmaDb ya creado; cada llamada es independiente. */
expect fun crearDriverPrueba(): SqlDriver
