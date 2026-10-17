package com.graphics; // Vive junto a las clases de las que se reutiliza.

import java.awt.Color; // Permite elegir el color de cada letra.
import java.awt.Font; // Usa una fuente real del sistema en vez de una matriz de puntos.
import java.awt.Graphics2D; // Dibuja el texto sobre una imagen en memoria.
import java.awt.RenderingHints; // Activa el suavizado que evita los bordes escalonados.
import java.awt.image.BufferedImage; // Guarda los píxeles del texto antes de mandarlos a la GPU.
import java.nio.ByteBuffer; // Transporta los píxeles hasta OpenGL.
import java.nio.ByteOrder; // Define el orden de los bytes para que la GPU los lea como RGBA.
import java.nio.IntBuffer; // Vista del buffer nativo para escribir un píxel por operación.
import org.lwjgl.BufferUtils; // Reserva el buffer nativo que necesita la GPU.

import static org.lwjgl.opengl.GL11.*; // Triángulos, mezcla de colores, viewport y filtros de textura.
import static org.lwjgl.opengl.GL13.*; // Genera los mipmaps que dan nitidez al reducir la imagen.
import static org.lwjgl.opengl.GL15.*; // Vertex array y vertex buffer del quad de pantalla.
import static org.lwjgl.opengl.GL20.*; // Compila y enlaza los dos shaders del texto.
import static org.lwjgl.opengl.GL30.*; // Vincula el VAO durante el dibujo.

/**
 * TEXTO EN 2D CON FUENTE DEL SISTEMA.
 *
 * <p>
 * Sustituye a las letras formadas con cajas pequeñas, que se veían pixeladas
 * porque cada punto de
 * la fuente 5x7 era un cubo sin suavizado. Ahora el texto se dibuja en memoria
 * con
 * {@link Graphics2D}, que aplica el suavizado de curvas y bordes de una fuente
 * real del sistema, y
 * después se sube a la GPU como textura transparente que se coloca encima del
 * panel.
 *
 * <p>
 * Para que siga nitido aunque la textura sea más grande que el rectángulo que
 * ocupa en pantalla, la
 * imagen se dibuja al doble ({@link #SUPER}) y se filtra con mipmaps, de modo
 * que la GPU reduce el
 * tamaño promediando en vez de tomar muestras sueltas.
 *
 * <p>
 * Solo se vuelve a generar y subir la textura cuando el texto escrito cambia:
 * si en dos cuadros
 * seguidos se dibuja exactamente lo mismo, la GPU no recibe nada nuevo.
 *
 * <p>
 * Coordenadas: {@link #texto} trabaja en píxeles con el origen arriba a la
 * izquierda, igual que
 * cualquier lienzo de Java; {@link #dibujar} recibe la esquina del rectángulo
 * en píxeles de pantalla
 * medidos desde abajo a la izquierda, que es como los mide OpenGL.
 */
public class Texto2D {

    /**
     * Factor de supermuestreo: la imagen del texto tiene el doble de lado que el
     * rectángulo.
     */
    private static final int SUPER = 2;

    private int programa; // Shader propio del texto: no comparte estado con el de la ciudad.
    private int vao; // Describe cómo leer los vértices del quad.
    private int vbo; // Contiene los seis vértices del quad.
    private int textura; // Guarda los píxeles del texto ya suavizados.
    private int uRect; // Uniform con la posición y el tamaño del rectángulo del texto.
    private int uResolucion; // Uniform con el tamaño de la ventana para proyectar a normalizado.

    private int anchoRect; // Ancho en píxeles de pantalla del rectángulo que ocupa el texto.
    private int altoRect; // Alto en píxeles de pantalla del mismo rectángulo (en el panel es igual al
                          // ancho).
    private BufferedImage imagen; // Lienzo en memoria donde Java dibuja las letras.
    private Graphics2D lienzo; // Pincel asociado a esa imagen.
    private int[] pixeles; // Copia plana de la imagen que se envía a la GPU.
    private ByteBuffer transporte; // Buffer nativo con los píxeles de la imagen.
    private final StringBuilder contenido = new StringBuilder(); // Texto dibujado en este cuadro.
    private String enviado = null; // Texto que ya está en la GPU.

