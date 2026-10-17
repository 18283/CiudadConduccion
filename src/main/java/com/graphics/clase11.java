package com.graphics; // Sigue la serie junto a las diez clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas para la direccion.

/**
 * CLASE 11: CHATA (REMOLQUE) ARTICULADA AL AUTO DEL JUGADOR.
 * Hereda clase10, que hereda clase9, clase8, clase7, clase6, clase5, clase4,
 * clase3, clase2 y clase1; por eso reutiliza la ciudad, la conduccion, la
 * iluminacion, las entregas, el minimapa, el trafico, el panel, el menu, los
 * peatones, los retrovisores y los autos agrandados sin copiarlos.
 * 
 * Agrega una chata (remolque tradicional de carga con doble eje) unida
 * mediante una bocha de enganche por detras exclusivamente al auto del jugador.
 * A diferencia de un cuerpo rigido soldado, la chata es ARTICULADA:
 *  - Pivotea libremente sobre la bocha de enganche con su propio angulo (anguloChata).
 *  - Sigue la cinematica de arrastre al avanzar, recortando las curvas de manera realista.
 *  - Reacciona dinamicamente en reversa (permitiendo maniobras y tijereo realista).
 *  - Posee un limite fisico de articulacion para evitar que golpee el habitaculo del auto.
 *  - La chata cuenta con lanza triangular, chasis de acero negro, piso y barandas
 *    de madera barnizada, arquillos superiores tubulares para lona, doble eje con
 *    4 ruedas, guardabarros y luces traseras.
 * 
 * Si la chata colisiona o se atasca con una acera, edificio, limite del mapa o auto
 * del trafico, el auto del jugador no puede avanzar ni doblar hacia el obstaculo
 * y se detiene (velocidad = 0), obligando al conductor a maniobrar para liberarla.
 * 
 * Orden de lectura: variables y pivote, actualizacion cinematica, deteccion de atasco,
 * camara ajustada, dibujo articulado y punto de entrada.
 */
public class clase11 extends clase10 {

    // ==================== 1. VARIABLES DE LA CHATA ARTICULADA ====================

    /** Orientacion independiente de la chata en radianes (pivota sobre la bocha). */
    protected float anguloChata = 0;

    /** Distancia desde el centro del auto hasta la bocha de enganche en el parachoques trasero. */
    protected static final float DIST_ENGANCHE = 1.74f;

    /** Distancia entre la bocha de enganche y el centro de los ejes de la chata. */
    protected static final float DISTANCIA_LANZA_EJE = 2.21f;

    /** Limite maximo de articulacion angular entre auto y chata (25 grados para que no se doble en exceso). */
    protected static final float MAX_ARTICULACION = (float) Math.toRadians(25);

    /** Factor de amortiguacion del doble eje: aporta firmeza y evita que la chata quede suelta. */
    protected static final float AMORTIGUACION_GIRO = 0.35f;

    /** Indica si la chata colisiono contra un obstaculo en el ultimo cuadro. */
    protected boolean chataAtascada = false;

    /**
     * Puntos estructurales de la chata para la comprobacion de colisiones.
     * Estructura: { X_local, Z_local, radio_colision } con origen en la bocha de enganche.
     */
    private static final float[][] PUNTOS_COLISION_CHATA = {
            { 0.00f, 0.70f, 0.25f }, // Lanza y acople
            { 0.00f, 2.21f, 0.45f }, // Centro de la caja de carga y ejes
            { 0.00f, 3.40f, 0.40f }  // Parte posterior y compuerta trasera
    };

    /**
     * Constructor: ubica el auto y la chata alineados en una posicion segura
     * dentro de la avenida inicial (autoZ = 43).
     */
    public clase11() {
        super();
        this.autoZ = 43.0f;
        this.anguloChata = this.angulo;
    }

    /**
     * Restablece el auto y la chata al reiniciar la partida con la tecla 'R'.
     */
    @Override
    protected void reiniciar() {
        super.reiniciar();
        autoZ = 43.0f;
        anguloChata = angulo;
        chataAtascada = false;
    }

    // ==================== 2. CINEMÁTICA Y FÍSICA DE ARTICULACIÓN ====================

