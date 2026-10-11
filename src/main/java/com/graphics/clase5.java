//Añade 3 vehículos autonomos

package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla T que enciende el tráfico autónomo.

/**
 * CLASE 5: TRÁFICO AUTÓNOMO.
 * Hereda clase4, que hereda clase3, clase2 y clase1; por eso reutiliza la ciudad,
 * la conducción, la iluminación, las entregas y el minimapa sin copiarlos.
 * Añade tres vehículos blancos que circulan sin intervención del usuario: cada uno
 * conserva posición, orientación y velocidad, recorre un circuito cerrado por las
 * calles, gira en cada esquina y continúa su recorrido.
 * Los tres circuitos son concéntricos y giran en el mismo sentido, y cada auto mantiene
 * su carril a la derecha del eje de la calle. Con esa circulación a la derecha los autos
 * que comparten calle nunca se encuentran de frente, así que solo hace falta ceder el
 * paso en los cuatro cruces donde se cortan los circuitos.
 * El tráfico no atraviesa edificios ni sale del mapa porque toda posición propuesta se
 * valida con puedeCircular(), el mismo método que usa el auto del jugador. Además, antes
 * de aceptar cada posición se la compara con los demás vehículos mediante un rectángulo
 * orientado, de modo que los autos autónomos no se pisan entre sí ni con el auto rojo:
 * si el paso está ocupado, el auto frena y, si aún no basta, se aparta hacia el centro.
 * Orden de lectura: estado, reglas, circuitos, controles, actualización, evitación y dibujo.
 */
public class clase5 extends clase4 {

    // ==================== 1. ESTADO DE CADA VEHÍCULO ====================

    /** Estado mínimo que comparten el auto del jugador y los autos autónomos. */
    protected static class Vehiculo {
        protected int id = 0; // El 0 identifica al auto del jugador; 1, 2 y 3 al tráfico autónomo.
        protected float x = 0; // Posición horizontal en el mundo.
        protected float z = 0; // Posición en profundidad en el mundo.
        protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
        protected float velocidad = 0; // Unidades por segundo; el tráfico autónomo nunca retrocede.
    }

    /** Un vehículo autónomo: un vehículo que además recuerda el circuito que debe recorrer. */
    protected static class AutoIA extends Vehiculo {
        protected float[] ruta = new float[0]; // Pares X,Z que describen un circuito cerrado.
        protected int indiceRuta = 1; // Índice del par que se busca ahora; avanza al llegar a él.
        protected float velocidadCrucero = 6; // Velocidad deseada cuando la calle está libre.
        protected float desvio = 0; // Cuánto se ha apartado de su carril; positivo es hacia el lado contrario.
    }

    // ==================== 2. REGLAS DEL TRÁFICO ====================

    protected static final float ALCANCE = 16; // Distancia a la que un vehículo cercano puede estorbar.
    /*
     * MEDIO_LARGO y MEDIO_ANCHO son el radio de la carrocería de cualquier vehículo de la
     * ciudad, el mismo auto rojo de clase2 que el auto blanco: el largo llega hasta los
     * faros y el ancho hasta las ruedas. Con esas dos medidas, y no con dos umbrales
     * independientes, se decide si dos carrocerías se pisan: basta que estén separadas en
     * alguno de sus cuatro ejes, de frente y de costado. Las dos holguras son el margen que
     * se pide además de ese contacto, y al avanzar se pide más que al apartarse de costado,
     * porque al advance el auto va a cerrar distancia mientras que de costado ya se ha
     * desligado por el eje de frente. ANCHO_CARRIL tiene que quedar por encima del contacto
     * de costado, 2 por MEDIO_ANCHO, porque si no un auto podría quedarse detenido por la
     * protección final sin ninguna orden de frenar y se atascaría detrás del otro.
     */
    protected static final float MEDIO_LARGO = 1.36f; // Del centro del auto a su extremo más adelantado, con los faros.
    protected static final float MEDIO_ANCHO = 1f; // Del centro del auto a su rueda más separada.
    protected static final float SEGUIMIENTO = 0.25f; // Margen que se pide al avanzar para no ir pegado detrás.
    protected static final float APARTE = 0.1f; // Margen que se pide al apartarse de costado o al girar.
    protected static final float ANCHO_CARRIL = 2.2f; // Separación lateral a partir de la cual el auto va en otro carril.
    protected static final float CARRIL = 2.4f; // Separación lateral con la que se circula: siempre por la derecha.
    protected static final float MIRA = 9; // Distancia del punto al que se apunta por delante del auto para seguir el carril.
    protected static final float GUIA_MINIMA = 3.6f; // Separación que se quiere mantener detrás de otro auto.
    protected static final float LIBERADO = 4; // Distancia por la que se da por terminado un encuentro.
    protected static final float TAQUIA = 2.5f; // Velocidad del desplazamiento lateral hacia el eje cuando el paso está ocupado.
    protected static final float VELOCIDAD_CRUCE = 4.2f; // Tope de velocidad al entrar en una esquina.
    protected static final float VELOCIDAD_CARA = 3.5f; // Tope de velocidad cuando viene otro auto de frente.
    protected static final float ACELERACION = 3.5f; // Unidades por segundo cuadrado al acelerar.
    protected static final float FRENADO = 8; // Unidades por segundo cuadrado al frenar.
    protected static final float GIRO_SUAVE = 0.30f; // Giro tan pequeño que no obliga a bajar la velocidad.
    protected static final float LLEGADA = 3.0f; // Distancia al punto objetivo que cuenta como esquina alcanzada.

