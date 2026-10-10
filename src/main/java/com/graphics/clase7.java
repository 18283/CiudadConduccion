
package com.graphics; // Menu de inicio sobre la ciudad de las seis clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Teclas W, flecha arriba y titulo de la ventana.
import static org.lwjgl.opengl.GL33.*; // Recorte, limpieza y viewport del recuadro del menu.

/**
 * CLASE 7: MENU DE INICIO.
 * Hereda clase6, que hereda clase5, clase4, clase3, clase2 y clase1; por eso
 * reutiliza la ciudad, la conduccion, la iluminacion, las entregas, el
 * minimapa, el trafico y el panel sin copiarlos.
 * Al arrancar muestra la ciudad de fondo con un recuadro central que dice
 * MENU, controles y PULSA W. El auto no se mueve hasta pulsar W o flecha
 * arriba (^). Todo el menu queda dentro de esta clase.
 * Orden de lectura: estado, teclado, actualizacion, recuadro, contenido,
 * texto, letras y punto de entrada.
 */
public class clase7 extends clase6 {

    // ==================== 1. ESTADO DEL MENU ====================

    private boolean enMenu = true; // true: muestra menu y bloquea conduccion; false: juega normal.
    private static final float PIXEL_MENU = 0.9f; // Punto de la fuente del menu: chico para que quepa en 110 unidades.
    private static final float ALTURA_MENU = 55; // Altura Y de las letras del menu.

    // ==================== 2. TECLADO ====================

