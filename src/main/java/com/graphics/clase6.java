package com.graphics; // Continúa la serie junto a las cinco clases anteriores.

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
 * El panel se dibuja con las mismas cajas del proyecto sobre un viewport
 * cuadrado recortado con scissor, igual que el minimapa de clase4 pero abajo
 * a la derecha. Las letras son una fuente 3x5 dibujada con cajas pequeñas,
 * porque el proyecto no usa texturas ni bibliotecas de texto.
 * Orden de lectura: recuadro, fondo y filas, texto, letras y punto de entrada.
 */
public class clase6 extends clase5 {

    // ==================== 1. TAMAÑO DEL RECUADRO ====================

    private static final int LADO_MAXIMO = 230; // Lado del panel en píxeles: agrandado para la N de 5 puntos.
    private static final int MARGEN = 12; // Separación respecto a los bordes derecho e inferior.
    private static final float PIXEL_LETRA = 1.3f; // Punto fino de la fuente 5x7: solo afecta al panel.
    private static final float ALTURA_TEXTO = 55; // Altura Y de las letras: la más alta para quedar por encima.

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
        int x = ancho - lado - MARGEN; // Esquina derecha con margen.
        int y = MARGEN; // Esquina inferior con margen; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar la ciudad.
        glScissor(x - 2, y - 2, lado + 4, lado + 4); // Selecciona el panel más un borde de dos píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Mismo marco claro del minimapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el marco gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del panel.
        glClearColor(0.05f, 0.08f, 0.13f, 1); // Fondo oscuro para que el texto blanco se lea.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad del interior.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Colores planos como en el minimapa, sin iluminación.
        decimal("uLimite", LIMITE); // El shader ortográfico espera el límite real del mapa.
        try { // Asegura que la ventana completa se restaure aunque falle el panel.
            dibujarContenido(); // Fondo, barra, lámparas y letras del panel.
        } finally { // El siguiente cuadro debe volver a pantalla completa.
            entero("uMapa", 0); // Recupera la perspectiva de la escena principal.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque todo.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del panel no haya fallado.
            throw new IllegalStateException("Error OpenGL en panel"); // Expone el error en la consola.
        }
    }

    // ==================== 3. FONDO Y FILAS ====================

    /** Dibuja las tres filas del panel sobre el fondo oscuro. */
    private void dibujarContenido() {
        caja(0, 30, 0, 110, 1, 110, 0.05f, 0.08f, 0.13f); // Rellena el recuadro; el clear ya lo pintó, la caja lo fija.
        dibujarTexto("PANEL", -19, -46, PIXEL_LETRA, 0.55f, 0.75f, 0.95f); // Cabecera centrada en azul claro.
        dibujarFilaVelocidad(); // Etiqueta VEL, número en km/h y barra proporcional.
        dibujarFilaFaros(); // Etiqueta FAROS con su estado ON u OFF y su lámpara.
        dibujarFilaHora(); // Etiqueta HORA con DIA o NOCHE y su lámpara.
    }

    /** Muestra la velocidad del auto rojo con número y barra de 0 a 16 m/s. */
    private void dibujarFilaVelocidad() {
        int kmh = Math.round(Math.abs(velocidad) * 3.6f); // Misma conversión que el título de clase2.
        dibujarTexto("VEL " + kmh + " KM/H", -48, -30, PIXEL_LETRA, 1, 1, 1); // Texto blanco con el valor actual.
        float fraccion = Math.min(1, Math.abs(velocidad) / 16); // 16 es el tope de avance de clase2.
        caja(0, 40, -12, 96, 1, 6, 0.25f, 0.28f, 0.33f); // Riel gris de la barra.
        float rojo = 0.2f + fraccion * 0.8f; // La barra pasa de verde a rojo al acelerar.
        float verde = 0.8f - fraccion * 0.55f; // El verde baja mientras el rojo sube.
        float anchoRelleno = 96 * fraccion; // Ancho proporcional a la velocidad.
        if (anchoRelleno > 0.5f) { // Evita dibujar una lámina invisible en reposo.
            float centroX = -48 + anchoRelleno / 2; // Alinea el relleno a la izquierda del riel.
            caja(centroX, 41, -12, anchoRelleno, 1, 4, rojo, verde, 0.25f); // Relleno de color según la fracción.
        }
    }

    /** Muestra FAROS ON u OFF con una lámpara que se enciende en cálido. */
    private void dibujarFilaFaros() {
        String estado = faros ? "ON" : "OFF"; // faros viene de clase3 y cambia con la tecla F.
        dibujarTexto("FAROS " + estado, -48, 0, PIXEL_LETRA, 1, 1, 1); // Texto blanco con el estado real.
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
        dibujarTexto("HORA " + momento, -48, 22, PIXEL_LETRA, 1, 1, 1); // Texto blanco con el momento real.
        caja(39, 42, 26, 11, 1, 11, 0.25f, 0.28f, 0.33f); // Aro gris detrás de la lámpara.
        if (noche) { // De noche: lámpara azul de luna.
            caja(39, 43, 26, 8, 1, 8, 0.3f, 0.5f, 1); // Azul frío.
        } else { // De día: lámpara amarilla de sol.
            caja(39, 43, 26, 8, 1, 8, 1, 0.82f, 0.2f); // Amarillo solar.
        }
    }

    // ==================== 4. TEXTO CON CAJAS ====================

    /**
     * Dibuja una cadena con la fuente 5x7 a partir de su esquina superior
     * izquierda.
     */
    private void dibujarTexto(String texto, float x0, float z0, float pixel, float r, float g, float b) {
        float cursorX = x0; // Posición horizontal del carácter actual.
        for (int indice = 0; indice < texto.length(); indice++) { // Recorre la cadena letra por letra.
            char letra = texto.charAt(indice); // Carácter actual.
            if (letra == ' ') { // El espacio solo avanza el cursor.
                cursorX += 6 * pixel; // Ancho de un carácter 5x7 más su separación.
                continue; // Pasa a la siguiente letra.
            }
            boolean[][] puntos = glifo(letra); // Matriz de 7 filas por 5 columnas.
            int ancho = puntos[0].length; // Todas las letras usan 5 columnas.
            for (int fila = 0; fila < puntos.length; fila++) { // Recorre las filas de arriba a abajo.
                for (int columna = 0; columna < ancho; columna++) { // Recorre las columnas de la letra.
                    if (!puntos[fila][columna]) { // Punto apagado de la fuente.
                        continue; // No dibuja nada en esa celda.
                    }
                    float px = cursorX + columna * pixel; // X del punto dentro del carácter.
                    float pz = z0 + fila * pixel; // Z del punto; hacia abajo en el panel.
                    caja(px, ALTURA_TEXTO, pz, pixel * 0.92f, 0.6f, pixel * 0.92f, r, g, b); // Punto del carácter.
                }
            }
            cursorX += (ancho + 1) * pixel; // Avanza según el ancho real más su separación.
        }
    }

    /**
     * Devuelve la matriz 7x5 de una letra o dígito. La N usa diagonal
     * progresiva así: X...X / XX..X / XX..X / X.X.X / X..XX / X..XX / X...X.
     * Solo cambia el dibujo de las letras del panel; la ciudad queda igual.
     */
    private static boolean[][] glifo(char letra) {
        String[] filas; // Siete cadenas de cinco caracteres con X encendido y . apagado.
        switch (Character.toUpperCase(letra)) { // Acepta minúsculas aunque el panel usa mayúsculas.
            case 'A':
                filas = new String[] { ".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X" };
                break;
            case 'C':
                filas = new String[] { ".XXXX", "X....", "X....", "X....", "X....", "X....", ".XXXX" };
                break;
            case 'D':
                filas = new String[] { "XXX..", "X..X.", "X...X", "X...X", "X...X", "X..X.", "XXX.." };
                break;
            case 'E':
                filas = new String[] { "XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX" };
                break;
            case 'F':
                filas = new String[] { "XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "X...." };
                break;
            case 'H':
                filas = new String[] { "X...X", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X" };
                break;
            case 'I':
                filas = new String[] { "XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "XXXXX" };
                break;
            case 'K':
                filas = new String[] { "X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X" };
                break;
            case 'L':
                filas = new String[] { "X....", "X....", "X....", "X....", "X....", "X....", "XXXXX" };
                break;
            case 'M':
                filas = new String[] { "X...X", "XX.XX", "X.X.X", "X.X.X", "X...X", "X...X", "X...X" };
                break;
            case 'N':
                filas = new String[] { "X...X", "XX..X", "XX..X", "X.X.X", "X..XX", "X..XX", "X...X" };
                break;
            case 'O':
                filas = new String[] { ".XXX.", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX." };
                break;
            case 'P':
                filas = new String[] { "XXXX.", "X...X", "X...X", "XXXX.", "X....", "X....", "X...." };
                break;
            case 'R':
                filas = new String[] { "XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X" };
                break;
            case 'S':
                filas = new String[] { ".XXXX", "X....", "X....", ".XXX.", "....X", "....X", "XXXX." };
                break;
            case 'T':
                filas = new String[] { "XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.." };
                break;
            case 'U':
                filas = new String[] { "X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX." };
                break;
            case 'V':
                filas = new String[] { "X...X", "X...X", "X...X", "X...X", "X...X", ".X.X.", "..X.." };
                break;
            case '/':
                filas = new String[] { "....X", "....X", "...X.", "..X..", "..X..", ".X...", "X...." };
                break;
            case '0':
                filas = new String[] { ".XXX.", "X...X", "X..XX", "X.X.X", "XX..X", "X...X", ".XXX." };
                break;
            case '1':
                filas = new String[] { "..X..", ".XX..", "..X..", "..X..", "..X..", "..X..", ".XXXX" };
                break;
            case '2':
                filas = new String[] { ".XXX.", "X...X", "....X", "...X.", "..X..", ".X...", "XXXXX" };
                break;
            case '3':
                filas = new String[] { "XXXX.", "....X", "....X", ".XXX.", "....X", "....X", "XXXX." };
                break;
            case '4':
                filas = new String[] { "...X.", "..XX.", "..XX.", ".X.X.", "XXXXX", "...X.", "...X." };
                break;
            case '5':
                filas = new String[] { "XXXXX", "X....", "X....", "XXXX.", "....X", "....X", "XXXX." };
                break;
            case '6':
                filas = new String[] { ".XXX.", "X....", "X....", "XXXX.", "X...X", "X...X", ".XXX." };
                break;
            case '7':
                filas = new String[] { "XXXXX", "....X", "...X.", "..X..", "..X..", "..X..", "..X.." };
                break;
            case '8':
                filas = new String[] { ".XXX.", "X...X", "X...X", ".XXX.", "X...X", "X...X", ".XXX." };
                break;
            case '9':
                filas = new String[] { ".XXX.", "X...X", "X...X", ".XXXX", "....X", "....X", ".XXX." };
                break;
            default:
                filas = new String[] { "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX" };
                break; // Signo raro: bloque lleno.
        }
        boolean[][] puntos = new boolean[filas.length][filas[0].length()]; // Matriz de salida con el alto y ancho real.
        for (int fila = 0; fila < filas.length; fila++) { // Convierte cada cadena en booleanos.
            for (int columna = 0; columna < filas[0].length(); columna++) { // Revisa todos los puntos de la fila.
                puntos[fila][columna] = filas[fila].charAt(columna) == 'X'; // X significa punto encendido.
            }
        }
        return puntos; // Devuelve la letra lista para dibujar.
    }

    /** Punto de entrada de la sexta lección. */
    public static void main(String[] args) {
        clase6 aplicacion = new clase6(); // Crea la versión con panel de velocidad y luces.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
