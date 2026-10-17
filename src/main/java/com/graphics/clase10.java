package com.graphics; // Sigue la serie junto a las nueve clases anteriores.

/**
 * CLASE 10: AUTOS MAS GRANDES.
 * Hereda clase9, que hereda clase8, clase7, clase6, clase5, clase4, clase3,
 * clase2 y clase1; por eso reutiliza la ciudad, la conduccion, la
 * iluminacion, las entregas, el minimapa, el trafico, el panel, el menu, los
 * peatones y los retrovisores sin copiarlos.
 * Agranda un poco los 4 autos (el rojo del jugador y los 3 blancos del
 * trafico): multiplica por ESCALA_AUTOS cada medida local del auto, tanto la
 * posicion de cada pieza como su tamano, para que crezca parejo sin
 * deformarse.
 * Se hace dentro de dibujarCarroceria(), que es el unico lugar donde se
 * construye un auto; asi los 4 reciben el mismo aumento sin repetir medidas.
 * Orden de lectura: escala, auto grande, ayudante y punto de entrada.
 */
public class clase10 extends clase9 {

    // ==================== 1. CUANTO CRECE EL AUTO ====================

    /**
     * Factor de agrandado: 1.3 es un 30% mas grande, se nota sin tapar la calle.
     */
    protected static final float ESCALA_AUTOS = 1.3f;

    // ==================== 2. AUTO GRANDE ====================

    /**
     * Construye el mismo auto de clase2 + retrovisores de clase9, pero mas
     * grande. Conserva los mismos parametros: origen, orientacion y colores
     * de carroceria y cabina.
     */
    @Override // Amplia el modelo unico que usan el jugador y el trafico.
    protected void dibujarCarroceria(float origenX, float origenZ, float angulo, float cuerpoR, float cuerpoG,
            float cuerpoB, float cabinaR, float cabinaG, float cabinaB) {
        // Carroceria y cabina (mismas medidas de clase2, agrandadas por el ayudante).
        piezaGrandeEn(origenX, origenZ, angulo, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, cuerpoR, cuerpoG, cuerpoB); // Carroceria.
        piezaGrandeEn(origenX, origenZ, angulo, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, cabinaR, cabinaG, cabinaB); // Cabina.

        // Cuatro ruedas: izquierda/derecha y eje delantero/trasero.
        float[] ladosRuedas = { -0.88f, 0.88f }; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = { -0.82f, 0.82f }; // Ubica las ruedas delanteras y traseras.
        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehiculo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                piezaGrandeEn(origenX, origenZ, angulo, x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Rueda.
            }
        }

        // Faros delanteros y luces traseras.
        float[] ladosFaros = { -0.55f, 0.55f }; // Separacion lateral de las luces.
        for (float x : ladosFaros) { // Repite el dibujo para ambos lados.
            piezaGrandeEn(origenX, origenZ, angulo, x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Faro
                                                                                                            // delantero.
            piezaGrandeEn(origenX, origenZ, angulo, x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Luz
                                                                                                                // trasera
                                                                                                                // roja.
        }

        // Retrovisores de clase9, con las mismas medidas pero agrandados.
        float[] lados = { -1f, 1f }; // Izquierda y derecha del auto.
        for (float lado : lados) { // Repite el mismo espejo en ambos costados.
            float brazoX = lado * 0.80f; // El brazo une la cabina con la carcasa.
            piezaGrandeEn(origenX, origenZ, angulo, brazoX, 1.15f, -0.45f, 0.30f, 0.07f, 0.07f, 0.08f, 0.09f, 0.10f); // Brazo
                                                                                                                      // oscuro
                                                                                                                      // y
                                                                                                                      // delgado.
            float carcasaX = lado * 0.98f; // La carcasa cuelga del extremo del brazo.
            piezaGrandeEn(origenX, origenZ, angulo, carcasaX, 1.20f, -0.45f, 0.28f, 0.16f, 0.16f, cuerpoR, cuerpoG,
                    cuerpoB); // Carcasa del color del auto.
        }
        entero("uEmision", 1); // El vidrio brilla como un espejo, visible incluso de noche.
        for (float lado : lados) { // Repite el vidrio en ambos costados.
            float carcasaX = lado * 0.98f; // Misma posicion lateral que su carcasa.
            piezaGrandeEn(origenX, origenZ, angulo, carcasaX, 1.20f, -0.36f, 0.22f, 0.12f, 0.03f, 0.80f, 0.90f, 1.0f); // Vidrio
                                                                                                                       // en
                                                                                                                       // la
                                                                                                                       // cara
                                                                                                                       // que
                                                                                                                       // mira
                                                                                                                       // al
                                                                                                                       // conductor
                                                                                                                       // (+Z).
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminacion normal.
    }

    // ==================== 3. AYUDANTE DE ESCALA ====================

    /**
     * Coloca una pieza del auto agrandada: multiplica por ESCALA_AUTOS su
     * posicion local (x, y, z) y su tamano (sx, sy, sz) antes de pedir a
     * piezaEn() que la lleve al mundo. Asi todo crece parejo desde el suelo.
     */
    private void piezaGrandeEn(float origenX, float origenZ, float angulo, float x, float y, float z, float sx,
            float sy,
            float sz, float r, float g, float b) {
        piezaEn(origenX, origenZ, angulo, x * ESCALA_AUTOS, y * ESCALA_AUTOS, z * ESCALA_AUTOS,
                sx * ESCALA_AUTOS, sy * ESCALA_AUTOS, sz * ESCALA_AUTOS, r, g, b);
    }

    /** Punto de entrada de la decima leccion. */
    public static void main(String[] args) {
        clase10 aplicacion = new clase10(); // Crea la version con autos mas grandes.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicacion.
    }
}