    /**
     * Conserva ESC, camara, reinicio, luces, mapa y trafico; sale del menu con W o
     * arriba.
     */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C, R, N, F, M y T.
        if (enMenu && (key == GLFW_KEY_W || key == GLFW_KEY_UP)) { // Teclas de avance para empezar.
            enMenu = false; // La ciudad queda libre y se puede conducir.
        }
    }

    // ==================== 3. ACTUALIZACION BLOQUEADA ====================

    /**
     * Mientras el menu esta activo la ciudad gira despacio de fondo pero el
     * auto, las entregas y el trafico no avanzan. Al mantener W o flecha
     * arriba se sale del menu y se empieza a conducir.
     */
    @Override
    protected void actualizar(float deltaTime) {
        if (!enMenu) { // Juego normal.
            super.actualizar(deltaTime); // Conduccion, entregas, trafico y titulo.
            return;
        }
        orbita += deltaTime * 0.15f; // Gira la camara aerea despacio para ver la ciudad detras del menu.
        tiempo += deltaTime; // Mueve el parpadeo del menu y los semaforos de fondo.
        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba mantenidas.
            enMenu = false; // Sale del menu en el mismo cuadro.
            super.actualizar(0); // Refresca el titulo con los controles de conduccion.
            return;
        }
        glfwSetWindowTitle(ventana, "Clase 7 | MENU | Pulsa W o flecha arriba (^) para empezar"); // Aviso fijo en el
                                                                                                  // menu.
    }

    // ==================== 4. RECUADRO CENTRAL ====================

    /** Dibuja la ciudad con panel y minimapa, y encima el recuadro del menu. */
    @Override
    protected void dibujarFrame() {
        super.dibujarFrame(); // Vista principal, minimapa de clase4 y panel de clase6.
        if (enMenu) { // Solo tapa el centro mientras no se haya empezado.
            dibujarMenu(); // Recuadro con titulo, controles y orden de pulsar W.
        }
    }

    /** Recorta un rectangulo al centro y dibuja el contenido del menu. */
    private void dibujarMenu() {
        int menuW = Math.min(980, (int) (ancho * 0.96)); // Un poco mas ancho, casi todo el ancho.
        int menuH = Math.min(530, (int) (alto * 0.88)); // Un poco mas alto, deja poco borde de ciudad.
        if (menuW < 300 || menuH < 200) { // Ventana demasiado pequena: el texto no cabria.
            return; // Omite el recuadro antes que dibujarlo ilegible.
        }
        int x = (ancho - menuW) / 2; // Centra en horizontal.
        int y = (alto - menuH) / 2; // Centra en vertical; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar la ciudad de alrededor.
        glScissor(x - 12, y - 12, menuW + 24, menuH + 24); // Selecciona el menu mas un borde de doce pixeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Mismo marco claro del minimapa y del panel.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el marco gracias al scissor.
        glScissor(x, y, menuW, menuH); // Reduce el recorte al interior del menu.
        glClearColor(0.05f, 0.08f, 0.13f, 1); // Fondo oscuro para que el texto blanco se lea.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad del interior.
        glViewport(x, y, menuW, menuH); // Redirige la proyeccion al recuadro centrado.
        entero("uMapa", 1); // Colores planos como en el minimapa, sin iluminacion.
        decimal("uLimite", LIMITE); // El shader ortografico espera el limite real del mapa.
        try { // Asegura que la ventana completa se restaure aunque falle el menu.
            dibujarContenidoMenu(); // Titulo, controles y aviso de pulsar W.
        } finally { // El siguiente cuadro debe volver a pantalla completa.
            entero("uMapa", 0); // Recupera la perspectiva de la escena principal.
            glDisable(GL_SCISSOR_TEST); // Permite que la proxima limpieza abarque todo.
            glViewport(0, 0, ancho, alto); // Recupera el area de dibujo completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del menu no haya fallado.
            throw new IllegalStateException("Error OpenGL en menu"); // Expone el error en la consola.
        }
    }

    // ==================== 5. CONTENIDO ====================

    /** Dibuja titulo y aviso parpadeante dentro del recuadro, todo centrado. */
    private void dibujarContenidoMenu() {
        caja(0, 30, 0, 110, 1, 110, 0.05f, 0.08f, 0.13f); // Rellena el recuadro; el clear ya lo pinto, la caja lo fija.
        dibujarTextoCentrado("DELIVERY", -38, 1.5f, 0.55f, 0.75f, 0.95f); // Titulo azul claro, grande en dos lineas.
        dibujarTextoCentrado("EN LA CIUDAD", -20, 1.3f, 0.55f, 0.75f, 0.95f); // Segunda parte del titulo.
        if ((tiempo % 1.0f) < 0.7f) { // Parpadea cada segundo para llamar la atencion.
            dibujarTextoCentrado("COMENZAR -> W o ^", 6, 0.85f, 1, 1, 1); // Orden blanca para empezar.
        }
        dibujarTextoCentrado("SALIR -> ESC", 24, 0.6f, 1, 1, 1); // Aviso fijo, no parpadea.
    }

    /** Calcula el ancho y centra la cadena en X antes de dibujarla. */
    private void dibujarTextoCentrado(String texto, float z0, float pixel, float r, float g, float b) {
        float anchoTexto = texto.length() * 6 * pixel - pixel; // Todos los caracteres avanzan 6 pixeles.
        dibujarTextoMenu(texto, -anchoTexto / 2, z0, pixel, r, g, b); // x0 centrado en el recuadro.
    }

    // ==================== 6. TEXTO CON CAJAS ====================

    /** Dibuja una cadena con la fuente 5x7 desde su esquina superior izquierda. */
    private void dibujarTextoMenu(String texto, float x0, float z0, float pixel, float r, float g, float b) {
        float cursorX = x0; // Posicion horizontal del caracter actual.
        for (int indice = 0; indice < texto.length(); indice++) { // Recorre la cadena letra por letra.
            char letra = texto.charAt(indice); // Caracter actual.
            if (letra == ' ') { // El espacio solo avanza el cursor.
                cursorX += 6 * pixel; // Ancho de un caracter 5x7 mas su separacion.
                continue; // Pasa a la siguiente letra.
            }
            boolean[][] puntos = glifoMenu(letra); // Matriz de 7 filas por 5 columnas, incluye la flecha ^.
            int ancho = puntos[0].length; // Todas las letras usan 5 columnas.
            for (int fila = 0; fila < puntos.length; fila++) { // Recorre las filas de arriba a abajo.
                for (int columna = 0; columna < ancho; columna++) { // Recorre las columnas de la letra.
                    if (!puntos[fila][columna]) { // Punto apagado de la fuente.
                        continue; // No dibuja nada en esa celda.
                    }
                    float px = cursorX + columna * pixel; // X del punto dentro del caracter.
                    float pz = z0 + fila * pixel; // Z del punto; hacia abajo en el menu.
                    caja(px, ALTURA_MENU, pz, pixel * 1.05f, 0.6f, pixel * 1.05f, r, g, b); // Punto levemente solapado:
                                                                                            // trazo solido.
                }
            }
            cursorX += (ancho + 1) * pixel; // Avanza segun el ancho real mas su separacion.
        }
    }

    /** Devuelve la matriz 7x5 de una letra, digito o simbolo del menu. */
    private static boolean[][] glifoMenu(char letra) {
        String[] filas; // Siete cadenas de cinco caracteres con X encendido y . apagado.
        switch (Character.toUpperCase(letra)) { // Acepta minusculas aunque el menu usa mayusculas.
            case 'A':
                filas = new String[] { ".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X" };
                break;
            case 'B':
                filas = new String[] { "XXXX.", "X...X", "X...X", "XXXX.", "X...X", "X...X", "XXXX." };
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
            case 'G':
                filas = new String[] { ".XXXX", "X....", "X....", "X.XXX", "X...X", "X...X", ".XXX." };
                break;
            case 'H':
                filas = new String[] { "X...X", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X" };
                break;
            case 'I':
                filas = new String[] { "XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "XXXXX" };
                break;
            case 'J':
                filas = new String[] { "..XXX", "...X.", "...X.", "...X.", "...X.", "X..X.", ".XX.." };
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
            case 'Q':
                filas = new String[] { ".XXX.", "X...X", "X...X", "X...X", "X.X.X", "X..X.", ".XX.X" };
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
            case 'W':
                filas = new String[] { "X...X", "X...X", "X...X", "X.X.X", "X.X.X", "XX.XX", "X...X" };
                break;
            case 'X':
                filas = new String[] { "X...X", "X...X", ".X.X.", "..X..", ".X.X.", "X...X", "X...X" };
                break;
            case 'Y':
                filas = new String[] { "X...X", "X...X", ".X.X.", "..X..", "..X..", "..X..", "..X.." };
                break;
            case 'Z':
                filas = new String[] { "XXXXX", "....X", "...X.", "..X..", ".X...", "X....", "XXXXX" };
                break;
            case '/':
                filas = new String[] { "....X", "....X", "...X.", "..X..", "..X..", ".X...", "X...." };
                break;
            case '-':
                filas = new String[] { ".....", ".....", ".....", "XXXXX", ".....", ".....", "....." };
                break;
            case '>':
                filas = new String[] { "X....", ".X...", "..X..", "...X.", "..X..", ".X...", "X...." };
                break;
            case '^': // Acento circunflejo: V volcada, sin cuerpo de flecha.
                filas = new String[] { "..X..", ".X.X.", "X...X", ".....", ".....", ".....", "....." };
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

    /** Punto de entrada de la septima leccion. */
    public static void main(String[] args) {
        clase7 aplicacion = new clase7(); // Crea la version con menu de inicio.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicacion.
    }
}
