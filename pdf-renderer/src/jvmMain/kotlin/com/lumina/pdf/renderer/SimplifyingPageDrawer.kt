package com.lumina.pdf.renderer

import org.apache.pdfbox.rendering.PageDrawer
import org.apache.pdfbox.rendering.PageDrawerParameters
import java.awt.geom.GeneralPath

class SimplifyingPageDrawer(
    parameters: PageDrawerParameters,
    private val epsilon: Double
) : PageDrawer(parameters) {

    override fun strokePath() {
        val path = linePath
        if (path != null) {
            val simplified = RdpPathSimplifier.simplifyPath(path, epsilon)
            linePath.reset()
            linePath.append(simplified, false)
        }
        super.strokePath()
    }

    override fun fillPath(windingRule: Int) {
        val path = linePath
        if (path != null) {
            val simplified = RdpPathSimplifier.simplifyPath(path, epsilon)
            linePath.reset()
            linePath.append(simplified, false)
        }
        super.fillPath(windingRule)
    }
}
