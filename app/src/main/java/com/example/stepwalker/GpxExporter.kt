package com.example.stepwalker

import android.content.Context
import android.net.Uri

object GpxExporter {
    fun write(context: Context, uri: Uri, route: List<Pair<Double, Double>>) {
        val xml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""")
            append("""<gpx version="1.1" creator="StepWalker" xmlns="http://www.topografix.com/GPX/1/1">""")
            append("<trk><name>StepWalker route</name><trkseg>")
            route.forEach { (lat, lon) ->
                append("""<trkpt lat="$lat" lon="$lon"></trkpt>""")
            }
            append("</trkseg></trk></gpx>")
        }

        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(xml.toByteArray(Charsets.UTF_8))
        }
    }
}
