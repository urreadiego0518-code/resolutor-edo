# Resolutor de EDO: Euler vs Runge-Kutta 4

> Versión de escritorio. Para la app de Android y los binarios ya compilados, mira el
> [README del repositorio](../README.md).

Proyecto de **NetBeans (Java Application, Ant)** que resuelve ecuaciones diferenciales
`dy/dx = f(x, y)` con el **método de Euler** y el **método de Runge-Kutta de 4º orden**,
y los compara con tabla de iteraciones, gráfica y error. No usa librerías externas.

## Cómo abrirlo en NetBeans

1. Descomprime el `.zip`.
2. En NetBeans: **Archivo → Abrir proyecto…** y elige la carpeta `ResolutorEDO`
   (se reconoce por el icono de taza de café).
3. Ejecuta con **F6** (o clic derecho en el proyecto → *Run*). La clase principal es `edo.Main`.

Requisitos: JDK 8 o superior y NetBeans 8.2 o Apache NetBeans (cualquier versión).
La primera vez NetBeans genera solo el archivo `nbproject/build-impl.xml`; es normal.

**Si NetBeans no lo reconoce:** crea un proyecto nuevo (*Java with Ant → Java Application*),
borra la clase `Main` que genera y copia dentro del paquete `edo` los 6 archivos de `src/edo/`.
Sin NetBeans también funciona con `compilar_y_ejecutar.bat` (Windows) o `compilar_y_ejecutar.sh`.

## Qué hace la aplicación

- Escribes la ecuación como texto (`x - y`, `-0.5*(y-20)`, `y*cos(x)`...), `x0`, `y0`, `h` y `x final`.
- **Gráfica**: Euler, RK4 y solución exacta. Al pasar el ratón muestra los valores de cada paso.
- **Tabla de iteraciones**: `i | x | y Euler | y RK4 | y exacta | error Euler | error RK4`.
- **Paso a paso**: muestra `k1, k2, k3, k4` y `y(i+1)` con números, como en tu cuaderno.
- **Resumen de errores**: error en x final, error máximo y cuántas veces es mejor RK4.
- **Deslizador de h**: cambia el paso en vivo y se ve cómo se mueve el error.
- **Ejemplos listos**: tu ejemplo del cuaderno, enfriamiento de Newton, población, logístico,
  paracaidista, circuito RC y uno oscilante donde Euler falla.
- Si no escribes la solución exacta, el error se mide contra RK4 con un paso 20 veces menor.

## Estructura del código (`src/edo/`)

| Archivo | Para qué sirve |
|---|---|
| `Metodos.java` | **El núcleo**: `euler()` y `rk4()` (tus fórmulas) y el texto del paso a paso. |
| `Expresion.java` | Convierte el texto de la ecuación en una función `f(x, y)`. |
| `Ejemplo.java` | Catálogo de problemas preparados. |
| `PanelGrafica.java` | Dibuja la gráfica con `Graphics2D`. |
| `VentanaPrincipal.java` | Interfaz Swing: lee datos, llama a los métodos y muestra resultados. |
| `Main.java` | Arranca el programa. |

## Guion corto para la exposición

1. **Problema:** una EDO no siempre se puede resolver con fórmula; se aproxima paso a paso con `h`.
2. **Euler:** `y(i+1) = y(i) + h·f(x(i), y(i))`. Una pendiente por paso. Mostrar `Metodos.euler()`.
3. **RK4:** promedia cuatro pendientes `k1..k4`. Mostrar `Metodos.rk4()` y la pestaña *Paso a paso*.
4. **Demo 1:** ejemplo del cuaderno (`y(0.1) = 0.909675`, `y(2) ≈ 1.2707`).
5. **Demo 2:** mover el deslizador de h. Al reducir h a la mitad, el error de Euler baja a la mitad
   y el de RK4 a 1/16.
6. **Demo 3:** ejemplo oscilante `y' = y·cos(x)`: Euler se aleja de la solución exacta.

## Ideas para ampliar

- Pasar a sistemas de ecuaciones (`y` como arreglo) para modelo SIR o tiro parabólico.
- Convertirlo en el juego del paracaidista usando `Metodos.rk4()` en cada fotograma.
