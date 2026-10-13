package pe.edu.upeu.pharmamobil.platform

import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

class CompartidorIos : Compartidor {
    override fun compartir(texto: String) {
        var presenter: UIViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        while (presenter.presentedViewController != null) {
            presenter = presenter.presentedViewController ?: break
        }

        val selector = UIActivityViewController(
            activityItems = listOf(texto),
            applicationActivities = null
        )
        selector.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds
        }
        presenter.presentViewController(selector, animated = true, completion = null)
    }
}
