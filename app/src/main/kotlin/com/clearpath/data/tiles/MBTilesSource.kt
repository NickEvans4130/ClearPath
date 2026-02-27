package com.clearpath.data.tiles

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File

/**
 * osmdroid ITileSource backed by a local .mbtiles SQLite file.
 *
 * MBTiles uses TMS tile coordinates where the Y axis is flipped relative to
 * the standard XYZ (slippy map) scheme used by osmdroid.
 *
 * Conversion:  tmsY = (1 << zoom) - 1 - osmY
 */
class MBTilesSource(
    private val mbtilesFile: File,
) : BitmapTileSourceBase(
    /* aName          = */ mbtilesFile.nameWithoutExtension,
    /* aZoomMinLevel  = */ 1,
    /* aZoomMaxLevel  = */ 18,
    /* aTileSizePixels= */ 256,
    /* aImageFilenameEnding = */ ".png",
) {

    private var db: SQLiteDatabase? = null

    fun open() {
        db = SQLiteDatabase.openDatabase(
            mbtilesFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        )
    }

    fun close() {
        db?.close()
        db = null
    }

    override fun getTile(pMapTile: Long): Bitmap? {
        val db = db ?: return null
        val zoom = MapTileIndex.getZoom(pMapTile)
        val col  = MapTileIndex.getX(pMapTile).toLong()
        val row  = MapTileIndex.getY(pMapTile).toLong()

        // Flip Y: MBTiles TMS → XYZ
        val tmsRow = (1L shl zoom) - 1L - row

        return try {
            db.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
                arrayOf(zoom.toString(), col.toString(), tmsRow.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val blob = cursor.getBlob(0)
                    BitmapFactory.decodeByteArray(blob, 0, blob.size)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    val isOpen: Boolean get() = db?.isOpen == true

    companion object {
        fun fromContext(context: Context, filename: String): MBTilesSource? {
            val file = File(context.getExternalFilesDir("tiles"), filename)
            if (!file.exists()) return null
            return MBTilesSource(file).also { it.open() }
        }
    }
}