    /**
     * Crea el shader, el quad y la textura. Solo puede llamarse con un contexto
     * OpenGL ya activo,
     * es decir desde dentro del ciclo de dibujo.
     */
    public Texto2D() {
        crearPrograma(); // Compila y enlaza el par de shaders del texto.
        crearQuad(); // Prepara el quad de pantalla en el que se pega la textura.
        crearTextura(); // Reserva la textura y sus filtros.
    }

    // ==================== 1. ESCRIBIR SOBRE LA IMAGEN ====================

    /**
     * Prepara el lienzo para un recuadro de {@code ladoNuevo} píxeles de pantalla.
     * Si el tamaño no ha
     * cambiado no hace nada, así que se puede llamar en cada cuadro sin coste.
     */
    public void redimensionar(int ladoNuevo) {
        redimensionar(ladoNuevo, ladoNuevo); // El panel usa un recuadro cuadrado.
    }

    /**
     * Prepara el lienzo para un rectángulo de ancho por alto en píxeles de
     * pantalla.
     * El menú lo usa porque su recuadro es más ancho que alto.
     */
    public void redimensionar(int anchoNuevo, int altoNuevo) {
        if (anchoNuevo == anchoRect && altoNuevo == altoRect) { // El tamaño sigue siendo el mismo.
            return; // El lienzo actual sirve tal cual.
        }
        anchoRect = anchoNuevo; // Guarda el nuevo ancho en píxeles de pantalla.
        altoRect = altoNuevo; // Guarda el nuevo alto en píxeles de pantalla.
        int anchoImagen = anchoRect * SUPER; // La imagen real es el doble para que al reducir quede suave.
        int altoImagen = altoRect * SUPER; // Lo mismo en vertical.
        if (lienzo != null) { // Libera el lienzo del tamaño anterior antes de crear otro.
            lienzo.dispose(); // Cierra el pincel del lienzo viejo.
        }
        imagen = new BufferedImage(anchoImagen, altoImagen, BufferedImage.TYPE_INT_ARGB); // Lienzo con canal alfa.
        lienzo = imagen.createGraphics(); // Crea el pincel 2D sobre ese lienzo.
        lienzo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Suaviza
        // las
        // curvas
        // de las
        // letras.
        lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Suaviza los
                                                                                                     // bordes.
        pixeles = new int[anchoImagen * altoImagen]; // Reserva la copia plana de la imagen.
        transporte = BufferUtils.createByteBuffer(pixeles.length * 4); // Reserva el buffer nativo del tamaño exacto.
        transporte.order(ByteOrder.LITTLE_ENDIAN); // Hace que OpenGL lea los bytes en el orden RGBA que espera.
        contenido.setLength(0); // El lienzo nuevo empieza sin nada escrito.
        enviado = null; // Obliga a subir la imagen nueva a la GPU.
    }

    /** Borra todo lo dibujado antes y da el lienzo por limpio. */
    public void limpiar() {
        if (lienzo == null) { // Todavía no hay lienzo que limpiar.
            return; // No hace nada en el primer cuadro.
        }
        lienzo.setComposite(java.awt.AlphaComposite.Clear); // Cambia a modo transparente.
        lienzo.fillRect(0, 0, imagen.getWidth(), imagen.getHeight()); // Borra toda la superficie.
        lienzo.setComposite(java.awt.AlphaComposite.SrcOver); // Vuelve al modo normal de composición.
        contenido.setLength(0); // El contenido de este cuadro empieza vacío.
    }

    /**
     * Escribe una línea de texto en píxeles. La X es el borde izquierdo y la Y es
     * la línea base, igual
     * que en Java: la letra queda por encima de esa Y y su caja alta sube unas
     * decenas de puntos. El
     * lienzo es transparente, así que el texto se mezcla con lo que ya estuviera
     * dibujado debajo.
     */
    public void texto(String cadena, int x, int y, int tamano, Color color) {
        if (lienzo == null) { // Sin lienzo no hay nada donde escribir.
            return; // Espera a que se llame antes a redimensionar().
        }
        lienzo.setFont(fuente(tamano)); // Usa una fuente real del sistema.
        lienzo.setColor(color); // Elige el color de la tinta.
        lienzo.drawString(cadena, x * SUPER, y * SUPER); // Escribe el texto sobre el lienzo.
        contenido.append(cadena).append('\n'); // Anota qué se ha escrito para saber si hay que subirlo.
    }

