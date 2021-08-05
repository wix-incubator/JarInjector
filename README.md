# JarInjector

JarInjector is a simple-to-use tool providing a way to modify a binary class files in the any existing jar file.

## What is the tool for
If you need to modify an existing jar without full recompiling, JarInjector can help you. Say, we have a huge library
like React Native, and want to change the behaviour of some specific RN class. Basically,
we could rebuild the total lib from its source, but it is a complicated
and time-loading task. You should also to take into account the maintenance price: you must 
fork the lib project, to merge your changes, and sync it constantly
with lib updates.

The better way is to modify the class / classes you need to,
and replace them in the lib jar. And again we could just get the original
source code of that class, modify it somehow, compile and replace it, but 
we should do it after every lib update.

The **JarInjector** gives you a possibility to define
only the difference you need, and inject it.

## How does it work
Let's look at a little example.
There is an *example.jar* containing a lot of classes,
one of them named *ClassA* in package *com.example.logic*.
```java
package com.example.logic;

// imports

final class ClassA {
    // class's code
    
    private void doSomething(String val1, int val2) {
        // method's code
    }
    
    // class's code
}
```

We want to change a behaviour of *doSomething* method, 
say modify val1 value before the original method's code run.

For that purpose, we'll create a new ClassA.java file:

```java
// the same package as an original class placed
package com.example.logic;

// A base class name should be Base__${originalClassName}
// An original class is declared as final, but it doesn't matter
class ClassA extends Base__ClassA {
    // The original method is private,
    // but we define ours to be package accessible
    @Override
    void doSomething(String val1, int val2) {
        String newVal1 = val1 != null ? val1.toUpperCase() : null;
        super.doSomething(newVal1, val2);
    }
}
```

Now we can run:
```
java -jar JarInjector.jar -jar ./example.jar -src ./ClassA.java
```
and the *example-new.jar* will be created.

## Usage

```
java -jar JarInjector.jar -jar JarFileToModify -src ListOfJavaClassesToInject -cw AdditionalJarsToCompileWith
```
