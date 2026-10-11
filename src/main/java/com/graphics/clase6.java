package com.graphics; // Continúa la serie junto a las cinco clases anteriores.

import java.awt.Color; // Elige el color de cada letra del panel.

import static org.lwjgl.opengl.GL33.*; // Permite recortar, limpiar y redirigir el recuadro del panel.

/**
 * CLASE 6: PANEL DE VELOCIDAD Y LUCES.
 * Hereda clase5, que hereda clase4, clase3, clase2 y clase1; por eso reutiliza
 * la ciudad, la conducción, la iluminación, las entregas, el minimapa y el
 * tráfico sin copiarlos.
 * Añade un panel pequeño en la esquina inferior derecha de la ventana, dentro
 * del mismo pase OpenGL, con tres filas: velocidad del auto rojo en km/h con
 * barra proporcional, estado de los faros (ON u OFF) y hora del día (DIA o
 * NOCHE). Todo queda dentro de un único recuadro para no tapar la ciudad.
 * El fondo del recuadro, la barra y las lámparas siguen dibujándose con las
 * cajas del proyecto sobre un viewport cuadrado recortado con scissor, igual
 * que el minimapa de clase4 pero abajo a la derecha. Las letras ya no son una
 * fuente de puntos: se escriben con {@link Texto2D}, que usa una fuente real
 * del sistema con suavizado y la pega como textura, así que ya no se ven
 * pixeladas.
 * Orden de lectura: recuadro, fondo y filas, texto y punto de entrada.
 */
public class clase6 extends clase5 {

    // ==================== 1. TAMAÑO DEL RECUADRO ====================

    private static final int LADO_MAXIMO = 230; // Lado del panel en píxeles: agrandado para la N de 5 puntos.
    private static final int MARGEN = 12; // Separación respecto a los bordes derecho e inferior.
    private static final float TAMANO_TITULO = 20f; // Altura de la fuente del texto, en puntos de pantalla.
    private static final float TAMANO_PANEL = 24f; // Título un poco más grande que las filas, sin exagerar.

    private Texto2D texto; // Lienzo de letras con fuente real; se crea en el primer cuadro con contexto OpenGL.
    private int ladoPanel; // Lado en píxeles del recuadro del último cuadro, para convertir coordenadas.

    // ==================== 2. RECUADRO INFERIOR DERECHO ====================

