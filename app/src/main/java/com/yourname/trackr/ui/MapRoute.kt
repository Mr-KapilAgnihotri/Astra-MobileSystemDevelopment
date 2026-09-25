package com.yourname.trackr.ui

import android.graphics.Paint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

private const val REPLAY_ZOOM = 18.0
private const val DEFAULT_ZOOM = 4.0
private val DEFAULT_CENTER = GeoPoint(20.0, 0.0)

/** One-time setup shared by both the live tracking map and the static replay map. */
fun MapView.setUpBasicMap() {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    controller.setZoom(DEFAULT_ZOOM)
    controller.setCenter(DEFAULT_CENTER)
}

private fun routePolyline(points: List<GeoPoint>, routeColor: Int): Polyline =
    Polyline().apply {
        setPoints(points)
        outlinePaint.apply {
            color = routeColor
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

/** Draws a finished route once - start/end markers, rounded polyline - and zooms/pans to fit it. */
fun MapView.showStaticRoute(points: List<GeoPoint>, routeColor: Int, startColor: Int) {
    overlays.clear()
    if (points.isEmpty()) {
        invalidate()
        return
    }

    if (points.size > 1) {
        overlays.add(routePolyline(points, routeColor))
        overlays.add(MapDotOverlay(startColor).apply { point = points.first() })
    }
    overlays.add(MapDotOverlay(routeColor).apply { point = points.last() })

    // zoomToBoundingBox needs the view to already have a real size, so defer to after layout.
    post {
        if (points.size > 1) {
            zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 64)
        } else {
            controller.setZoom(REPLAY_ZOOM)
            controller.setCenter(points.first())
        }
    }
    invalidate()
}