    // ==================== 3. CIRCUITOS SOBRE LAS CALLES ====================

    /*
     * El mapa de clase1 tiene 11 celdas de 10 unidades: los centros de las celdas
     * pares son las calles, es decir X y Z iguales a -50, -30, -10, 10, 30 y 50.
     * Cada circuito es un circuito cerrado: la última esquina repite la primera.
     * Los tres usan calles distintas y giran todos en el mismo sentido, así que solo
     * se cruzan los dos más amplios; el central no se cruza con ninguno.
     */
    protected static final float[] RUTA_EXTERNA = { // Circuito amplio por las calles x = ±50 y z = ±30.
            -50, -30, // Esquina Suroeste: el auto arranca aquí mirando hacia +X.
            50, -30, // Esquina Sureste: gira hacia +Z.
            50, 30, // Esquina Noreste: gira hacia -X.
            -50, 30, // Esquina Noroeste: gira hacia -Z.
            -50, -30 // Cierra el circuito regresando a la esquina de salida.
    };

    protected static final float[] RUTA_MEDIA = { // Circuito intermedio por las calles x = ±30 y z = ±50.
            30, -50, // Esquina Sureste: el auto arranca aquí mirando hacia +Z.
            30, 50, // Esquina Noreste: gira hacia -X.
            -30, 50, // Esquina Noroeste: gira hacia -Z.
            -30, -50, // Esquina Suroeste: gira hacia +X.
            30, -50 // Cierra el circuito.
    };

    protected static final float[] RUTA_CENTRAL = { // Circuito pequeño por las calles x = ±10 y z = ±10.
            -10, -10, // Esquina Suroeste: el auto arranca aquí mirando hacia +X.
            10, -10, // Esquina Sureste: gira hacia +Z.
            10, 10, // Esquina Noreste: gira hacia -X.
            -10, 10, // Esquina Noroeste: gira hacia -Z.
            -10, -10 // Cierra el circuito.
    };

    // ==================== 4. ESTADO DE LA LECCIÓN ====================

    protected AutoIA[] VEHICULOS = crearVehiculos(); // El índice 0 es el auto del jugador; los demás son tráfico autónomo.
    protected boolean traficoActivo = true; // Permite apagar y encender el tráfico con la tecla T.

    /**
     * Crea la lista de vehículos: primero una copia del auto del jugador y después
     * los tres autos autónomos con su circuito, su esquina de salida y su velocidad.
     */
    protected AutoIA[] crearVehiculos() {
        AutoIA jugador = new AutoIA(); // Espejo del auto rojo que conduce el usuario.
        jugador.id = 0; // Su identificador cero permite reconocerlo al recorrer la lista.
        jugador.x = autoX; // Copia la posición horizontal inicial definida por clase2.
        jugador.z = autoZ; // Copia la posición inicial en profundidad definida por clase2.
        jugador.angulo = angulo; // Copia la orientación inicial definida por clase2.
        AutoIA primero = nuevoAuto(1, RUTA_EXTERNA, 0, 7.6f); // Rápido, recorre el circuito más amplio.
        AutoIA segundo = nuevoAuto(2, RUTA_MEDIA, 2, 6.4f); // Velocidad media por el circuito intermedio.
        AutoIA tercero = nuevoAuto(3, RUTA_CENTRAL, 1, 5.2f); // El más tranquilo, en el circuito pequeño.
        return new AutoIA[] { jugador, primero, segundo, tercero }; // La lista completa de vehículos.
    }

