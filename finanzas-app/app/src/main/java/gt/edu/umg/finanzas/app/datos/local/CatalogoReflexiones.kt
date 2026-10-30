package gt.edu.umg.finanzas.app.datos.local

import java.time.LocalDate

data class Reflexion(
    val texto: String,
    val referencia: String,
    val traduccion: String = "Reina-Valera 1909",
    val fuente: String = "Catálogo local · eBible",
)

object CatalogoReflexiones {
    // RV1909 de dominio público. Texto y procedencia: docs/referencias/SERVICIOS_INCREMENTO_7.md.
    private val citas = listOf(
        Reflexion("Salmo de David. JEHOVÁ es mi pastor; nada me faltará.", "Salmos 23:1"),
        Reflexion("Encomienda á Jehová tus obras, y tus pensamientos serán afirmados.", "Proverbios 16:3"),
        Reflexion("Todo lo puedo en Cristo que me fortalece.", "Filipenses 4:13"),
    )

    fun paraFecha(fecha: LocalDate): Reflexion = citas[Math.floorMod(fecha.toEpochDay(), citas.size.toLong()).toInt()]
}