    /**
     * Actualiza la conducción del auto y calcula la cinemática de arrastre / pivoteo
     * de la chata sobre la bocha de enganche.
     * Si la chata choca (contra edificios, parques, hospitales, límites o tráfico),
     * el vehículo se queda quieto y no avanza hacia el obstáculo.
     * Si el jugador maniobra alejándose del obstáculo hacia espacio libre, el auto
     * puede salir y desatascarse sin quedar trabado.
     */
    @Override
    protected void actualizar(float deltaTime) {
        float prevX = autoX;
        float prevZ = autoZ;
        float prevAngulo = angulo;
        float prevVelocidad = velocidad;
        float prevAnguloChata = anguloChata;

        // Permite girar el volante en parado o muy baja velocidad para poder orientar
        // las ruedas hacia el espacio libre y maniobrar para salir de cualquier atasco:
        if (Math.abs(velocidad) < 0.6f && deltaTime > 0) {
            float dirVolante = 0;
            if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) dirVolante += 1;
            if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) dirVolante -= 1;
            if (dirVolante != 0) {
                angulo += dirVolante * 1.3f * deltaTime;
            }
        }

        super.actualizar(deltaTime); // Procesa aceleracion, direccion y limites del auto.

        // Si el vehiculo se esta desplazando, la chata pivota con firmeza y suavidad:
        if (deltaTime > 0 && Math.abs(velocidad) > 0.001f) {
            float deltaAngulo = angulo - anguloChata;

            // Normaliza la diferencia angular al intervalo [-PI, PI]:
            while (deltaAngulo > (float) Math.PI) deltaAngulo -= (float) (2 * Math.PI);
            while (deltaAngulo < (float) -Math.PI) deltaAngulo += (float) (2 * Math.PI);

            // Al avanzar (velocidad > 0), el doble eje de la chata ofrece resistencia y autocentrado:
            if (velocidad > 0) {
                float tasaGiro = (velocidad / DISTANCIA_LANZA_EJE) * (float) Math.sin(deltaAngulo) * AMORTIGUACION_GIRO;
                float autocentrado = deltaAngulo * 1.8f * deltaTime; // Tiende a estabilizarse en recta
                anguloChata += (tasaGiro * deltaTime) + autocentrado;
            } else {
                // En reversa responde con pivoteo amortiguado:
                float tasaGiro = (velocidad / DISTANCIA_LANZA_EJE) * (float) Math.sin(deltaAngulo) * AMORTIGUACION_GIRO;
                anguloChata += tasaGiro * deltaTime;
            }

            // Restringe la articulacion angular maxima a un arco corto y controlado (25°):
            float difFinal = angulo - anguloChata;
            while (difFinal > (float) Math.PI) difFinal -= (float) (2 * Math.PI);
            while (difFinal < (float) -Math.PI) difFinal += (float) (2 * Math.PI);

            if (difFinal > MAX_ARTICULACION) {
                anguloChata = angulo - MAX_ARTICULACION;
            } else if (difFinal < -MAX_ARTICULACION) {
                anguloChata = angulo + MAX_ARTICULACION;
            }
        }

        // Medimos la penetracion de la chata en obstaculos antes y despues del movimiento:
        float penPrev = calcularPenetracion(prevX, prevZ, prevAngulo, prevAnguloChata);
        float penNueva = calcularPenetracion(autoX, autoZ, angulo, anguloChata);

        // Si la chata no invade ningun obstaculo:
        if (penNueva <= 0.001f) {
            chataAtascada = false;
        } else {
            // La chata colisiona con un obstaculo (parque, edificio, hospital, limite o trafico).
            // Si el movimiento propuesto reduce la invasion (se aleja del obstaculo hacia espacio libre):
            if (penNueva < penPrev - 0.0001f) {
                chataAtascada = false; // Se permite el movimiento para que el auto salga
            } else {
                // El auto intenta empujar hacia el obstaculo: se queda quieto y no avanza
                autoX = prevX;
                autoZ = prevZ;
                angulo = prevAngulo;
                anguloChata = prevAnguloChata;
                velocidad = 0;
                chataAtascada = true;

                if (VEHICULOS != null && VEHICULOS.length > 0 && VEHICULOS[0] != null) {
                    VEHICULOS[0].x = autoX;
                    VEHICULOS[0].z = autoZ;
                    VEHICULOS[0].angulo = angulo;
                    VEHICULOS[0].velocidad = 0;
                }
            }
        }
    }

    // ==================== 3. DETECCIÓN DE ATASCO Y SALIDA ====================

    /**
     * Calcula la maxima penetracion de la chata en todos los obstaculos de la ciudad
     * (parques, edificios, hospitales, limites del mapa o vehiculos del trafico).
     * Devuelve 0 si la chata esta libre en la calle.
     * Devuelve un valor mayor a 0 si algun punto invade un obstaculo.
     */
    protected float calcularPenetracion(float carX, float carZ, float carAngulo, float trailerAngulo) {
        // 1. Posicion mundial de la bocha de enganche en el parachoques trasero:
        float cosCar = (float) Math.cos(carAngulo);
        float sinCar = (float) Math.sin(carAngulo);
        float engancheX = carX + sinCar * DIST_ENGANCHE;
        float engancheZ = carZ + cosCar * DIST_ENGANCHE;

        // 2. Orientacion mundial de la chata pivotada:
        float cosT = (float) Math.cos(trailerAngulo);
        float sinT = (float) Math.sin(trailerAngulo);

        float maxPen = 0;

        for (float[] punto : PUNTOS_COLISION_CHATA) {
            float lx = punto[0];
            float lz = punto[1];
            float radio = punto[2];

            float mundoX = engancheX + cosT * lx + sinT * lz;
            float mundoZ = engancheZ - sinT * lx + cosT * lz;

            // 1. Limites exteriores del mapa de la ciudad:
            float limite = LIMITE - radio;
            if (Math.abs(mundoX) > limite) {
                float pen = Math.abs(mundoX) - limite;
                if (pen > maxPen) maxPen = pen;
            }
            if (Math.abs(mundoZ) > limite) {
                float pen = Math.abs(mundoZ) - limite;
                if (pen > maxPen) maxPen = pen;
            }

            // 2. Todas las manzanas de la ciudad (edificios, parques, hospitales):
            for (int fila = 0; fila < MAPA.length; fila++) {
                for (int columna = 0; columna < MAPA[fila].length; columna++) {
                    if (MAPA[fila][columna] == 0) continue; // 0 = calle libre; 1, 2 y 3 son obstaculos

                    float centroX = centro(columna);
                    float centroZ = centro(fila);
                    float cercaX = Math.max(centroX - 5, Math.min(mundoX, centroX + 5));
                    float cercaZ = Math.max(centroZ - 5, Math.min(mundoZ, centroZ + 5));
                    float dx = mundoX - cercaX;
                    float dz = mundoZ - cercaZ;
                    float distSq = dx * dx + dz * dz;

                    if (distSq < radio * radio) {
                        float dist = (float) Math.sqrt(distSq);
                        float pen = radio - dist;
                        if (pen > maxPen) maxPen = pen;
                    }
                }
            }

            // 3. Vehiculos del trafico autonomo:
            if (VEHICULOS != null) {
                for (int i = 1; i < VEHICULOS.length; i++) {
                    Vehiculo otro = VEHICULOS[i];
                    if (otro == null) continue;
                    float dx = mundoX - otro.x;
                    float dz = mundoZ - otro.z;
                    float distMin = radio + RADIO_AUTO;
                    float distSq = dx * dx + dz * dz;

                    if (distSq < distMin * distMin) {
                        float dist = (float) Math.sqrt(distSq);
                        float pen = distMin - dist;
                        if (pen > maxPen) maxPen = pen;
                    }
                }
            }
        }
        return maxPen;
    }


    /**
     * Muestra aviso en el titulo cuando la chata queda atascada.
     */
    @Override
    protected String estadoExtra() {
        String mensaje = super.estadoExtra();
        if (chataAtascada) {
            mensaje += " | [CHATA ATASCADA]";
        }
        return mensaje;
    }

    // ==================== 4. CÁMARA DE SEGUIMIENTO ====================

    /**
     * Encuadra en tercera persona el auto y la chata articulada.
     */
    @Override
    protected void configurarCamara() {
        if (camaraAerea) {
            super.configurarCamara();
            return;
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 14.5f;
        float camaraZ = autoZ + (float) Math.cos(angulo) * 14.5f;
        vector("uOjo", camaraX, 10.0f, camaraZ);
        vector("uObjetivo", autoX, 0.85f, autoZ);
        decimal("uAspecto", (float) ancho / alto);
    }

    // ==================== 5. DIBUJO DE LA CHATA ARTICULADA ====================

    /**
     * Dibuja el auto y la chata articulada exclusivamente para el jugador.
     */
    @Override
    protected void dibujarAuto() {
        super.dibujarAuto(); // Auto agrandado del jugador (clase10 + clase9).

        // 1. Bocha de enganche fijada al parachoques trasero del auto:
        pieza(0, 0.48f, DIST_ENGANCHE, 0.16f, 0.16f, 0.16f, 0.18f, 0.18f, 0.20f);

        // 2. Coordenadas mundiales exactas de la bocha de enganche:
        float cosCar = (float) Math.cos(angulo);
        float sinCar = (float) Math.sin(angulo);
        float engancheX = autoX + sinCar * DIST_ENGANCHE;
        float engancheZ = autoZ + cosCar * DIST_ENGANCHE;

        // 3. Dibuja la chata completa pivotando libremente con anguloChata:
        dibujarChataPivotada(engancheX, engancheZ, anguloChata);
    }

    /**
     * Construye geometricamente la chata articulada usando la bocha como origen
     * y rotando segun anguloChata.
     */
    protected void dibujarChataPivotada(float origX, float origZ, float rotChata) {
        float aceroR = 0.12f, aceroG = 0.12f, aceroB = 0.14f;
        float maderaR = 0.52f, maderaG = 0.29f, maderaB = 0.14f;
        float maderaOscR = 0.42f, maderaOscG = 0.23f, maderaOscB = 0.10f;
        float gomaR = 0.06f, gomaG = 0.06f, gomaB = 0.07f;
        float llantaR = 0.70f, llantaG = 0.70f, llantaB = 0.72f;

        // ----------------- 5.1. LANZA TRIANGULAR DE TIRO -----------------
        // Barra central de acople a la bocha:
        piezaEn(origX, origZ, rotChata, 0, 0.48f, 0.21f, 0.12f, 0.12f, 0.30f, aceroR, aceroG, aceroB);
        // Placa identificativa en la lanza (como en la foto de referencia):
        piezaEn(origX, origZ, rotChata, 0, 0.49f, 0.38f, 0.22f, 0.07f, 0.02f, 0.95f, 0.85f, 0.20f);

        // Brazos triangulares en 'V':
        piezaEn(origX, origZ, rotChata, -0.28f, 0.46f, 0.51f, 0.10f, 0.10f, 0.42f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, 0.28f, 0.46f, 0.51f, 0.10f, 0.10f, 0.42f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, -0.58f, 0.46f, 0.76f, 0.10f, 0.10f, 0.38f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, 0.58f, 0.46f, 0.76f, 0.10f, 0.10f, 0.38f, aceroR, aceroG, aceroB);

        // ----------------- 5.2. CHASIS Y BASE -----------------
        // Largueros longitudinales de acero:
        piezaEn(origX, origZ, rotChata, -0.92f, 0.42f, 2.21f, 0.12f, 0.10f, 2.70f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, 0.92f, 0.42f, 2.21f, 0.12f, 0.10f, 2.70f, aceroR, aceroG, aceroB);
        // Travesanos frontal y posterior del chasis:
        piezaEn(origX, origZ, rotChata, 0, 0.42f, 0.88f, 1.96f, 0.10f, 0.12f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, 0, 0.42f, 3.54f, 1.96f, 0.10f, 0.12f, aceroR, aceroG, aceroB);

        // Piso de madera:
        piezaEn(origX, origZ, rotChata, 0, 0.49f, 2.21f, 1.84f, 0.06f, 2.62f, maderaOscR, maderaOscG, maderaOscB);

        // ----------------- 5.3. PAREDES Y BARANDAS DE MADERA -----------------
        // Pared delantera:
        piezaEn(origX, origZ, rotChata, 0, 0.82f, 0.91f, 1.84f, 0.60f, 0.06f, maderaR, maderaG, maderaB);
        // Compuerta trasera:
        piezaEn(origX, origZ, rotChata, 0, 0.82f, 3.51f, 1.84f, 0.60f, 0.06f, maderaR, maderaG, maderaB);

        // Barandas laterales de listones de madera:
        float[] ladosBaranda = { -0.90f, 0.90f };
        for (float lx : ladosBaranda) {
            piezaEn(origX, origZ, rotChata, lx, 0.98f, 2.21f, 0.06f, 0.26f, 2.60f, maderaR * 1.05f, maderaG * 1.05f, maderaB * 1.05f);
            piezaEn(origX, origZ, rotChata, lx, 0.68f, 2.21f, 0.06f, 0.26f, 2.60f, maderaR * 0.92f, maderaG * 0.92f, maderaB * 0.92f);
        }

        // ----------------- 5.4. POSTES Y PASAMANOS DE FIERRO NEGRO -----------------
        float[] esquinasX = { -0.92f, 0.92f };
        float[] esquinasZ = { 0.91f, 3.51f };
        for (float ex : esquinasX) {
            for (float ez : esquinasZ) {
                piezaEn(origX, origZ, rotChata, ex, 0.80f, ez, 0.08f, 0.70f, 0.08f, aceroR, aceroG, aceroB);
            }
        }
        float[] parantesZ = { 1.77f, 2.65f };
        for (float ex : esquinasX) {
            for (float pz : parantesZ) {
                piezaEn(origX, origZ, rotChata, ex, 0.80f, pz, 0.08f, 0.70f, 0.08f, aceroR, aceroG, aceroB);
            }
        }
        for (float ex : esquinasX) {
            piezaEn(origX, origZ, rotChata, ex, 1.14f, 2.21f, 0.08f, 0.06f, 2.68f, aceroR, aceroG, aceroB);
        }
        piezaEn(origX, origZ, rotChata, 0, 1.14f, 0.91f, 1.88f, 0.06f, 0.08f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, 0, 1.14f, 3.51f, 1.88f, 0.06f, 0.08f, aceroR, aceroG, aceroB);

        // ----------------- 5.5. ARQUILLOS TUBULARES DE SOSTÉN (CARPA) -----------------
        float[] arquillosZ = { 0.91f, 1.77f, 2.65f, 3.51f };
        for (float az : arquillosZ) {
            piezaEn(origX, origZ, rotChata, -0.92f, 1.48f, az, 0.06f, 0.64f, 0.06f, aceroR, aceroG, aceroB);
            piezaEn(origX, origZ, rotChata, 0.92f, 1.48f, az, 0.06f, 0.64f, 0.06f, aceroR, aceroG, aceroB);
            piezaEn(origX, origZ, rotChata, 0, 1.82f, az, 1.76f, 0.06f, 0.06f, aceroR, aceroG, aceroB);
            piezaEn(origX, origZ, rotChata, -0.84f, 1.78f, az, 0.12f, 0.06f, 0.06f, aceroR, aceroG, aceroB);
            piezaEn(origX, origZ, rotChata, 0.84f, 1.78f, az, 0.12f, 0.06f, 0.06f, aceroR, aceroG, aceroB);
        }
        piezaEn(origX, origZ, rotChata, -0.92f, 1.82f, 2.21f, 0.05f, 0.05f, 2.64f, aceroR, aceroG, aceroB);
        piezaEn(origX, origZ, rotChata, 0.92f, 1.82f, 2.21f, 0.05f, 0.05f, 2.64f, aceroR, aceroG, aceroB);

        // ----------------- 5.6. DOBLE EJE, RUEDAS Y GUARDABARROS -----------------
        float[] ejesZ = { 1.81f, 2.61f };
        for (float ez : ejesZ) {
            for (float sx : esquinasX) {
                float ladoRueda = sx > 0 ? 1.10f : -1.10f;
                float ladoLlanta = sx > 0 ? 1.18f : -1.18f;
                piezaEn(origX, origZ, rotChata, ladoRueda, 0.38f, ez, 0.22f, 0.54f, 0.54f, gomaR, gomaG, gomaB);
                piezaEn(origX, origZ, rotChata, ladoLlanta, 0.38f, ez, 0.06f, 0.30f, 0.30f, llantaR, llantaG, llantaB);
            }
        }
        for (float sx : esquinasX) {
            float ladoG = sx > 0 ? 1.10f : -1.10f;
            piezaEn(origX, origZ, rotChata, ladoG, 0.70f, 2.21f, 0.30f, 0.06f, 1.60f, aceroR, aceroG, aceroB);
            piezaEn(origX, origZ, rotChata, ladoG, 0.56f, 1.36f, 0.28f, 0.22f, 0.10f, aceroR, aceroG, aceroB);
            piezaEn(origX, origZ, rotChata, ladoG, 0.56f, 3.06f, 0.28f, 0.22f, 0.10f, aceroR, aceroG, aceroB);
        }

        // ----------------- 5.7. LUCES TRASERAS Y REFLECTORES -----------------
        entero("uEmision", 1);
        piezaEn(origX, origZ, rotChata, -0.75f, 0.56f, 3.58f, 0.20f, 0.14f, 0.05f, 0.95f, 0.08f, 0.08f);
        piezaEn(origX, origZ, rotChata, 0.75f, 0.56f, 3.58f, 0.20f, 0.14f, 0.05f, 0.95f, 0.08f, 0.08f);
        piezaEn(origX, origZ, rotChata, 0, 0.54f, 3.58f, 0.30f, 0.12f, 0.04f, 0.95f, 0.88f, 0.30f);
        entero("uEmision", 0);
    }

    // ==================== 6. PUNTO DE ENTRADA ====================

    /** Punto de entrada de la undecima leccion. */
    public static void main(String[] args) {
        clase11 aplicacion = new clase11(); // Crea la version con la chata articulada.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicacion.
    }
}