    /** Coloca un auto autónomo en su carril, en una esquina de su circuito y ya orientado hacia la siguiente. */
    private AutoIA nuevoAuto(int id, float[] ruta, int inicio, float velocidadCrucero) {
        AutoIA auto = new AutoIA(); // Vehículo vacío que se completa con los datos del circuito.
        auto.id = id; // Identificador dentro de la lista de tráfico.
        auto.ruta = ruta; // El circuito cerrado que seguirá siempre.
        auto.x = ruta[inicio * 2]; // Parte de la esquina inicial en X.
        auto.z = ruta[inicio * 2 + 1]; // Parte de la esquina inicial en Z.
        auto.indiceRuta = inicio + 1; // Su primer objetivo es la esquina siguiente.
        auto.velocidadCrucero = velocidadCrucero; // Velocidad de crucero en calles libres.
        auto.velocidad = velocidadCrucero; // Arranca ya en marcha, sin aceleración progresiva.
        int objetivo = auto.indiceRuta * 2; // Índice del primer objetivo dentro del arreglo de la ruta.
        auto.angulo = anguloHacia(auto.x, auto.z, ruta[objetivo], ruta[objetivo + 1]); // Orienta el frente hacia él.
        auto.x += componenteDerechaX(auto.angulo) * CARRIL; // Se coloca en el carril derecho de su calle.
        auto.z += componenteDerechaZ(auto.angulo) * CARRIL; // Igual en profundidad, para arrancar centrado en el carril.
        return auto; // Devuelve el auto listo para circular.
    }

    // ==================== 5. CONTROLES, REINICIO E INDICADORES ====================

    /** Añade el interruptor del tráfico a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase4.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, cámara, reinicio, luces y minimapa.
        if (key == GLFW_KEY_T) { // Comprueba si se pulsó la tecla del tráfico.
            traficoActivo = !traficoActivo; // Enciende o apaga los tres autos autónomos.
        }
    }

    /** Devuelve también los tres autos a su esquina de salida. */
    @Override // Amplía el reinicio definido por clase4.
    protected void reiniciar() {
        super.reiniciar(); // Restaura auto, entregas y cronómetro.
        VEHICULOS = crearVehiculos(); // Recrea el espejo del jugador y los tres circuitos desde el inicio.
    }

    /** Añade el estado del tráfico al título de la ventana. */
    @Override // Amplía los indicadores de clase3 y clase4.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera luces, minimapa y entregas.
        mensaje += " | T: trafico " + (traficoActivo ? "ON" : "OFF"); // Muestra la tecla y el estado del tráfico.
        mensaje += " (" + (VEHICULOS.length - 1) + " autos) | "; // Indica cuántos autos autónomos circulan.
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 6. UN CUADRO DE TRÁFICO ====================

    /** Actualiza el auto del jugador y después los tres autos autónomos. */
    @Override // Amplía la actualización definida por clase4.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa conducción, entregas y título de la ventana.
        actualizarTrafico(deltaTime); // Mueve el tráfico autónomo con el mismo paso de tiempo.
    }

    /**
     * Avanza todos los autos autónomos un cuadro. Se declara protegido para que las
     * pruebas puedan simular minutos de recorrido sin abrir una ventana OpenGL.
     */
    protected void actualizarTrafico(float deltaTime) {
        VEHICULOS[0].x = autoX; // El espejo del jugador adopta la posición real del auto rojo.
        VEHICULOS[0].z = autoZ; // Igual para la profundidad.
        VEHICULOS[0].angulo = angulo; // Igual para la orientación.
        VEHICULOS[0].velocidad = velocidad; // Igual para la velocidad, incluida la reversa.
        if (!traficoActivo) { // Con el tráfico apagado los autos se quedan quietos.
            return; // Conserva la última posición válida de cada uno.
        }
        for (int indice = 1; indice < VEHICULOS.length; indice++) { // Recorre solo el tráfico autónomo.
            avanzarVehiculo(VEHICULOS[indice], deltaTime); // Integra posición, orientación y velocidad del auto.
        }
    }

