package com.lumina.pdf.viewer

// Android already provides system back navigation (gesture/button), so an in-app
// back arrow in the top bar is redundant there. Other platforms have no equivalent,
// so they still need it.
expect fun shouldShowInAppBackButton(): Boolean