    /**
     * Igual que {@link #texto}, pero centra la cadena horizontalmente sobre
     * {@code centroX}.
     */
    public void textoCentrado(String cadena, int centroX, int y, int tamano, Color color) {
        texto(cadena, centroX - anchoDe(cadena, tamano) / 2, y, tamano, color); // La desplaza media cadena a la
                                                                                // izquierda.
    }

    /**
     * Ancho en píxeles de pantalla que ocupará una cadena con ese tamaño de fuente.
     */
    public int anchoDe(String cadena, int tamano) {
        if (lienzo == null) { // Sin lienzo todavía no se puede medir con Java2D.
            return cadena.length() * tamano; // Una estimación razonable mientras no exista imagen.
        }
        return Math.round(lienzo.getFontMetrics(fuente(tamano)).stringWidth(cadena) / (float) SUPER); // Mide con la
                                                                                                      // misma
                                                                                                      // fuente
                                                                                                      // que
                                                                                                      // escribe.
    }

    /**
     * Construye la fuente que se usa para un tamaño dado, ya escalada por el
     * supermuestreo.
     */
    private static Font fuente(int tamano) {
        return new Font(Font.SANS_SERIF, Font.BOLD, Math.max(1, Math.round(tamano * SUPER))); // Misma en todo el flujo.
    }

    // ==================== 2. PASAR LA IMAGEN A LA GPU ====================