/**
     * Recorre un cuadro de un auto autónomo: localiza el tramo de calle en el que está,
     * esquiva, gira, acelera o frena, avanza sobre la calle y salta a la siguiente esquina.
     * La esquiva se mide sobre el eje y el carril de la calle, no sobre el frente del auto, para
     * que el criterio de carril no cambie mientras el vehículo gira en una esquina.
     */
    private void avanzarVehiculo(AutoIA auto, float deltaTime) {
        // --- 1. TRAMO ACTUAL: la calle por la que se circula, su carril derecho y su esquina objetivo.
        int esquinas = auto.ruta.length / 2 - 1; // El último par del arreglo repite la primera esquina.
        int anterior = (auto.indiceRuta - 1 + esquinas) % esquinas; // Esquina de la que parte el tramo actual.
        float desdeX = auto.ruta[anterior * 2]; // X de la esquina de partida del tramo.
        float desdeZ = auto.ruta[anterior * 2 + 1]; // Z de la esquina de partida del tramo.
        float destinoX = auto.ruta[auto.indiceRuta * 2]; // X de la esquina objetivo.
        float destinoZ = auto.ruta[auto.indiceRuta * 2 + 1]; // Z de la esquina objetivo.
        float tramoX = destinoX - desdeX; // Vector del tramo en X.
        float tramoZ = destinoZ - desdeZ; // Vector del tramo en Z.
        float largo = (float) Math.sqrt(tramoX * tramoX + tramoZ * tramoZ); // Longitud real del tramo.
        float pasoX = tramoX / largo; // Dirección de marcha de la calle en X.
        float pasoZ = tramoZ / largo; // Dirección de marcha de la calle en Z.
        float carrilX = -pasoZ; // El costado derecho de la calle está a un cuarto de vuelta de su dirección.
        float carrilZ = pasoX; // El mismo costado derecho medido en profundidad.
        float ancho = CARRIL + auto.desvio; // Separación lateral con la que se está circulando ahora mismo.
        float propio = (auto.x - desdeX) * carrilX + (auto.z - desdeZ) * carrilZ; // Distancia con signo al eje.
        int siguiente = (auto.indiceRuta + 1) % esquinas; // Esquina a la que se irá después de esta.
        float luegoX = auto.ruta[siguiente * 2] - destinoX; // Vector del tramo siguiente en X.
        float luegoZ = auto.ruta[siguiente * 2 + 1] - destinoZ; // Vector del tramo siguiente en Z.
        float largoSiguiente = (float) Math.sqrt(luegoX * luegoX + luegoZ * luegoZ); // Longitud de ese tramo.
        float giraX = luegoX / largoSiguiente; // Dirección del tramo siguiente en X.
        float giraZ = luegoZ / largoSiguiente; // Dirección del tramo siguiente en Z.
        float haciaDentro = pasoX * giraZ - pasoZ * giraX >= 0 ? -1f : 1f; // El viraje acorta hacia el interior.
        float lineaX = destinoX + ancho * (carrilX + haciaDentro * pasoX); // Esquina del carril: cruce de las dos líneas.
        float lineaZ = destinoZ + ancho * (carrilZ + haciaDentro * pasoZ); // El auto gira sin acercarse al eje.

        // --- 2. ESQUIVA: ningún auto corre más de lo que permite el vehículo que tiene delante.
        float limiteVelocidad = auto.velocidadCrucero; // Sin nadie cerca circularía a velocidad de crucero.
        boolean paradoEnCarril = false; // Todavía no hay ningún vehículo parado en el carril que se está usando.
        boolean juntoAlParado = false; // Todavía no se ha terminado de rebasar a un auto parado en esta calle.
        for (int indice = 0; indice < VEHICULOS.length; indice++) { // Revisa el auto del jugador y los otros autos.
            Vehiculo otro = VEHICULOS[indice]; // Vehículo que podría estorbar.
            if (otro == auto) { // Un vehículo no estorba a sí mismo.
                continue; // Pasa al siguiente de la lista.
            }
            float haciaX = otro.x - auto.x; // Separación horizontal respecto del otro vehículo.
            float haciaZ = otro.z - auto.z; // Separación en profundidad respecto del otro vehículo.
            float distancia = (float) Math.sqrt(haciaX * haciaX + haciaZ * haciaZ); // Distancia real entre centros.
            if (distancia < 0.001f || distancia > ALCANCE) { // Demasiado cerca para ser el propio auto, o demasiado lejos.
                continue; // Este vehículo no estorba en este cuadro.
            }
            float adelante = haciaX * pasoX + haciaZ * pasoZ; // Distancia medida a lo largo de la calle.
            if (adelante <= 0 || adelante < -LIBERADO) { // Está al lado, recién rebasado o ya claramente atrás.
                continue; // Ninguno de esos casos obliga a frenar: solo a mirar el guardián de posición.
            }
            float alEje = haciaX * carrilX + haciaZ * carrilZ; // Distancia de costado al eje de la calle, con signo.
            float lateral = alEje - ancho; // Distancia de costado al carril que se está usando.
            if (otro.velocidad < 1f && alEje > 0) { // Hay un auto parado en la mitad derecha de esta calle.
                juntoAlParado = true; // El auto no vuelve a su carril hasta haberlo rebasado del todo.
            }
            if (Math.abs(lateral) > ANCHO_CARRIL) { // El otro auto va por el carril contrario o por la acera.
                continue; // No hace falta frenar: el auto ya está apartado.
            }
            if (otro.velocidad < 1f) { // Hay un vehículo parado en el carril que se está usando.
                paradoEnCarril = true; // Frenar no basta: hay que bordearlo saliendo hacia el eje de la calle.
            }
            float otroFrenteX = componenteFrenteX(otro.angulo); // Dirección frontal del otro auto en X.
            float otroFrenteZ = componenteFrenteZ(otro.angulo); // La misma dirección en profundidad.
            float sentido = pasoX * otroFrenteX + pasoZ * otroFrenteZ; // Su marcha comparada con la de esta calle.
            if (sentido < -0.3f) { // El otro auto viene de frente: no se le puede pasar por su sitio.
                float espera = otro.velocidad < 1f ? 0f : VELOCIDAD_CARA; // Parado se espera; en marcha, con calma.
                limiteVelocidad = Math.min(limiteVelocidad, espera); // Nunca se rebasa a un auto que viene de frente.
            } else { // El otro auto va en el mismo sentido o cruza la calle: se conserva la distancia.
                float permitido = otro.velocidad + (adelante - GUIA_MINIMA) * 1.2f; // Ritmo que aún permite cerrar la brecha.
                if (adelante < GUIA_MINIMA) { // Ya está dentro de la separación deseada.
                    permitido = 0; // Ordena frenar hasta detenerse sin tocarlo.
                }
                limiteVelocidad = Math.min(limiteVelocidad, Math.max(0, permitido)); // El tráfico nunca retrocede.
            }
        }

        // --- 3. PUNTO OBJETIVO: un punto fijo de la línea del carril y, cerca de la esquina, la esquina.
        float recorrido = (auto.x - desdeX) * pasoX + (auto.z - desdeZ) * pasoZ; // Distancia del auto al inicio del tramo.
        float objetivoX = lineaX; // Con la esquina cerca se apunta a ella, porque toca girar.
        float objetivoZ = lineaZ; // Igual en profundidad.
        float mira = MIRA - (largo - recorrido); // Camino que le queda al auto por recorrer con la misma mira.
        if (mira < 0) { // Lejos de la esquina el punto de mira es un punto fijo de la línea del carril.
            objetivoX = desdeX + carrilX * ancho + pasoX * (recorrido + MIRA); // Punto de mira en X.
            objetivoZ = desdeZ + carrilZ * ancho + pasoZ * (recorrido + MIRA); // El mismo punto en Z.
        } else { // Cerca de la esquina el punto de mira avanza por la línea del carril de salida.
            /*
             * La esquina lineaX es el cruce de las dos líneas de carril, es decir la punta
             * interior de la curva, así que apuntar a ella obliga a girar tarde y el auto se
             * va de carril. La curva que respeta el ancho termina una anchura más allá, en
             * linea + gira * ancho; desde ahí el punto de mira sigue por la línea del tramo
             * siguiente, de modo que el giro empieza antes de llegar sin pasarse después.
             */
            objetivoX = lineaX + giraX * (mira + ancho); // Avanza una anchura hasta el final de la curva.
            objetivoZ = lineaZ + giraZ * (mira + ancho); // El mismo punto de mira en profundidad.
        }

        // --- 4. ORIENTACIÓN: gira el frente poco a poco hacia el objetivo.
        float haciaObjetivoX = objetivoX - auto.x; // Separación horizontal hasta el objetivo.
        float haciaObjetivoZ = objetivoZ - auto.z; // Separación en profundidad hasta el objetivo.
        float cuadradoObjetivo = haciaObjetivoX * haciaObjetivoX + haciaObjetivoZ * haciaObjetivoZ; // Cuadrado de la
                                                                                                        // distancia.
        float distanciaObjetivo = (float) Math.sqrt(cuadradoObjetivo); // Distancia real al objetivo.
        float diferencia = 0; // Sin distancia que corregir el auto mantiene su orientación.
        if (distanciaObjetivo > 0.001f) { // Hay un objetivo al que apuntar.
            float apuntado = (float) Math.atan2(-haciaObjetivoX, -haciaObjetivoZ); // Ángulo que apunta al objetivo.
            diferencia = normalizar(apuntado - auto.angulo); // Error de giro que queda entre -π y π.
            if (Math.abs(diferencia) > GIRO_SUAVE) { // El giro es de esquina: hay que frenar antes de entrar.
                limiteVelocidad = Math.min(limiteVelocidad, VELOCIDAD_CRUCE); // Limita la velocidad de la maniobra.
            }
        }
        float giroMaximo = (1.1f + auto.velocidad * 0.4f) * deltaTime; // Giro máximo de este cuadro.
        float girado = auto.angulo + limitar(diferencia, -giroMaximo, giroMaximo); // Orientación de este cuadro.
        if (!ocupado(auto, girado, auto.x, auto.z, APARTE)) { // Al girar, la carrocería no debe invadir a nadie.
            auto.angulo = girado; // Aplica el giro sin pasarse del máximo.
        }

        // --- 5. VELOCIDAD: acelera hasta el límite o frena hasta él.
        if (auto.velocidad < limiteVelocidad) { // El auto va más despacio de lo permitido.
            auto.velocidad = Math.min(limiteVelocidad, auto.velocidad + ACELERACION * deltaTime); // Acelera un poco más.
        } else { // El auto va más rápido de lo permitido.
            auto.velocidad = Math.max(limiteVelocidad, auto.velocidad - FRENADO * deltaTime); // Frena hasta el límite.
        }

        // --- 6. POSICIÓN: avanza sobre la calle y, si el paso está ocupado, se aparta de su carril.
        float frenteX = componenteFrenteX(auto.angulo); // El frente del auto, ya girado, en X.
        float frenteZ = componenteFrenteZ(auto.angulo); // El mismo frente medido en profundidad.
        float nuevoX = auto.x + frenteX * auto.velocidad * deltaTime; // Propone la nueva posición horizontal.
        float nuevoZ = auto.z + frenteZ * auto.velocidad * deltaTime; // Propone la nueva posición en profundidad.
        boolean sinMapa = puedeCircular(nuevoX, nuevoZ); // La posición propuesta no es una manzana ni el borde.
        boolean libre = sinMapa && !ocupado(auto, auto.angulo, nuevoX, nuevoZ, SEGUIMIENTO); // Y además no pisa a nadie.
        boolean apartado = false; // Todavía no se ha apartado del carril en este cuadro.
        if (libre && auto.velocidad > 0 && !paradoEnCarril) { // El paso está despejado y el auto está en marcha.
            auto.x = nuevoX; // Acepta el desplazamiento horizontal.
            auto.z = nuevoZ; // Acepta el desplazamiento en profundidad.
        } else if (!libre || paradoEnCarril) { // Hay una manzana, un borde del mapa, un vehículo en el paso o un
            // auto parado en el carril: en cualquiera de los casos el auto se aparta de su carril.
            float desplazamiento = desplazamientoLibre(auto, carrilX, carrilZ, propio, deltaTime); // Cuánto sale de su carril.
            if (desplazamiento != 0) { // Hay por lo menos un costado libre por el que apartarse.
                auto.x -= carrilX * desplazamiento; // Sale de su carril de costado, sin avanzar.
                auto.z -= carrilZ * desplazamiento; // La misma salida medida en profundidad.
                auto.desvio = limitar(auto.desvio - desplazamiento, -CARRIL, CARRIL); // Recuerda cuánto se ha apartado.
                apartado = true; // Este cuadro sí se ha apartado del carril.
            } else { // Tapado por los dos costados: no hay ninguna posición válida.
                auto.velocidad = 0; // Se detiene conservando su última posición válida.
            }
        }
        if (!apartado && !juntoAlParado) { // Sin nada que rebasar, el auto vuelve poco a poco a su carril.
            auto.desvio += (0 - auto.desvio) * Math.min(1f, 2.5f * deltaTime); // El regreso al carril es más lento.
        }

        // --- 7. RUTA: al alcanzar la esquina, salta a la siguiente.
        float haciaDestinoX = lineaX - auto.x; // Separación horizontal hasta la esquina del carril.
        float haciaDestinoZ = lineaZ - auto.z; // Separación en profundidad hasta la esquina del carril.
        float cuadradoDestino = haciaDestinoX * haciaDestinoX + haciaDestinoZ * haciaDestinoZ; // Cuadrado de la distancia.
        float distanciaDestino = (float) Math.sqrt(cuadradoDestino); // Distancia real a la esquina del carril.
        boolean llego = distanciaDestino < LLEGADA; // Ya está dentro del radio de llegada.
        boolean paso = haciaDestinoX * frenteX + haciaDestinoZ * frenteZ < 0; // O el auto dejó la esquina atrás.
        if (llego || paso) { // Cualquiera de las dos señales indica que toca cambiar de objetivo.
            auto.indiceRuta = (auto.indiceRuta + 1) % esquinas; // Avanza y vuelve al inicio al cerrar el circuito.
        }
    }

    /**
     * Calcula cuánto se aparta el auto de su carril en este cuadro. La salida natural al rebasar
     * a alguien es hacia el eje de la calle, y hacia el lado contrario solo se sale cuando el eje
     * está tapado, que es lo que ocurre si el auto que estorba se ha quedado justo en el centro.
     * Ya en el eje el auto no se aparta más de costado: hacia fuera no cabe y hacia dentro
     * volvería al carril, así que lo que le queda es frenar y esperar a que se despeje el paso.
     */
    private float desplazamientoLibre(AutoIA auto, float carrilX, float carrilZ, float propio, float deltaTime) {
        float paso = TAQUIA * deltaTime; // Cuánto se aparta el auto en un cuadro.
        float alEje = Math.min(paso, Math.max(0, propio)); // Hacia el eje, sin cruzarlo nunca.
        if (alEje <= 0) { // El auto ya está en el eje: no tiene salida de costado.
            return 0; // Espera a que se despeje el paso.
        }
        if (costeLibre(auto, carrilX, carrilZ, alEje)) { // Queda calle entre el carril y el eje.
            return alEje; // Es la salida de siempre y además está libre.
        }
        if (costeLibre(auto, carrilX, carrilZ, -paso)) { // Se prueba el costado contrario de la calle.
            return -paso; // El eje estaba tapado, pero el otro lado sirve.
        }
        return 0; // Los dos costados están tapados: el auto no tiene por dónde apartarse.
    }

    /** Dice si el auto puede separarse de su carril el desplazamiento indicado sin pisar nada. */
    private boolean costeLibre(AutoIA auto, float carrilX, float carrilZ, float desplazamiento) {
        float ladoX = auto.x - carrilX * desplazamiento; // La posición de destino al apartarse, en X.
        float ladoZ = auto.z - carrilZ * desplazamiento; // La misma posición de destino, en profundidad.
        return puedeCircular(ladoX, ladoZ) && !ocupado(auto, auto.angulo, ladoX, ladoZ, APARTE); // Calle libre y sin pisar a nadie.
    }

