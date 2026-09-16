package com.yourname.trackr.ui

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val LIVE_ZOOM = 18.0
private const val DEFAULT_ZOOM = 4.0
private val DEFAULT_CENTER = GeoPoint(20.0, 0.0)

/** One-time setup shared by both the live tracking map and the static replay map. */
fun MapView.setUpBasicMap() {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    controller.setZoom(DEFAULT_ZOOM)
    controller.setCenter(DEFAULT_CENTER)
}

/**
 * Redraws the route as new GPS points arrive during live tracking: a polyline for the path
 * so far, a marker on the latest point, and the camera follows that latest point.
 */
fun MapView.updateLiveRoute(points: List<GeoPoint>, routeColor: Int) {
    overlays.clear()
    if (points.isEmpty()) {
        invalidate()
        return
    }

    if (points.size > 1) {
        overlays.add(
            Polyline(this).apply {
                setPoints(points)
                outlinePaint.color = routeColor
                outlinePaint.strokeWidth = 8f
            }
        )
    }

    val last = points.last()
    overlays.add(
        Marker(this).apply {
            position = last
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
    )

    if (zoomLevelDouble < LIVE_ZOOM) controller.setZoom(LIVE_ZOOM)
    controller.animateTo(last)
    invalidate()
}

/** Draws a finished route once and zooms/pans to fit its full bounding box. */
fun MapView.showStaticRoute(points: List<GeoPoint>, routeColor: Int) {
    overlays.clear()
    if (points.isEmpty()) {
        invalidate()
        return
    }

    if (points.size > 1) {
        overlays.add(
            Polyline(this).apply {
                setPoints(points)
                outlinePaint.color = routeColor
                outlinePaint.strokeWidth = 8f
            }
        )
    }
    overlays.add(Marker(this).apply { position = points.first() })
    overlays.add(Marker(this).apply { position = points.last() })

    // zoomToBoundingBox needs the view to already have a real size, so defer to after layout.
    post {
        if (points.size > 1) {
            zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 64)
        } else {
            controller.setZoom(LIVE_ZOOM)
            controller.setCenter(points.first())
        }
    }
    invalidate()
}
