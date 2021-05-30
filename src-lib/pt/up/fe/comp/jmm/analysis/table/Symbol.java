package pt.up.fe.comp.jmm.analysis.table;

import pt.up.fe.comp.jmm.JmmNode;

public class Symbol {
    private final Type type;
    private final String name;

    public Symbol(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Symbol(JmmNode type, JmmNode name) {
        this.type = new Type(type);
        this.name = name.getKind().replaceAll("'", "").replace("Identifier ", "");
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Symbol [type=" + type + ", name=" + name + "]";
    }

    public String toOLLIR() {
        return name + type.toOLLIR();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Symbol other = (Symbol) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            return other.type == null;
        } else return type.equals(other.type);
    }

}
