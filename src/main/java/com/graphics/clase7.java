
package com.graphics; // Menu de inicio sobre la ciudad de las seis clases anteriores.

import java.awt.Color; // Elige el color de cada letra del menú.

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
 * El fondo del recuadro se limpia con scissor como en el panel; las letras ya
 * no son una fuente de puntos: se escriben con {@link Texto2D}, la misma
 * fuente real con suavizado del panel de clase6, así que ya no se ven
 * pixeladas.
 * Orden de lectura: estado, teclado, actualizacion, recuadro, contenido y
 * punto de entrada.
 */
public class clase7 extends clase6 {

    // ==================== 1. ESTADO DEL MENU ====================

    private boolean enMenu = true; // true: muestra menu y bloquea conduccion; false: juega normal.

    private Texto2D menuTexto; // Lienzo de letras con fuente real; se crea en el primer cuadro con contexto
                               // OpenGL.

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
        glfwSetWindowTitle(ventana, "Clase 7 | MENU | C = Camara aerea"); // Título fijo en el menú; la orden ya está dentro del recuadro.
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
        int menuW = Math.min(800, (int) (ancho * 0.80)); // Más angosto: menos fondo a los lados, texto centrado igual.
        int menuH = Math.min(530, (int) (alto * 0.88)); // Un poco mas alto, deja poco borde de ciudad.
        if (menuW < 300 || menuH < 200) { // Ventana demasiado pequena: el texto no cabria.
            return; // Omite el recuadro antes que dibujarlo ilegible.
        }
        int x = (ancho - menuW) / 2; // Centra en horizontal.
        int y = (alto - menuH) / 2; // Centra en vertical; OpenGL mide Y desde abajo.
        if (menuTexto == null) { // El primer cuadro es también el primero con contexto OpenGL.
            menuTexto = new Texto2D(); // Crea el shader, el quad y la textura del texto.
        }
        menuTexto.redimensionar(menuW, menuH); // Ajusta el lienzo al recuadro; no hace nada si no cambió.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar la ciudad de alrededor.
        glScissor(x - 12, y - 12, menuW + 24, menuH + 24); // Selecciona el menu mas un borde de doce pixeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Mismo marco claro del minimapa y del panel.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el marco gracias al scissor.
        glScissor(x, y, menuW, menuH); // Reduce el recorte al interior del menu.
        glClearColor(0.05f, 0.08f, 0.13f, 1); // Fondo oscuro para que el texto blanco se lea.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad del interior.
        glViewport(x, y, menuW, menuH); // Redirige la proyección al recuadro centrado.
        entero("uMapa", 1); // Colores planos como en el minimapa, sin iluminacion.
        decimal("uLimite", LIMITE); // El shader ortografico espera el limite real del mapa.
        try { // Asegura que la ventana completa se restaure aunque falle el menu.
            dibujarContenidoMenu(menuW, menuH); // Escribe el titulo y los avisos sobre el lienzo.
            menuTexto.dibujar(x, y, ancho, alto); // Pega las letras encima del recuadro ya pintado.
        } finally { // El siguiente cuadro debe volver a pantalla completa.
            entero("uMapa", 0); // Recupera la perspectiva de la escena principal.
            glDisable(GL_SCISSOR_TEST); // Permite que la proxima limpieza abarque todo.
            glViewport(0, 0, ancho, alto); // Recupera el area de dibujo completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del menu no haya fallado.
            throw new IllegalStateException("Error OpenGL en menu"); // Expone el error en la consola.
        }
    }

    /** Libera también la textura de las letras del menú al cerrar la aplicación. */
    @Override // Añade la limpieza del menú a la del panel.
    protected void limpiar() {
        if (menuTexto != null) { // Solo hace falta si el menú llegó a crearse.
            menuTexto.liberar(); // Devuelve a la GPU lo que ocupa el texto.
        }
        super.limpiar(); // Ejecuta la limpieza del panel y de la ciudad.
    }

    // ==================== 5. CONTENIDO CON FUENTE REAL ====================

    /*
     * Las cuatro líneas conservan las posiciones del menú anterior: cada z0 era la
     * parte alta de la letra en mundo (-38, -20, 6 y 24), así que la línea base en
     * píxeles es esa altura más el alto de la fuente. Los tamaños crecen con el
     * alto del recuadro para que el menú se lea igual en ventanas grandes y chicas.
     */

    /** Escribe el titulo y los avisos sobre el lienzo, todo centrado. */
    private void dibujarContenidoMenu(int menuW, int menuH) {
        menuTexto.limpiar(); // Borra las letras del cuadro anterior antes de escribir las nuevas.
        int titulo = Math.max(24, menuH / 8); // DELIVERY grande: 66 px con el recuadro al máximo.
        int subtitulo = Math.max(20, menuH / 9); // EN LA CIUDAD un poco menor: 58 px al máximo.
        int comenzar = Math.max(16, menuH / 13); // COMENZAR mediano: 40 px al máximo.
        int salir = Math.max(14, menuH / 17); // SALIR más chico: 31 px al máximo.
        Color azul = new Color(140, 191, 242); // Mismo azul claro del título del panel.
        menuTexto.textoCentrado("DELIVERY", menuW / 2, baseMenu(-38, menuH, titulo), titulo, azul); // Título azul
                                                                                                    // claro.
        menuTexto.textoCentrado("EN LA CIUDAD", menuW / 2, baseMenu(-20, menuH, subtitulo), subtitulo, azul); // Segunda
                                                                                                              // parte.
        if ((tiempo % 1.0f) < 0.7f) { // Parpadea cada segundo para llamar la atención.
            menuTexto.textoCentrado("COMENZAR -> W o ^", menuW / 2, baseMenu(6, menuH, comenzar), comenzar,
                    Color.WHITE); // Orden blanca para empezar.
        }
        menuTexto.textoCentrado("SALIR -> ESC", menuW / 2, baseMenu(24, menuH, salir), salir, Color.WHITE); // Aviso
                                                                                                            // fijo.
    }

    /**
     * Convierte la parte alta de una línea en mundo a su línea base en píxeles: la
     * altura en mundo sobre el borde superior más el alto de la fuente real.
     */
    private static int baseMenu(float z0, int menuH, int tamano) {
        int arriba = Math.round((z0 + LIMITE) / (2f * LIMITE) * menuH); // Píxeles desde arriba hasta la parte alta.
        return arriba + Math.round(tamano * 0.9f); // Baja el alto de la fuente para llegar a la línea base.
    }

    /** Punto de entrada de la septima leccion. */
    public static void main(String[] args) {
        clase7 aplicacion = new clase7(); // Crea la version con menu de inicio.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicacion.
    }
}
