// Token.java - Representa un componente léxico
public class Token {
    public final String tipo;
    public final String lexema;
    public final int linea;

    public Token(String tipo, String lexema, int linea) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
    }

    @Override
    public String toString() {
        return String.format("%-25s | %-20s | Línea %d", tipo, lexema, linea);
    }
}
