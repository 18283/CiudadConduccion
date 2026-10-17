package com.graphics; // Sigue la serie junto a las ocho clases anteriores.

/**
 * CLASE 9: RETROVISORES DEL AUTO.
 * Hereda clase8, que hereda clase7, clase6, clase5, clase4, clase3, clase2 y
 * clase1; por eso reutiliza la ciudad, la conduccion, la iluminacion, las
 * entregas, el minimapa, el trafico, el panel, el menu y los peatones sin
 * copiarlos.
 * Anade dos retrovisores laterales, uno en cada costado, como en un auto de
 * verdad: un brazo corto que sale de la cabina, una carcasa del color de la
 * carroceria y un vidrio claro en la cara que mira al conductor.
 * Se agregan dentro de dibujarCarroceria(), que es el unico lugar donde se
 * construye un auto; asi el rojo del jugador y los blancos del trafico
 * reciben los mismos espejos sin repetir medidas.
 * Orden de lectura: modelo del auto, retrovisores y punto de entrada.
 */
public class clase9 extends clase8 {

    // ==================== 1. AUTO CON ESPEJOS ====================

    /**
     * Construye el auto heredado y le suma los dos retrovisores.
     * Conserva los mismos parametros de clase2: origen, orientacion y colores
     * de carroceria y cabina.
     */
    @Override // Amplia el modelo unico que usan el jugador y el trafico.
    protected void dibujarCarroceria(float origenX, float origenZ, float angulo, float cuerpoR, float cuerpoG,
            float cuerpoB, float cabinaR, float cabinaG, float cabinaB) {
        super.dibujarCarroceria(origenX, origenZ, angulo, cuerpoR, cuerpoG, cuerpoB, cabinaR, cabinaG, cabinaB); // Auto
                                                                                                                 // completo
                                                                                                                 // heredado.
        dibujarRetrovisores(origenX, origenZ, angulo, cuerpoR, cuerpoG, cuerpoB); // Un espejo en cada costado.
    }

    // ==================== 2. LOS DOS RETROVISORES ====================

    /*
     * Espacio local del auto (el mismo de clase2): el frente mira hacia -Z,
     * X es el costado (negativo a la izquierda) e Y es la altura.
     * La cabina mide 1.3 de ancho (llega hasta x = +-0.65) y su frente esta
     * cerca de z = -0.5; los espejos van ahi, a la altura de la cabina.
     */

    /** Dibuja el espejo izquierdo y el derecho con las piezas de clase2. */
    private void dibujarRetrovisores(float origenX, float origenZ, float angulo, float cuerpoR, float cuerpoG,
            float cuerpoB) {
        float[] lados = { -1f, 1f }; // Izquierda y derecha del auto.
        for (float lado : lados) { // Repite el mismo espejo en ambos costados.
            float brazoX = lado * 0.80f; // El brazo une la cabina (x = 0.65) con la carcasa (x = 0.98).
            piezaEn(origenX, origenZ, angulo, brazoX, 1.15f, -0.45f, 0.30f, 0.07f, 0.07f, 0.08f, 0.09f, 0.10f); // Brazo
                                                                                                                // oscuro
                                                                                                                // y
                                                                                                                // delgado.
            float carcasaX = lado * 0.98f; // La carcasa cuelga del extremo del brazo.
            piezaEn(origenX, origenZ, angulo, carcasaX, 1.20f, -0.45f, 0.28f, 0.16f, 0.16f, cuerpoR, cuerpoG, cuerpoB); // Carcasa
                                                                                                                        // apaisada
                                                                                                                        // del
                                                                                                                        // color
                                                                                                                        // del
                                                                                                                        // auto:
                                                                                                                        // mas
                                                                                                                        // ancha
                                                                                                                        // que
                                                                                                                        // alta.
        }
        entero("uEmision", 1); // El vidrio brilla como un espejo, visible incluso de noche.
        for (float lado : lados) { // Repite el vidrio en ambos costados.
            float carcasaX = lado * 0.98f; // Misma posicion lateral que su carcasa.
            piezaEn(origenX, origenZ, angulo, carcasaX, 1.20f, -0.36f, 0.22f, 0.12f, 0.03f, 0.80f, 0.90f, 1.0f); // Vidrio
                                                                                                                 // apaisado
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

    /** Punto de entrada de la novena leccion. */
    public static void main(String[] args) {
        clase9 aplicacion = new clase9(); // Crea la version con retrovisores.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicacion.
    }
}
