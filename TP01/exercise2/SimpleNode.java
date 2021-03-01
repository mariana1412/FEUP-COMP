//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

public class SimpleNode implements Node {
  protected Node parent;
  protected Node[] children;
  protected int id;
  protected Object value;
  protected Calculator parser;

  public int val;
  public int Op;

  public SimpleNode(int var1) {
    this.Op = 0;
    this.id = var1;
  }

  public SimpleNode(Calculator var1, int var2) {
    this(var2);
    this.parser = var1;
  }

  public void jjtOpen() {
  }

  public void jjtClose() {
  }

  public void jjtSetParent(Node var1) {
    this.parent = var1;
  }

  public Node jjtGetParent() {
    return this.parent;
  }

  public void jjtAddChild(Node var1, int var2) {
    if (this.children == null) {
      this.children = new Node[var2 + 1];
    } else if (var2 >= this.children.length) {
      Node[] var3 = new Node[var2 + 1];
      System.arraycopy(this.children, 0, var3, 0, this.children.length);
      this.children = var3;
    }

    this.children[var2] = var1;
  }

  public Node jjtGetChild(int var1) {
    return this.children[var1];
  }

  public int jjtGetNumChildren() {
    return this.children == null ? 0 : this.children.length;
  }

  public void jjtSetValue(Object var1) {
    this.value = var1;
  }

  public Object jjtGetValue() {
    return this.value;
  }

  public String toString() {
    return CalculatorTreeConstants.jjtNodeName[this.id];
  }

  public String toString(String var1) {
    return var1 + this.toString();
  }

  public void dump(String var1) {
    System.out.println(this.toString(var1));
    switch(this.id) {
      case CalculatorTreeConstants.JJTADD:
        System.out.println("\t[ + ]");
        break;
      case CalculatorTreeConstants.JJTSUB:
        System.out.println("\t[ - ]");
        break;
      case CalculatorTreeConstants.JJTMUL:
        System.out.println("\t[ * ]");
        break;
      case CalculatorTreeConstants.JJTDIV:
        System.out.println("\t[ / ]");
    }

    if (this.children != null) {
      for(int var2 = 0; var2 < this.children.length; ++var2) {
        SimpleNode var3 = (SimpleNode)this.children[var2];
        if (var3 != null) {
          var3.dump(var1 + " ");
        }
      }
    }

  }

  public int getId() {
    return this.id;
  }
}
