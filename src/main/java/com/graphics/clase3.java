//agrega modo noche/dia, farolas y focos del auto 

package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Conserva ciudad y conducción; añade sol, farolas y focos del vehículo.
 * El shader calcula iluminación local: este ejemplo todavía no proyecta
 * sombras.
 */
public class clase3 extends clase2 {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Inicia la escena con iluminación nocturna.
    protected boolean faros = true; // Inicia los focos del auto encendidos.

    // NUEVO: Array dinámico de farolas (se rellena en iniciarLucesSiNecesario()).
    // Una farola por cada edificio/parque (MAPA != 0) en su esquina NO.
    protected float[][] LUCES = new float[0][3];
    protected int numLuces = 0; // Número real de farolas activas.

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de clase2.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Alterna entre noche y día.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que clase2 incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | N: " + ambiente + " | F: faros " + estadoFaros; // Devuelve ambos indicadores y sus teclas.
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /**
     * Genera las farolas la primera vez que se necesitan (inicialización perezosa).
     * Recorre MAPA y crea una farola en la esquina NO de cada edificio/parque.
     */
    private void iniciarLucesSiNecesario() {
        if (numLuces > 0)
            return; // Ya inicializado.
        java.util.ArrayList<float[]> lista = new java.util.ArrayList<>();
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int col = 0; col < MAPA[fila].length; col++) {
                if (MAPA[fila][col] != 0) { // Solo celdas con edificio (1) o parque (2) u hospitales (3)
                    float x = centro(col) - 4f; // Esquina NO: centroX - 5 (borde acera) + 1 (mitad poste)
                    float z = centro(fila) - 4f; // Esquina NO: centroZ - 5 + 1
                    lista.add(new float[] { x, 4.5f, z }); // Y=4.5: altura de la bombilla
                }
            }
        }
        LUCES = lista.toArray(new float[0][3]);
        numLuces = LUCES.length; // 25 farolas (5x5 edificios en filas/cols 1,3,5,7,9)
    }

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    @Override // Sustituye el método vacío de clase1.
    protected void prepararLuces() {
        iniciarLucesSiNecesario(); // NUEVO: inicializa farolas en el primer frame (shader ya compilado).
        entero("uNoche", noche ? 1 : 0); // 1 = noche, 0 = día.
        entero("uFaros", faros ? 1 : 0); // Activa/desactiva focos del auto.
        entero("uEmision", 0); // Material normal por defecto.
        entero("uNumLuces", numLuces); // Pasa el número real de farolas al shader.

        // Envía la posición de cada farola al array uLuces[25] del shader.
        for (int i = 0; i < numLuces; i++) {
            vector("uLuces[" + i + "]", LUCES[i][0], LUCES[i][1], LUCES[i][2]);
        }

        // Posición y dirección del auto para los faros delanteros.
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        vector("uAuto", autoX, 0.7f, autoZ); // Centro del vehículo a altura de focos.
        vector("uFrente", frenteX, 0, frenteZ); // Dirección frontal del vehículo.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    /**
     * Dibuja postes y bombillas en las posiciones generadas por iniciarLuces().
     */
    @Override // Añade geometría a la ciudad y al auto heredados.
    protected void escena() {
        super.escena(); // Dibuja la ciudad y el vehículo de las primeras dos lecciones.
        // Recorre TODAS las farolas calculadas dinámicamente (numLuces, no LUCES.length
        // fijo).
        for (int i = 0; i < numLuces; i++) {
            float x = LUCES[i][0];
            float y = LUCES[i][1];
            float z = LUCES[i][2];
            caja(x, 2.2f, z, 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f); // Poste delgado y oscuro.
            if (noche) { // Las bombillas aparentan estar encendidas únicamente de noche.
                entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            }
            caja(x, y, z, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f); // Bombilla de color cálido.
            entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
        }
    }

    // ==================== 5. CÁLCULO DE LUZ EN LA GPU ====================

    /**
     * Devuelve el programa GLSL que calcula el color iluminado de cada fragmento.
     */
    @Override // Reemplaza el shader de color plano de clase1.
    protected String fragmentShader() {
        return """
                #version 330 core // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
                in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
                in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
                uniform vec3 uColor; // Recibe el color base de la caja.
                uniform vec3 uLuces[25]; // NUEVO: Array ampliado a 25 farolas (5x5 edificios).
                uniform int  uNumLuces;  // NUEVO: Número real de farolas activas (para no iterar basura).
                uniform vec3 uAuto; // Recibe la posición del auto a la altura de los faros.
                uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
                uniform int uNoche; // Vale 1 de noche y 0 de día.
                uniform int uFaros; // Vale 1 cuando los focos están encendidos.
                uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
                uniform int uMapa; // Vale 1 durante el dibujo del minimapa de clase4.
                out vec4 color; // Entrega el color RGBA final al framebuffer.

                void main() { // Se ejecuta para cada fragmento visible de una caja.
                    if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
                        color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
                        return; // Termina el shader sin calcular iluminación.
                    }

                    vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
                    vec3 luz = vec3(0.48); // Define la luz ambiental diurna que llega a todas las caras.
                    float intensidadSol = 0.65; // Define la fuerza de la iluminación direccional diurna.
                    if (uNoche == 1) { // Ajusta el ambiente si es de noche.
                        luz = vec3(0.12, 0.16, 0.24); // Usa una luz ambiental tenue y azulada.
                        intensidadSol = 0.10; // Conserva una pequeña luz direccional nocturna.
                    }
                    vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // Define una fuente lejana por su dirección.
                    float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe más luz si mira al sol.
                    luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribución direccional al ambiente.

                    if (uNoche == 1) { // Calcula la iluminación de las farolas solo de noche.
                        // NUEVO: Itera solo hasta uNumLuces (25 reales), no tamaño fijo del array.
                        for (int i = 0; i < uNumLuces; i++) {
                            vec3 haciaLuz = uLuces[i] - vMundo; // Vector desde la superficie hacia la farola.
                            float distancia = length(haciaLuz); // Distancia superficie-bombilla.
                            float difusa = max(dot(normal, normalize(haciaLuz)), 0.0); // Incidencia de la luz sobre la cara.
                            float atenuacion = 1.0 + 0.12 * distancia + 0.045 * distancia * distancia; // Atenuación cuadrática.
                            vec3 colorFarola = vec3(1.0, 0.73, 0.34); // Tono cálido de la farola.
                            luz += colorFarola * difusa * 3.0 / atenuacion; // Suma aporte atenuado de esta bombilla.
                        }
                    }

                    if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
                        for (int i = 0; i < 2; i++) { // Dos faros: izquierdo y derecho.
                            vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Dirección lateral derecha del auto.
                            float separacion = (i == 0) ? -0.55 : 0.55; // Izquierdo (-) o derecho (+).
                            vec3 origen = uAuto + uFrente * 1.36 + lateral * separacion; // Posición del faro delante del auto.
                            vec3 haciaSuperficie = vMundo - origen; // Vector del faro al fragmento.
                            float distancia = length(haciaSuperficie); // Distancia recorrida por la luz.
                            vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Eje del cono inclinado hacia el suelo.
                            float alineacion = dot(normalize(haciaSuperficie), eje); // 1 = centro del haz.
                            float cono = smoothstep(0.85, 0.97, alineacion); // Suaviza borde del cono.
                            float difusa = max(dot(normal, -normalize(haciaSuperficie)), 0.0); // Cara mira al faro.
                            float atenuacion = 1.0 + 0.04 * distancia * distancia; // Atenuación por distancia.
                            vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Luz frontal blanca y cálida.
                            luz += colorFaro * cono * difusa * 8.0 / atenuacion; // Añade aporte del foco.
                        }
                    }

                    color = vec4(uColor * luz, 1.0); // Multiplica material por toda la luz acumulada.
                }
                """; // Finaliza el texto GLSL que Java enviará a OpenGL.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        clase3 aplicacion = new clase3(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
