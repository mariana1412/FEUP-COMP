/* Generated By:JJTree: Do not edit this line. SimpleNode.java Version 6.1 */
/* JavaCCOptions:MULTI=false,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */

import pt.up.fe.comp.jmm.JmmNode;

import java.lang.RuntimeException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public
class SimpleNode implements Node, JmmNode {

    // added
    public int val = -1;
    public String var = "";
    public Operator op = null;
    public int line;
    public int column;

    private HashMap<String,String> attributes = new HashMap<>();

    protected Node parent;
    protected Node[] children;
    protected int id;
    protected Object value;
    protected Grammar parser;

    public SimpleNode(int i) {
        id = i;
    }

    public SimpleNode(Grammar p, int i) {
        this(i);
        parser = p;
    }

    public String getKind() {
        return toString();
    }

    public List<String> getAttributes() {
        List<String> names = new ArrayList<>();
        for ( String name : attributes.keySet() ) {
            names.add(name);
        }
        return names;
    }

    public void put(String attribute, String value) {
        attributes.put(attribute, value);
    }

    public String get(String attribute) {
        return attributes.get(attribute);
    }

    public List<JmmNode> getChildren() {
        return JmmNode.convertChildren(children);
    }

    public int getNumChildren() {
        return jjtGetNumChildren();
    }

    public void add(JmmNode child, int index) {
        if (!(child instanceof Node)) {
            throw new RuntimeException("Node not supported: " + child.getClass());
        }

        jjtAddChild((Node) child, index);
    }


    public void jjtOpen() {
    }

    public void jjtClose() {
    }

    public void jjtSetParent(Node n) {
        parent = n;
    }

    public Node jjtGetParent() {
        return parent;
    }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new Node[i + 1];
        } else if (i >= children.length) {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    public void jjtSetValue(Object value) {
        this.value = value;
    }

    public Object jjtGetValue() {
        return value;
    }

  /* You can override these two methods in subclasses of SimpleNode to
     customize the way the node appears when the tree is dumped.  If
     your output uses more than one line you should override
     toString(String), otherwise overriding toString() is probably all
     you need to do. */

    public String toString() {
        if (val != -1) return GrammarTreeConstants.jjtNodeName[id] + " '" + val + "'";
        else if (var != "") return GrammarTreeConstants.jjtNodeName[id] + " '" + var + "'";

        return GrammarTreeConstants.jjtNodeName[id];
    }

    public String toString(String prefix) {
        return prefix + toString();
    }

    /* Override this method if you want to customize how the node dumps
       out its children. */
    public void dump(String prefix) {
        if (children == null) {
            System.out.println(toString(prefix));
        }

        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                SimpleNode n = (SimpleNode) children[i];
                if (n != null) {
                    n.dump(prefix + " ");
                }

            }
        }
    }

    public int getId() {
        return id;
    }
}

/* JavaCC - OriginalChecksum=d33fdb2b8063d5de3474649324d5d160 (do not edit this line) */
