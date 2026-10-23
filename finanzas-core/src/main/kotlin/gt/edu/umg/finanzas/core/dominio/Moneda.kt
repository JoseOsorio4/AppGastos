package gt.edu.umg.finanzas.core.dominio

/**
 * Únicas monedas admitidas. GTQ es la base del saldo y de los reportes;
 * USD solo entra convertido a GTQ con una [TasaCambio].
 */
enum class Moneda(val simbolo: String) {
    GTQ("Q"),
    USD("US$"),
}
