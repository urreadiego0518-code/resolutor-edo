package edo;

// Aquí guardamos los problemas listos para probar rápido
public class Ejemplo {

    public final String nombre;
    public final String f;
    public final String exacta;
    public final double x0, y0, h, xf;

    public Ejemplo(String nombre, String f, String exacta,
                   double x0, double y0, double h, double xf) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El ejemplo necesita un nombre.");
        }
        if (!Metodos.finito(x0) || !Metodos.finito(y0) || !Metodos.finito(xf)) {
            throw new IllegalArgumentException("Datos no finitos en el ejemplo " + nombre);
        }
        if (!Metodos.finito(h) || h <= 0) {
            throw new IllegalArgumentException("h inválida en el ejemplo " + nombre);
        }
        if (!(xf > x0)) {
            throw new IllegalArgumentException("El intervalo del ejemplo " + nombre + " es inválido");
        }
        this.nombre = nombre;
        this.f = f;
        this.exacta = exacta;
        this.x0 = x0;
        this.y0 = y0;
        this.h = h;
        this.xf = xf;
    }

    // Catálogo de ejemplos que salen en el combo
    public static Ejemplo[] catalogo() {
        return new Ejemplo[]{
            new Ejemplo("Ejemplo del cuaderno:  y' + y = x",
                    "x - y", "x - 1 + 2*exp(-x)",
                    0, 1, 0.1, 2),
            new Ejemplo("Enfriamiento de Newton (taza de café)",
                    "-0.5*(y - 20)", "20 + 70*exp(-0.5*x)",
                    0, 90, 0.5, 10),
            new Ejemplo("Crecimiento de población",
                    "0.3*y", "100*exp(0.3*x)",
                    0, 100, 0.5, 10),
            new Ejemplo("Crecimiento logístico (límite 100)",
                    "0.5*y*(1 - y/100)", "100/(1 + 9*exp(-0.5*x))",
                    0, 10, 1, 15),
            new Ejemplo("Paracaidista: velocidad con resistencia del aire",
                    "9.81 - 0.25*y", "39.24*(1 - exp(-0.25*x))",
                    0, 0, 1, 20),
            new Ejemplo("Carga de un capacitor (circuito RC)",
                    "(12 - y)/2", "12*(1 - exp(-x/2))",
                    0, 0, 1, 10),
            new Ejemplo("Oscilante:  y' = y·cos(x)  (Euler falla)",
                    "y*cos(x)", "exp(sin(x))",
                    0, 1, 0.5, 10),
            new Ejemplo("Personalizada (escribe tu ecuación)",
                    null, "", 0, 1, 0.1, 2)
        };
    }

    @Override
    public String toString() {
        return nombre;
    }
}