    /** Dibuja la ciudad con minimapa y después el panel encima. */
    @Override // Amplía el doble pase definido en clase4.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja la vista principal y el minimapa superior derecho.
        dibujarPanel(); // Añade el recuadro inferior derecho con velocidad y luces.
    }

    /** Recorta un cuadrado abajo a la derecha y dibuja el contenido del panel. */
    private void dibujarPanel() {
        int lado = Math.min(LADO_MAXIMO, Math.min(ancho, alto) / 3); // Un tercio de la ventana como máximo.
        if (lado < 120) { // Ventana demasiado pequeña: el texto no cabría.
            return; // Omite el panel antes que dibujarlo ilegible.
        }
        ladoPanel = lado; // Guarda el lado para poder convertir de coordenadas del panel a píxeles.
        // El contenido termina en la lámpara de HORA (z = 26); debajo solo quedaba fondo vacío,
        // así que se recorta por abajo hasta z = 40 y se deja poco margen. La proyección sigue
        // cuadrada para no deformar nada: lo de abajo simplemente queda fuera del recorte.
        int recorte = Math.round((LIMITE - 40) / (2f * LIMITE) * lado); // Píxeles vacíos que se quitan abajo.
        int altoRecorte = lado - recorte; // Altura visible del panel.
        int extra = Math.round(TAMANO_PANEL * 0.9f); // Aire sobre el título, parecido al que queda bajo HORA.
        int altoPanel = altoRecorte + extra; // Altura final con el aire de arriba incluido.
        int x = ancho - lado - MARGEN; // Esquina derecha con margen.
        int y = MARGEN - recorte; // Viewport cuadrado hundido: el borde visible queda a MARGEN del fondo.
        if (texto == null) { // El primer cuadro es también el primero con contexto OpenGL.
            texto = new Texto2D(); // Crea el shader, el quad y la textura del texto.
        }
        texto.redimensionar(lado); // Ajusta el lienzo al lado del recuadro; no hace nada si no cambió.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar la ciudad.
        glScissor(x - 2, y + recorte - 2, lado + 4, altoPanel + 4); // Marco con el aire de arriba incluido.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Mismo marco claro del minimapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el marco gracias al scissor.
        glScissor(x, y + recorte, lado, altoPanel); // Interior con el aire de arriba incluido.
        glClearColor(0.05f, 0.08f, 0.13f, 1); // Fondo oscuro para que el texto blanco se lea.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad del interior.
        glViewport(x, y, lado, lado); // Proyección cuadrada de siempre; lo recortado queda fuera y no se ve.
        entero("uMapa", 1); // Colores planos como en el minimapa, sin iluminación.
        decimal("uLimite", LIMITE); // El shader ortográfico espera el límite real del mapa.
        try { // Asegura que la ventana completa se restaure aunque falle el panel.
            dibujarContenido(); // Fondo, barra, lámparas y letras del panel.
            texto.dibujar(x, y, ancho, alto); // Pega las letras encima del recuadro ya pintado.
        } finally { // El siguiente cuadro debe volver a pantalla completa.
            entero("uMapa", 0); // Recupera la perspectiva de la escena principal.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque todo.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del panel no haya fallado.
            throw new IllegalStateException("Error OpenGL en panel"); // Expone el error en la consola.
        }
    }

    /** Apaga el velocímetro del título: desde esta lección la velocidad vive en el panel. */
    @Override // Especializa el gancho definido en clase2.
    protected String textoVelocidad() {
        return ""; // Sin texto: evita duplicar el valor que ya muestra la fila VEL.
    }

    /** Libera también la textura de las letras al cerrar la aplicación. */
    @Override // Añade la limpieza del panel a la de clase1.
    protected void limpiar() {
        if (texto != null) { // Solo hace falta si el panel llegó a crearse.
            texto.liberar(); // Devuelve a la GPU lo que ocupa el texto.
        }
        super.limpiar(); // Ejecuta la limpieza original de la ciudad.
    }

    // ==================== 3. FONDO Y FILAS ====================

    /** Dibuja las tres filas del panel sobre el fondo oscuro. */
    private void dibujarContenido() {
        texto.limpiar(); // Borra las letras del cuadro anterior antes de escribir las nuevas.
        caja(0, 30, 0, 110, 1, 110, 0.05f, 0.08f, 0.13f); // Rellena el recuadro; el clear ya lo pintó, la caja lo fija.
        texto.texto("PANEL", columna(-19), fila(-46), Math.round(TAMANO_PANEL), new Color(140, 191, 242)); // Cabecera en azul
                                                                                                                // claro.
        dibujarFilaVelocidad(); // Etiqueta VEL, número en km/h y barra proporcional.
        dibujarFilaFaros(); // Etiqueta FAROS con su estado ON u OFF y su lámpara.
        dibujarFilaHora(); // Etiqueta HORA con DIA o NOCHE y su lámpara.
    }

    /** Muestra la velocidad del auto rojo con número y barra de 0 a 16 m/s. */
    private void dibujarFilaVelocidad() {
        int kmh = Math.round(Math.abs(velocidad) * 3.6f); // Misma conversión que el título de clase2.
        texto.texto("VEL " + kmh + " KM/H", columna(-48), fila(-24), Math.round(TAMANO_TITULO), Color.WHITE); // Texto blanco con
                                                                                                            // el
                                                                                                            // valor
                                                                                                            // actual.
        float fraccion = Math.min(1, Math.abs(velocidad) / 16); // 16 es el tope de avance de clase2.
        caja(0, 40, -14, 96, 1, 6, 0.25f, 0.28f, 0.33f); // Riel gris de la barra.
        float rojo = 0.2f + fraccion * 0.8f; // La barra pasa de verde a rojo al acelerar.
        float verde = 0.8f - fraccion * 0.55f; // El verde baja mientras el rojo sube.
        float anchoRelleno = 96 * fraccion; // Ancho proporcional a la velocidad.
        if (anchoRelleno > 0.5f) { // Evita dibujar una lámina invisible en reposo.
            float centroX = -48 + anchoRelleno / 2; // Alinea el relleno a la izquierda del riel.
            caja(centroX, 41, -14, anchoRelleno, 1, 4, rojo, verde, 0.25f); // Relleno de color según la fracción.
        }
    }

    /** Muestra FAROS ON u OFF con una lámpara que se enciende en cálido. */
    private void dibujarFilaFaros() {
        String estado = faros ? "ON" : "OFF"; // faros viene de clase3 y cambia con la tecla F.
        texto.texto("FAROS " + estado, columna(-48), fila(8), Math.round(TAMANO_TITULO), Color.WHITE); // Base 4 abajo del centro: el medio de la letra queda a la altura de la lampara (z = 4).
        caja(39, 42, 4, 11, 1, 11, 0.25f, 0.28f, 0.33f); // Aro gris detrás de la lámpara.
        if (faros) { // Faros encendidos: lámpara cálida brillante.
            caja(39, 43, 4, 8, 1, 8, 1, 0.9f, 0.5f); // Amarillo cálido como los faros del auto.
        } else { // Faros apagados: lámpara gris tenue.
            caja(39, 43, 4, 8, 1, 8, 0.3f, 0.3f, 0.33f); // Gris apagado.
        }
    }

    /** Muestra HORA DIA o NOCHE con una lámpara de sol o de luna. */
    private void dibujarFilaHora() {
        String momento = noche ? "NOCHE" : "DIA"; // noche viene de clase3 y cambia con la tecla N.
        texto.texto("HORA " + momento, columna(-48), fila(30), Math.round(TAMANO_TITULO), Color.WHITE); // Base 4 abajo del centro: el medio de la letra queda a la altura de la lampara (z = 26).
        caja(39, 42, 26, 11, 1, 11, 0.25f, 0.28f, 0.33f); // Aro gris detrás de la lámpara.
        if (noche) { // De noche: lámpara azul de luna.
            caja(39, 43, 26, 8, 1, 8, 0.3f, 0.5f, 1); // Azul frío.
        } else { // De día: lámpara amarilla de sol.
            caja(39, 43, 26, 8, 1, 8, 1, 0.82f, 0.2f); // Amarillo solar.
        }
    }

    // ==================== 4. CONVERSIÓN DE COORDENADAS ====================

    /*
     * El fondo y las lámparas se dibujan con cajas, que usan el sistema del panel: X y Z van de -LIMITE a
     * +LIMITE. Las letras se dibujan con Texto2D, que usa píxeles de pantalla. Estas dos funciones son el
     * puente entre ambos sistemas y evitan tener que rehacer el panel entero en píxeles.
     */

    /** Convierte una X del panel al píxel de pantalla equivalente, contando desde la izquierda. */
    private int columna(float xMundo) {
        return Math.round((xMundo + LIMITE) / (2f * LIMITE) * ladoPanel);
    }

    /** Convierte una Z del panel al píxel de pantalla equivalente, contando desde arriba. */
    private int fila(float zMundo) {
        return Math.round((zMundo + LIMITE) / (2f * LIMITE) * ladoPanel);
    }

    /** Punto de entrada de la sexta lección. */
    public static void main(String[] args) {
        clase6 aplicacion = new clase6(); // Crea la versión con panel de velocidad y luces.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