/**
     * Comprueba si una carrocería, con la orientación y la posición que se le indican, queda
     * encima de otro vehículo. Las dos cajas se tratan como rectángulos orientados y se comparan
     * con el teorema del eje separador: están separadas si en alguno de los cuatro ejes, el de
     * frente y el de costado de cada auto, sus proyecciones no se pisan, y basta uno solo para
     * que no haya contacto. La holgura es un parámetro: se pide más al avanzar que al apartarse
     * de costado o al girar, porque al avanzar el auto va a cerrar distancia mientras que en los
     * otros dos casos ya se ha desligado por el eje de frente. Se pregunta también por la
     * orientación, porque un auto parado sigue girando y al girar también topa.
     */
    private boolean ocupado(AutoIA auto, float angulo, float nuevoX, float nuevoZ, float holgura) {
        for (int indice = 0; indice < VEHICULOS.length; indice++) { // Revisa todos los vehículos de la ciudad.
            Vehiculo otro = VEHICULOS[indice]; // Vehículo que podría solaparse.
            if (otro == auto) { // El propio auto no se solapa consigo mismo.
                continue; // Pasa al siguiente de la lista.
            }
            float haciaX = otro.x - nuevoX; // Separación horizontal desde la posición propuesta.
            float haciaZ = otro.z - nuevoZ; // Separación en profundidad desde la posición propuesta.
            if (Math.abs(haciaX) > ALCANCE || Math.abs(haciaZ) > ALCANCE) { // Está demasiado lejos en un eje.
                continue; // No hay riesgo de solaparse.
            }
            if (!pisan(angulo, otro.angulo, haciaX, haciaZ, holgura)) { // Algún eje los separa ya.
                continue; // No hay riesgo de solaparse.
            }
            return true; // La posición propuesta pisa a otro vehículo: se descarta.
        }
        return false; // Ningún vehículo estorba: la posición es válida.
    }

    /**
     * Dice si dos carrocerías orientadas se pisan. Se recorren los cuatro ejes posibles, el
     * de frente y el de costado de cada una, y si en alguno la separación entre centros
     * supera lo que ocupan las dos cajas, esa dirección las separa y no hay contacto posible.
     */
    private static boolean pisan(float anguloA, float anguloB, float haciaX, float haciaZ, float holgura) {
        float[][] ejes = { // Los cuatro ejes que hay que comprobar.
                { componenteFrenteX(anguloA), componenteFrenteZ(anguloA) }, // Frente del primer vehículo.
                { componenteDerechaX(anguloA), componenteDerechaZ(anguloA) }, // Costado del primer vehículo.
                { componenteFrenteX(anguloB), componenteFrenteZ(anguloB) }, // Frente del segundo vehículo.
                { componenteDerechaX(anguloB), componenteDerechaZ(anguloB) } // Costado del segundo vehículo.
        };
        for (int indice = 0; indice < ejes.length; indice++) { // Recorre los cuatro ejes.
            float ejeX = ejes[indice][0]; // Componente X del eje que se está midiendo.
            float ejeZ = ejes[indice][1]; // Componente Z del mismo eje.
            float distancia = Math.abs(haciaX * ejeX + haciaZ * ejeZ); // Distancia entre centros en ese eje.
            float radio = alcance(anguloA, ejeX, ejeZ) + alcance(anguloB, ejeX, ejeZ) + holgura; // Lo que ocupan.
            if (distancia > radio) { // En esta dirección las cajas no se pisan.
                return false; // Un solo eje que las separe basta: no hay contacto posible.
            }
        }
        return true; // Ningún eje las separa: las carrocerías se tocan de verdad.
    }

    /** Distancia a la que el centro de un vehículo se sale ya de su propia carrocería, en un eje dado. */
    private static float alcance(float angulo, float ejeX, float ejeZ) {
        float frenteX = componenteFrenteX(angulo); // Frente del vehículo en X.
        float frenteZ = componenteFrenteZ(angulo); // El mismo frente en profundidad.
        float derechaX = componenteDerechaX(angulo); // Costado derecho del vehículo en X.
        float derechaZ = componenteDerechaZ(angulo); // El mismo costado en profundidad.
        float largo = MEDIO_LARGO * Math.abs(frenteX * ejeX + frenteZ * ejeZ); // Largo proyectado sobre el eje.
        float ancho = MEDIO_ANCHO * Math.abs(derechaX * ejeX + derechaZ * ejeZ); // Ancho proyectado sobre el eje.
        return largo + ancho; // Es el radio de la carrocería medido sobre ese eje.
    }

    // ==================== 7. DIBUJO DE LOS AUTOS BLANCOS ====================

    /** Añade los autos autónomos a la ciudad y sus marcas al minimapa. */
    @Override // Amplía el dibujo acumulado de las cuatro etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto rojo, la decoración y el destino.
        entero("uEmision", 0); // Los autos se iluminan como cualquier otro objeto de la calle.
        for (int indice = 1; indice < VEHICULOS.length; indice++) { // Recorre solo el tráfico autónomo.
            dibujarAutoBlanco(VEHICULOS[indice]); // Añade un vehículo blanco con su posición y orientación.
        }
        if (vistaMapa) { // El minimapa necesita marcas altas para verse sobre los edificios.
            dibujarMarcasTrafico(); // Coloca una marca blanca por cada auto autónomo.
        }
    }

    /**
     * Construye un auto blanco con el mismo modelo del jugador, que dibuja
     * clase2. No se repite ninguna medida ni ningún bucle: solo se cambian el
     * origen, la orientación y los colores.
     */
    private void dibujarAutoBlanco(AutoIA auto) {
        dibujarCarroceria(auto.x, auto.z, auto.angulo, 0.97f, 0.97f, 0.98f, // Carrocería blanca.
                0.72f, 0.79f, 0.83f); // Cabina gris azulada.
    }

    /** Dibuja una marca blanca y su punta para localizar cada auto en el minimapa. */
    private void dibujarMarcasTrafico() {
        for (int indice = 1; indice < VEHICULOS.length; indice++) { // Recorre solo el tráfico autónomo.
            AutoIA auto = VEHICULOS[indice]; // Auto que se quiere marcar.
            cajaGirada(auto.x, 24, auto.z, 1.8f, 0.1f, 2.6f, 0.85f, 0.88f, 0.92f, auto.angulo); // Rectángulo con su frente.
            float frenteX = componenteFrenteX(auto.angulo); // Dirección frontal en X.
            float frenteZ = componenteFrenteZ(auto.angulo); // Dirección frontal en Z.
            caja(auto.x + frenteX * 1.8f, 25, auto.z + frenteZ * 1.8f, 0.7f, 0.1f, 0.7f, 1, 1, 1); // Punta blanca del
                                                                                                   // frente.
        }
    }

    /*
     * Los ángulos de los vehículos (componenteFrenteX, componenteFrenteZ, componenteDerechaX,
     * componenteDerechaZ, anguloHacia, normalizar y limitar) y el dibujo de un auto (dibujarCarroceria,
     * pieza y piezaEn) ya no se repiten aquí: son los de clase2 y esta lección los hereda.
     */

    /** Punto de entrada de la quinta lección. */
    public static void main(String[] args) {
        clase5 aplicacion = new clase5(); // Crea la versión con tráfico autónomo.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
