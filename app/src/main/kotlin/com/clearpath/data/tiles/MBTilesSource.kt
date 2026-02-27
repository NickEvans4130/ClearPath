package com.clearpath.data.tiles

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.osmdroid.tileprovider.modules.IArchiveFile
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * MBTiles archive for osmdroid's [IArchiveFile] provider system.
 *
 * MBTiles uses TMS y-coordinates (y=0 at south), osmdroid uses XYZ (y=0 at north):
 *   tmsRow = (1 << zoom) - 1 - osmY
 */
class MBTilesArchive : IArchiveFile {

    private var db: SQLiteDatabase? = null
    private var ignoreTileSource = false

    // Called by MapTileFileArchiveProvider with the on-disk archive file
    override fun init(pFile: File) {
        db?.close()
        db = SQLiteDatabase.openDatabase(
            pFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        )
    }

    override fun getInputStream(tileSource: ITileSource, pMapTileIndex: Long): InputStream? {
        val db   = db ?: return null
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val col  = MapTileIndex.getX(pMapTileIndex).toLong()
        val row  = MapTileIndex.getY(pMapTileIndex).toLong()
        val tmsRow = (1L shl zoom) - 1L - row

        return try {
            db.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
                arrayOf(zoom.toString(), col.toString(), tmsRow.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) ByteArrayInputStream(cursor.getBlob(0)) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun close() {
        db?.close()
        db = null
    }

    override fun getTileSources(): Set<String> = emptySet()

    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {
        ignoreTileSource = pIgnoreTileSource
    }

    companion object {
        /**
         * Swap a MapView's tile provider to read from an MBTiles file.
         */
        fun applyToMapView(context: Context, mapView: MapView, file: File) {
            val archive  = MBTilesArchive().also { it.init(file) }
            val source   = TileSourceFactory.MAPNIK
            val receiver = SimpleRegisterReceiver(context)
            val provider = MapTileFileArchiveProvider(receiver, source, arrayOf(archive))
            mapView.tileProvider = org.osmdroid.tileprovider.MapTileProviderArray(
                source, receiver, arrayOf(provider)
            )
            mapView.invalidate()
        }
    }
}
