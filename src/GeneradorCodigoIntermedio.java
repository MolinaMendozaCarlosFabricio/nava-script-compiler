// GeneradorCodigoIntermedio.java - Genera código de tres direcciones
import java.util.*;

public class GeneradorCodigoIntermedio {
    private int tempCount = 0;
    private int etiqCount = 0;
    private final List<String> instrucciones = new ArrayList<>();

    private String nuevoTemp() { return "t" + (++tempCount); }
    private String nuevaEtiq() { return "L" + (++etiqCount); }

    public void generar(NodoAST nodo) {
        if (nodo instanceof NodoBloque)
            for (NodoAST s : ((NodoBloque) nodo).sentencias) generar(s);
        else if (nodo instanceof NodoDeclaracion)   generarDeclaracion((NodoDeclaracion) nodo);
        else if (nodo instanceof NodoAsignacion)    generarAsignacion((NodoAsignacion) nodo);
        else if (nodo instanceof NodoImprimir)      generarImprimir((NodoImprimir) nodo);
        else if (nodo instanceof NodoLeer)          generarLeer((NodoLeer) nodo);
        else if (nodo instanceof NodoSi)            generarSi((NodoSi) nodo);
        else if (nodo instanceof NodoMientras)      generarMientras((NodoMientras) nodo);
        else if (nodo instanceof NodoHacerMientras) generarHacerMientras((NodoHacerMientras) nodo);
        else if (nodo instanceof NodoPara)          generarPara((NodoPara) nodo);
        else if (nodo instanceof NodoSegun)         generarSegun((NodoSegun) nodo);
        else if (nodo instanceof NodoRomper)        instrucciones.add("GOTO __salida__");
    }

    private void generarDeclaracion(NodoDeclaracion d) {
        instrucciones.add("DECL " + d.tipo + " " + d.nombre);
        if (d.inicializador != null) {
            String val = generarExpr(d.inicializador);
            instrucciones.add(d.nombre + " = " + val);
        }
    }

    private void generarAsignacion(NodoAsignacion a) {
        String val = generarExpr(a.valor);
        instrucciones.add(a.nombre + " = " + val);
    }

    private void generarImprimir(NodoImprimir p) {
        String val = generarExpr(p.expresion);
        instrucciones.add("PRINT " + val);
    }

    private void generarLeer(NodoLeer l) {
        instrucciones.add("READ " + l.variable);
    }

    private void generarSi(NodoSi si) {
        String cond = generarExpr(si.condicion);
        String etiqElse = nuevaEtiq();
        String etiqFin  = nuevaEtiq();
        instrucciones.add("IFFALSE " + cond + " GOTO " + etiqElse);
        generar(si.entonces);
        if (si.sino != null) instrucciones.add("GOTO " + etiqFin);
        instrucciones.add(etiqElse + ":");
        if (si.sino != null) {
            generar(si.sino);
            instrucciones.add(etiqFin + ":");
        }
    }

    private void generarMientras(NodoMientras m) {
        String etiqIni = nuevaEtiq();
        String etiqFin = nuevaEtiq();
        instrucciones.add(etiqIni + ":");
        String cond = generarExpr(m.condicion);
        instrucciones.add("IFFALSE " + cond + " GOTO " + etiqFin);
        generar(m.cuerpo);
        instrucciones.add("GOTO " + etiqIni);
        instrucciones.add(etiqFin + ":");
    }

    private void generarHacerMientras(NodoHacerMientras h) {
        String etiqIni = nuevaEtiq();
        instrucciones.add(etiqIni + ":");
        generar(h.cuerpo);
        String cond = generarExpr(h.condicion);
        instrucciones.add("IFTRUE " + cond + " GOTO " + etiqIni);
    }

    private void generarPara(NodoPara p) {
        generar(p.inicio);
        String etiqIni = nuevaEtiq();
        String etiqFin = nuevaEtiq();
        instrucciones.add(etiqIni + ":");
        String cond = generarExpr(p.condicion);
        instrucciones.add("IFFALSE " + cond + " GOTO " + etiqFin);
        generar(p.cuerpo);
        generar(p.incremento);
        instrucciones.add("GOTO " + etiqIni);
        instrucciones.add(etiqFin + ":");
    }

    private void generarSegun(NodoSegun s) {
        String val = generarExpr(s.expresion);
        String etiqFin = nuevaEtiq();
        List<String> etiqCasos = new ArrayList<>();
        for (NodoCaso c : s.casos) {
            String ec = nuevaEtiq();
            etiqCasos.add(ec);
            String cVal = generarExpr(c.valor);
            String cond = nuevoTemp();
            instrucciones.add(cond + " = " + val + " == " + cVal);
            instrucciones.add("IFTRUE " + cond + " GOTO " + ec);
        }
        String etiqDef = nuevaEtiq();
        if (s.defecto != null) instrucciones.add("GOTO " + etiqDef);
        else instrucciones.add("GOTO " + etiqFin);

        for (int i = 0; i < s.casos.size(); i++) {
            instrucciones.add(etiqCasos.get(i) + ":");
            generar(s.casos.get(i).cuerpo);
            instrucciones.add("GOTO " + etiqFin);
        }
        if (s.defecto != null) {
            instrucciones.add(etiqDef + ":");
            generar(s.defecto);
        }
        instrucciones.add(etiqFin + ":");
    }

    // Genera código para expresiones y retorna el lugar (temp o literal)
    public String generarExpr(NodoAST nodo) {
        if (nodo instanceof NodoNumero)  return String.valueOf(((NodoNumero) nodo).valor).replaceAll("\\.0$", "");
        if (nodo instanceof NodoCadena)  return "\"" + ((NodoCadena) nodo).valor + "\"";
        if (nodo instanceof NodoBooleano) return String.valueOf(((NodoBooleano) nodo).valor);
        if (nodo instanceof NodoID)      return ((NodoID) nodo).nombre;

        if (nodo instanceof NodoBinario) {
            NodoBinario b = (NodoBinario) nodo;
            String izq = generarExpr(b.izq);
            String der = generarExpr(b.der);
            String temp = nuevoTemp();
            instrucciones.add(temp + " = " + izq + " " + b.operador + " " + der);
            return temp;
        }
        if (nodo instanceof NodoUnario) {
            NodoUnario u = (NodoUnario) nodo;
            String op = generarExpr(u.operando);
            String temp = nuevoTemp();
            instrucciones.add(temp + " = " + u.operador + op);
            return temp;
        }
        return "?";
    }

    public List<String> getInstrucciones() { return instrucciones; }

    public void imprimir() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                  CÓDIGO INTERMEDIO (3 DIRECCIONES)              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        for (int i = 0; i < instrucciones.size(); i++) {
            System.out.printf("  %3d │ %s%n", i + 1, instrucciones.get(i));
        }
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }
}
