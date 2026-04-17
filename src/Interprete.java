// Interprete.java - Ejecuta el AST directamente (código objeto interpretado)
import java.util.*;

public class Interprete {
    private final TablaSimbolos tabla;
    private final List<String> errores = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);
    private boolean romper = false;

    public Interprete(TablaSimbolos tabla) {
        this.tabla = tabla;
    }

    public void ejecutar(NodoAST nodo) {
        if (nodo == null) return;
        if (romper) return;

        if      (nodo instanceof NodoBloque)        ejecutarBloque((NodoBloque) nodo);
        else if (nodo instanceof NodoDeclaracion)   ejecutarDeclaracion((NodoDeclaracion) nodo);
        else if (nodo instanceof NodoAsignacion)    ejecutarAsignacion((NodoAsignacion) nodo);
        else if (nodo instanceof NodoImprimir)      ejecutarImprimir((NodoImprimir) nodo);
        else if (nodo instanceof NodoLeer)          ejecutarLeer((NodoLeer) nodo);
        else if (nodo instanceof NodoSi)            ejecutarSi((NodoSi) nodo);
        else if (nodo instanceof NodoMientras)      ejecutarMientras((NodoMientras) nodo);
        else if (nodo instanceof NodoHacerMientras) ejecutarHacerMientras((NodoHacerMientras) nodo);
        else if (nodo instanceof NodoPara)          ejecutarPara((NodoPara) nodo);
        else if (nodo instanceof NodoSegun)         ejecutarSegun((NodoSegun) nodo);
        else if (nodo instanceof NodoRomper)        romper = true;
    }

    private void ejecutarBloque(NodoBloque b) {
        for (NodoAST s : b.sentencias) {
            if (romper) break;
            ejecutar(s);
        }
    }

    private void ejecutarDeclaracion(NodoDeclaracion d) {
        if (!tabla.existe(d.nombre)) tabla.declarar(d.nombre, d.tipo, "variable", d.linea);
        if (d.inicializador != null) {
            Object val = evaluar(d.inicializador);
            tabla.setValor(d.nombre, val);
        }
    }

    private void ejecutarAsignacion(NodoAsignacion a) {
        Object val = evaluar(a.valor);
        tabla.setValor(a.nombre, val);
    }

    private void ejecutarImprimir(NodoImprimir p) {
        Object val = evaluar(p.expresion);
        System.out.println("  >> " + formatear(val));
    }

    private void ejecutarLeer(NodoLeer l) {
        TablaSimbolos.Simbolo s = tabla.obtener(l.variable);
        System.out.print("  Ingrese valor para '" + l.variable + "': ");
        String entrada = scanner.nextLine().trim();
        Object valor;
        if (s != null && s.tipo != null) {
            switch (s.tipo) {
                case "entero":
                    try { valor = (double) Long.parseLong(entrada); }
                    catch (NumberFormatException e) { errores.add("Error de ejecución: se esperaba entero para '" + l.variable + "'"); valor = 0.0; }
                    break;
                case "decimal":
                    try { valor = Double.parseDouble(entrada); }
                    catch (NumberFormatException e) { errores.add("Error de ejecución: se esperaba decimal para '" + l.variable + "'"); valor = 0.0; }
                    break;
                case "logico":
                    valor = entrada.equalsIgnoreCase("verdadero") || entrada.equals("1") || entrada.equalsIgnoreCase("true");
                    break;
                default:
                    valor = entrada;
            }
        } else {
            valor = entrada;
        }
        tabla.setValor(l.variable, valor);
    }

    private void ejecutarSi(NodoSi si) {
        Object cond = evaluar(si.condicion);
        if (esCierto(cond)) ejecutar(si.entonces);
        else if (si.sino != null) ejecutar(si.sino);
    }

    private void ejecutarMientras(NodoMientras m) {
        while (esCierto(evaluar(m.condicion))) {
            romper = false;
            ejecutar(m.cuerpo);
            if (romper) break;
        }
        romper = false;
    }

    private void ejecutarHacerMientras(NodoHacerMientras h) {
        do {
            romper = false;
            ejecutar(h.cuerpo);
            if (romper) break;
        } while (esCierto(evaluar(h.condicion)));
        romper = false;
    }

    private void ejecutarPara(NodoPara p) {
        ejecutar(p.inicio);
        while (esCierto(evaluar(p.condicion))) {
            romper = false;
            ejecutar(p.cuerpo);
            if (romper) break;
            ejecutar(p.incremento);
        }
        romper = false;
    }

    private void ejecutarSegun(NodoSegun s) {
        Object val = evaluar(s.expresion);
        boolean encontrado = false;
        for (NodoCaso c : s.casos) {
            Object cVal = evaluar(c.valor);
            if (iguales(val, cVal)) {
                encontrado = true;
                romper = false;
                ejecutar(c.cuerpo);
                romper = false;
                break;
            }
        }
        if (!encontrado && s.defecto != null) {
            romper = false;
            ejecutar(s.defecto);
            romper = false;
        }
    }

    // ─── Evaluación de expresiones ────────────────────────────────────────────

    public Object evaluar(NodoAST nodo) {
        if (nodo instanceof NodoNumero)   return ((NodoNumero) nodo).valor;
        if (nodo instanceof NodoCadena)   return ((NodoCadena) nodo).valor;
        if (nodo instanceof NodoBooleano) return ((NodoBooleano) nodo).valor;
        if (nodo instanceof NodoID) {
            TablaSimbolos.Simbolo s = tabla.obtener(((NodoID) nodo).nombre);
            return s != null && s.valor != null ? s.valor : 0.0;
        }
        if (nodo instanceof NodoBinario) return evaluarBinario((NodoBinario) nodo);
        if (nodo instanceof NodoUnario)  return evaluarUnario((NodoUnario) nodo);
        return 0.0;
    }

    private Object evaluarBinario(NodoBinario b) {
        Object izq = evaluar(b.izq);
        Object der = evaluar(b.der);
        switch (b.operador) {
            case "+":
                if (izq instanceof String || der instanceof String)
                    return formatear(izq) + formatear(der);
                return toDouble(izq) + toDouble(der);
            case "-": return toDouble(izq) - toDouble(der);
            case "*": return toDouble(izq) * toDouble(der);
            case "/":
                if (toDouble(der) == 0) { errores.add("Error: división por cero"); return 0.0; }
                return toDouble(izq) / toDouble(der);
            case "%": return toDouble(izq) % toDouble(der);
            case "==": return iguales(izq, der);
            case "!=": return !iguales(izq, der);
            case "<":  return toDouble(izq) < toDouble(der);
            case ">":  return toDouble(izq) > toDouble(der);
            case "<=": return toDouble(izq) <= toDouble(der);
            case ">=": return toDouble(izq) >= toDouble(der);
            case "y":  return esCierto(izq) && esCierto(der);
            case "o":  return esCierto(izq) || esCierto(der);
        }
        return 0.0;
    }

    private Object evaluarUnario(NodoUnario u) {
        Object val = evaluar(u.operando);
        if (u.operador.equals("-")) return -toDouble(val);
        if (u.operador.equals("no")) return !esCierto(val);
        return val;
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private double toDouble(Object v) {
        if (v instanceof Double)  return (Double) v;
        if (v instanceof Boolean) return ((Boolean) v) ? 1.0 : 0.0;
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    private boolean esCierto(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Double)  return (Double) v != 0;
        if (v instanceof String)  return !((String) v).isEmpty();
        return false;
    }

    private boolean iguales(Object a, Object b) {
        if (a instanceof Double && b instanceof Double) return a.equals(b);
        return formatear(a).equals(formatear(b));
    }

    private String formatear(Object v) {
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        return v != null ? v.toString() : "nulo";
    }

    public List<String> getErrores() { return errores; }
}
