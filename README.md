# Compiler of the Java-- language to Java Bytecodes

## GROUP: 1F

| Name             | Number    | E-Mail             | GRADE | CONTRIBUTION |
| ---------------- | --------- | ------------------ | ----- | ------------ |
| Beatriz Mendes    | 201806551 |up201806551@fe.up.pt| 20 | 25% |
| Henrique Pereira  | 201806538 |up201806538@fe.up.pt| 20 | 25% |
| Mariana Truta    | 201806543 |up201806543@fe.up.pt| 20 | 25% |
| Rita Peixoto    | 201806257 |up201806257@fe.up.pt| 20 | 25% |



GLOBAL Grade of the project: 18



**SUMMARY:**

The goal of this assignment is to acquire and apply knowledge within the Compilers’ course unit. To do so, we have implemented a compiler of Java-- language to Java bytecodes. This compiler includes syntactic and semantic analysis, as well as OLLIR and Jasmin code generation.


**COMPILE, EXECUTE AND TEST:**

The following command compiles our tool: ```gradle build```

To execute: ```.\comp2021-1f Main <filename.jmm> [-o]```, the -o flag must be specified in order to execute with the constant propagation optimization.

To test, run ```gradle test```.


**DEALING WITH SYNTACTIC ERRORS:**

We only recover from errors in the while loop. If an error is found in the while loop, the compiler ignores every token until the number of right braces matches the number of left braces.
With this, an error report with syntactic type is generated with the information of the line and column where the error is. The program only aborts after more than 10 errors occurred.


**SEMANTIC ANALYSIS:**


**Type Verification**

The following verifications are implemented:

* The operations are performed with the same type (e.g. int + boolean);

* Cannot use arrays directly for arithmetic operation (e.g. array1 + array2);

* An array access can only be made over an array (e.g. 1[10] is not valid);

* The index of the access array is an integer (e.g. a [true] is not allowed);

* Length method can only be used over arrays;

* The assignee's value is the same as the assigned's (e.g. a_int = b_boolean is not allowed);

* A boolean operation is performed only with booleans;

* Conditional expressions result in a boolean;

* Assumes Parameters as Initialized;

* New types allowed are from the class, superclass or imports.

**Method Verification**

The following verifications are implemented:

* The "target" of the method exists and contains the method (e.g. a.foo, see if 'a' exists and if it has a 'foo' method);

* The number of arguments in the invocation is equal to the number of parameters in the declaration;

* The type of the parameters matches the type of the arguments;

* The return type matches the method declaration return type;

* The method is either imported, belongs to the class or its superclass;

* When this invocation of the method is used, check if the method belongs to the class or there is a superclass;

* Allows overload of methods.



**CODE GENERATION:**

The OLLIR code is generated from .jmm files, creating the OLLIR code that is used afterwards to generate the Jasmin code.

The more efficient instructions are chosen in each situation, as described in the Pros of our project. The
limit of the stack and locals is calculated based on the instructions used in each method.

We implemented our code generation iteratively:

* Generating the class structure with constructor;
* Generating the method stubs with parameters and appropriate return types;
* Generating the method bodies (variable assignments, arithmetic operations and method invocations);
* Generating the fields of the class;
* Optimizing the instructions chosen for loading constants;
* Optimizing the instructions chosen for loading and storing variables;
* Generating incrementation of variables.

**TASK DISTRIBUTION:**

| Task             | Contributors    | 
| ---------------- | --------- |
| Syntactic Errors    | Beatriz Mendes, Henrique Pereira, Mariana Truta, Rita Peixoto |
| Semantic Analysis (Method Verification)  | Beatriz Mendes, Henrique Pereira |
| Semantic Analysis (Type Verification)    | Mariana Truta, Rita Peixoto |
| Code Generation (OLLIR)    | Mariana Truta, Rita Peixoto |
| Code Generation (Jasmin)    | Beatriz Mendes, Henrique Pereira |



**PROS:**

We have implemented some extra features, such as:

* Method overloading according to java method overloading;

* Verification of match of return type in the declaration and the actual method, verification of variable declaration more than once, verification of non-static variable 'identifier' cannot be referenced from a static context;

* Optimizations: eliminated the use of unnecessary “goto” instructions in the while loop, constant propagation, the use of lower-cost instructions.



**CONS:**

As the time was cut short, we weren't able to implement the register allocation optimization or any extra optimization besides the ones already mentioned.
