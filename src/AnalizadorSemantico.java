// AnalizadorSemantico.java - Verificación de tipos y variables declaradas
import java.util.*;

public class AnalizadorSemantico {
    private final TablaSimbolos tabla;
    private final List<String> errores = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();

    public AnalizadorSemantico(TablaSimbolos tabla) {
        this.tabla = tabla;
    }

    public void analizar(NodoAST nodo) {
        if (nodo == null) return;
        if      (nodo instanceof NodoBloque)        analizarBloque((NodoBloque) nodo);
        else if (nodo instanceof NodoDeclaracion)   analizarDeclaracion((NodoDeclaracion) nodo);
        else if (nodo instanceof NodoAsignacion)    analizarAsignacion((NodoAsignacion) nodo);
        else if (nodo instanceof NodoSi)            analizarSi((NodoSi) nodo);
        else if (nodo instanceof NodoMientras)      analizarMientras((NodoMientras) nodo);
        else if (nodo instanceof NodoHacerMientras) analizarHacerMientras((NodoHacerMientras) nodo);
        else if (nodo instanceof NodoPara)          analizarPara((NodoPara) nodo);
        else if (nodo instanceof NodoSegun)         analizarSegun((NodoSegun) nodo);
        else if (nodo instanceof NodoImprimir)      analizar(((NodoImprimir) nodo).expresion);
        else if (nodo instanceof NodoBinario)       analizarBinario((NodoBinario) nodo);
        else if (nodo instanceof NodoUnario)        analizar(((NodoUnario) nodo).operando);
        else if (nodo instanceof NodoID)            analizarID((NodoID) nodo);
    }

    private void analizarBloque(NodoBloque b) {
        for (NodoAST s : b.sentencias) analizar(s);
    }

    private void analizarDeclaracion(NodoDeclaracion d) {
        if (tabla.existe(d.nombre)) {
            errores.add("Error semántico línea " + d.linea + ": variable '" + d.nombre + "' ya fue declarada.");
        } else {
            tabla.declarar(d.nombre, d.tipo, "variable", d.linea);
        }
        if (d.inicializador != null) {
            String tipoExpr = inferirTipo(d.inicializador);
            if (!sonCompatibles(d.tipo, tipoExpr)) {
                errores.add("Error semántico línea " + d.linea + ": tipo incompatible. Variable '" + d.nombre + "' es '" + d.tipo + "' pero se asigna '" + tipoExpr + "'.");
            }
            analizar(d.inicializador);
        }
    }

    private void analizarAsignacion(NodoAsignacion a) {
        if (!tabla.existe(a.nombre)) {
            errores.add("Error semántico línea " + a.linea + ": variable '" + a.nombre + "' no fue declarada.");
        } else {
            TablaSimbolos.Simbolo s = tabla.obtener(a.nombre);
            String tipoExpr = inferirTipo(a.valor);
            if (!sonCompatibles(s.tipo, tipoExpr)) {
                errores.add("Error semántico línea " + a.linea + ": tipo incompatible. Variable '" + a.nombre + "' es '" + s.tipo + "' pero se asigna '" + tipoExpr + "'.");
            }
        }
        analizar(a.valor);
    }

    private void analizarSi(NodoSi s) {
        String tipoCond = inferirTipo(s.condicion);
        if (!tipoCond.equals("logico") && !tipoCond.equals("entero") && !tipoCond.equals("decimal")) {
            advertencias.add("Advertencia semántica línea " + s.linea + ": condición del 'si' podría no ser lógica.");
        }
        analizar(s.condicion);
        analizar(s.entonces);
        if (s.sino != null) analizar(s.sino);
    }

    private void analizarMientras(NodoMientras m) {
        analizar(m.condicion);
        analizar(m.cuerpo);
    }

    private void analizarHacerMientras(NodoHacerMientras h) {
        analizar(h.cuerpo);
        analizar(h.condicion);
    }

    private void analizarPara(NodoPara p) {
        analizar(p.inicio);
        analizar(p.condicion);
        analizar(p.incremento);
        analizar(p.cuerpo);
    }

    private void analizarSegun(NodoSegun s) {
        analizar(s.expresion);
        for (NodoCaso c : s.casos) {
            analizar(c.valor);
            analizar(c.cuerpo);
        }
        if (s.defecto != null) analizar(s.defecto);
    }

    private void analizarBinario(NodoBinario b) {
        analizar(b.izq);
        analizar(b.der);
        String tipoIzq = inferirTipo(b.izq);
        String tipoDer = inferirTipo(b.der);
        // División entre cero literal
        if (b.operador.equals("/") && b.der instanceof NodoNumero && ((NodoNumero) b.der).valor == 0) {
            errores.add("Error semántico línea " + b.linea + ": división por cero.");
        }
    }

    private void analizarID(NodoID id) {
        if (!tabla.existe(id.nombre)) {
            errores.add("Error semántico línea " + id.linea + ": variable '" + id.nombre + "' no fue declarada.");
        }
    }

    // Inferir tipo de una expresión (simplificado)
    public String inferirTipo(NodoAST nodo) {
        if (nodo instanceof NodoNumero) {
            double v = ((NodoNumero) nodo).valor;
            return (v == Math.floor(v)) ? "entero" : "decimal";
        }
        if (nodo instanceof NodoCadena)   return "cadena";
        if (nodo instanceof NodoBooleano) return "logico";
        if (nodo instanceof NodoID) {
            TablaSimbolos.Simbolo s = tabla.obtener(((NodoID) nodo).nombre);
            return s != null ? s.tipo : "desconocido";
        }
        if (nodo instanceof NodoBinario) {
            String op = ((NodoBinario) nodo).operador;
            if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">") ||
                op.equals("<=") || op.equals(">=") || op.equals("y") || op.equals("o")) return "logico";
            String tipoIzq = inferirTipo(((NodoBinario) nodo).izq);
            String tipoDer = inferirTipo(((NodoBinario) nodo).der);
            if (tipoIzq.equals("cadena") || tipoDer.equals("cadena")) return "cadena";
            if (tipoIzq.equals("decimal") || tipoDer.equals("decimal")) return "decimal";
            return "entero";
        }
        if (nodo instanceof NodoUnario) return inferirTipo(((NodoUnario) nodo).operando);
        return "desconocido";
    }

    private boolean sonCompatibles(String tipoVar, String tipoExpr) {
        if (tipoVar.equals(tipoExpr)) return true;
        if (tipoVar.equals("decimal") && tipoExpr.equals("entero")) return true;
        if (tipoVar.equals("desconocido") || tipoExpr.equals("desconocido")) return true;
        return false;
    }

    public List<String> getErrores()      { return errores; }
    public List<String> getAdvertencias() { return advertencias; }

    public void imprimirResultado() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    ANÁLISIS SEMÁNTICO                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        if (errores.isEmpty() && advertencias.isEmpty()) {
            System.out.println("  ✔ Sin errores semánticos.");
        } else {
            advertencias.forEach(a -> System.out.println("  ⚠ " + a));
            errores.forEach(e -> System.out.println("  ✘ " + e));
        }
    }
}