    /**
     * Sube la imagen a la textura si hace falta y la dibuja dentro del rectángulo
     * indicado.
     *
     * @param x            esquina izquierda del rectángulo, en píxeles desde la
     *                     izquierda de la ventana.
     * @param y            esquina inferior del rectángulo, en píxeles desde abajo
     *                     de la ventana.
     * @param anchoVentana ancho de la ventana en píxeles, para convertir a
     *                     coordenadas normalizadas.
     * @param altoVentana  alto de la ventana en píxeles, idem para el eje vertical.
     */
    public void dibujar(int x, int y, int anchoVentana, int altoVentana) {
        if (lienzo == null || anchoRect <= 0 || altoRect <= 0) { // Todavía no hay nada que enseñar.
            return; // Sale sin dibujar.
        }
        if (!contenido.toString().equals(enviado)) { // El texto escrito es distinto del que está en la GPU.
            subirTextura(); // Copia la imagen a la textura y genera los mipmaps.
            enviado = contenido.toString(); // Anota que la GPU ya tiene este contenido.
        }
        int[] estado = new int[1]; // Guarda el programa y el VAO que tenía la ciudad para restaurarlos.
        glGetIntegerv(GL_CURRENT_PROGRAM, estado); // Lee el shader que estaba en uso.
        int programaAnterior = estado[0]; // Lo conserva para el final del dibujo.
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, estado); // Lee el VAO enlazado ahora mismo.
        int vaoAnterior = estado[0]; // Lo conserva para el final del dibujo.
        glDisable(GL_DEPTH_TEST); // El texto va siempre por encima, aunque el panel tenga profundidad.
        glEnable(GL_BLEND); // Activa la mezcla para respetar la transparencia de las letras.
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Mezcla normal: color sobre fondo.
        glUseProgram(programa); // Activa el shader propio del texto.
        glBindVertexArray(vao); // Enlaza la descripción de los vértices del quad.
        glActiveTexture(GL_TEXTURE0); // Selecciona la unidad de textura cero.
        glBindTexture(GL_TEXTURE_2D, textura); // Enlaza la textura del texto.
        glUniform4f(uRect, x, y, anchoRect, altoRect); // Dice al shader dónde y con qué tamaño va el quad.
        glUniform2f(uResolucion, anchoVentana, altoVentana); // Le da el tamaño de la ventana para proyectar.
        glViewport(0, 0, anchoVentana, altoVentana); // El texto se coloca en coordenadas de ventana, no en el
                                                     // viewport del panel que estuviera activo antes.
        glDrawArrays(GL_TRIANGLES, 0, 6); // Dibuja los dos triángulos del rectángulo.
        glBindVertexArray(vaoAnterior); // Devuelve el VAO que tenía la ciudad.
        glUseProgram(programaAnterior); // Devuelve el control al shader de la ciudad.
        glDisable(GL_BLEND); // Devuelve el estado de mezcla como estaba.
        glEnable(GL_DEPTH_TEST); // Reactiva la profundidad para el resto de la escena.
    }

    /** Copia la imagen del lienzo a la textura y prepara sus niveles de detalle. */
    private void subirTextura() {
        int ladoImagen = imagen.getWidth(); // Ancho real de la imagen, ya con el supermuestreo.
        int altoImagen = imagen.getHeight(); // Alto real de la imagen, ya con el supermuestreo.
        imagen.getRGB(0, 0, ladoImagen, altoImagen, pixeles, 0, ladoImagen); // Lee la imagen en un arreglo plano.
        transporte.clear(); // Prepara el buffer nativo para escribir desde el principio.
        IntBuffer comoEnteros = transporte.asIntBuffer(); // Vista del mismo buffer, un píxel por operación.
        for (int indice = 0; indice < pixeles.length; indice++) { // Recorre cada píxel de la imagen.
            int pixel = pixeles[indice]; // Color empaquetado ARGB de Java2D.
            int alfa = pixel & 0xFF000000; // Conserva la transparencia.
            int rojo = (pixel >> 16) & 0xFF; // Extrae el rojo.
            int verde = (pixel >> 8) & 0xFF; // Extrae el verde.
            int azul = pixel & 0xFF; // Extrae el azul.
            comoEnteros.put(alfa | (azul << 16) | (verde << 8) | rojo); // Reordena a ABGR para que el orden
                                                                        // little endian deje R,G,B,A en memoria.
        }
        transporte.flip(); // Marca que el buffer ya tiene todos los píxeles escritos.
        glBindTexture(GL_TEXTURE_2D, textura); // Enlaza la textura del texto.
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, ladoImagen, altoImagen, 0, GL_RGBA, GL_UNSIGNED_BYTE, transporte); // Sube
                                                                                                                   // la
        // imagen
        // completa.
        glGenerateMipmap(GL_TEXTURE_2D); // Crea la versión reducida que evita el pixelado al pintar más pequeña.
        glBindTexture(GL_TEXTURE_2D, 0); // Suelta la textura.
    }

    /** Reserva la textura y activa el filtrado que la hace nítida al reducirla. */
    private void crearTextura() {
        textura = glGenTextures(); // Pide un identificador de textura.
        glBindTexture(GL_TEXTURE_2D, textura); // Enlaza la textura recién creada.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR); // Al reducir, promedia los
                                                                                        // niveles.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // Al ampliar, interpola sin escalones.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); // No repite en horizontal.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); // No repite en vertical.
        glBindTexture(GL_TEXTURE_2D, 0); // Suelta la textura.
    }

    // ==================== 3. QUAD Y SHADERS ====================

    /**
     * Crea el quad de pantalla con coordenadas de 0 a 1 y sus coordenadas de
     * textura.
     */
    private void crearQuad() {
        float[] vertices = { // Dos triángulos que cubren todo el rectángulo; la V se invierte porque la
                             // primera fila de la imagen es la de arriba.
                0.0f, 0.0f, 0.0f, 1.0f, // Abajo a la izquierda.
                1.0f, 0.0f, 1.0f, 1.0f, // Abajo a la derecha.
                1.0f, 1.0f, 1.0f, 0.0f, // Arriba a la derecha.
                0.0f, 0.0f, 0.0f, 1.0f, // Abajo a la izquierda, repetido.
                1.0f, 1.0f, 1.0f, 0.0f, // Arriba a la derecha, repetido.
                0.0f, 1.0f, 0.0f, 0.0f // Arriba a la izquierda.
        };
        int[] anterior = new int[1]; // Guarda el VAO que tenía la ciudad para devolverlo al terminar.
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, anterior); // Lee el VAO que está enlazado ahora mismo.
        vao = glGenVertexArrays(); // Pide un identificador de VAO.
        vbo = glGenBuffers(); // Pide un identificador de VBO.
        glBindVertexArray(vao); // Empieza a describir los vértices del quad.
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // Enlaza el buffer donde están los vértices.
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Sube los vértices a la GPU una sola vez.
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0); // Posición del vértice.
        glEnableVertexAttribArray(0); // Activa ese atributo.
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES); // Coordenada de textura.
        glEnableVertexAttribArray(1); // Activa también el segundo atributo.
        glBindVertexArray(anterior[0]); // Devuelve el VAO de la ciudad: el panel se dibuja en medio de su pase.
    }

    /**
     * Compila y enlaza el par de shaders que coloca la imagen del texto sobre el
     * panel.
     */
    private void crearPrograma() {
        int vs = compilar(GL_VERTEX_SHADER,
                """
                        #version 330 core // GLSL 3.30, el mismo perfil que usa el proyecto.
                        layout (location = 0) in vec2 aPos; // Posición del quad, de 0 a 1.
                        layout (location = 1) in vec2 aUv; // Coordenada de la textura del texto.
                        uniform vec4 uRect; // Posición y tamaño del rectángulo en píxeles.
                        uniform vec2 uResolucion; // Tamaño de la ventana en píxeles.
                        out vec2 vUv; // Pasa la coordenada de textura al fragmento.
                        void main() { // Coloca el quad en pantalla.
                            vec2 p = uRect.xy + aPos * uRect.zw; // Lleva el vértice a píxeles de pantalla.
                            vec2 ndc = vec2(p.x / uResolucion.x * 2.0 - 1.0, p.y / uResolucion.y * 2.0 - 1.0); // Pasa a
                                                                                                                                          // coordenadas
                                                                                                                                          // normalizadas.
                            gl_Position = vec4(ndc, 0.0, 1.0); // Publica la posición sin perspectiva.
                            vUv = aUv; // Entrega la coordenada de textura.
                        }
                        """); // Compila el shader de vértices.
        int fs = compilar(GL_FRAGMENT_SHADER, """
                #version 330 core // GLSL 3.30, el mismo perfil que usa el proyecto.
                in vec2 vUv; // Coordenada recibida del shader de vértices.
                uniform sampler2D uTextura; // La imagen del texto.
                out vec4 color; // Color final del fragmento.
                void main() { // Toma el píxel de la imagen del texto.
                    color = texture(uTextura, vUv); // Lo entrega con su transparencia original.
                }
                """); // Compila el shader de fragmentos.
        programa = glCreateProgram(); // Crea el programa que enlaza ambos shaders.
        glAttachShader(programa, vs); // Une el shader de vértices.
        glAttachShader(programa, fs); // Une el shader de fragmentos.
        glLinkProgram(programa); // Enlaza el programa completo.
        glDeleteShader(vs); // El shader ya está dentro del programa: se puede borrar.
        glDeleteShader(fs); // Lo mismo con el de fragmentos.
        uRect = glGetUniformLocation(programa, "uRect"); // Busca dónde está el rectángulo.
        uResolucion = glGetUniformLocation(programa, "uResolucion"); // Busca dónde está el tamaño de ventana.
    }

    /** Compila un shader y aborta con su mensaje de error si no se puede crear. */
    private static int compilar(int tipo, String codigo) {
        int shader = glCreateShader(tipo); // Crea el objeto de shader.
        glShaderSource(shader, codigo); // Le entrega el código fuente.
        glCompileShader(shader); // Lo compila en la GPU.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) { // Comprueba si la compilación falló.
            String error = glGetShaderInfoLog(shader); // Pide el motivo del fallo.
            glDeleteShader(shader); // Libera el shader que no sirve.
            throw new IllegalStateException("Error al compilar el shader del texto: " + error); // Informa del problema.
        }
        return shader; // Devuelve el shader listo para enlazar.
    }

    /** Libera la textura, el quad y el programa cuando la aplicación termina. */
    public void liberar() {

        if (textura != 0) { // Si llegó a crearse la textura.
            glDeleteTextures(textura); // Se libera de la GPU.
        }
        if (vbo != 0) { // Si llegó a crearse el buffer.
            glDeleteBuffers(vbo); // Se libera de la GPU.
        }
        if (vao != 0) { // Si llegó a crearse la descripción de vértices.
            glDeleteVertexArrays(vao); // Se libera de la GPU.
        }
        if (programa != 0) { // Si llegó a crearse el programa.
            glDeleteProgram(programa); // Se libera de la GPU.
        }
        if (lienzo != null) { // Si existía un lienzo en memoria.
            lienzo.dispose(); // Se cierra el pincel.
        }
    }
}