package net.iernst.base64;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Base64 {
  private interface Encoder {
    String encodeToString(byte[] bytes); 
  }
  
  private static final Encoder encoder;
  static {
    try {
      Encoder theEncoder = getJava8Encoder();
      if (theEncoder == null) {
        theEncoder = getJaxbEncoder();
      }
      encoder = theEncoder;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static Encoder getJava8Encoder() throws ReflectiveOperationException { 
    final Class base64Clazz;
    try {
      base64Clazz = Class.forName("java.util.Base64");
    } catch (ClassNotFoundException e) {
      return null; // not java 8+
    }
    final Method encoderGetter = base64Clazz.getMethod("getEncoder");
    final Object encoder = encoderGetter.invoke(base64Clazz); 
    final Class encoderClazz = Class.forName("java.util.Base64$Encoder");
    final Method encodeMethod = encoderClazz.getMethod("encodeToString", byte[].class);
    return new Encoder() {
      public String encodeToString(byte[] bytes) {
        try {
          return (String)encodeMethod.invoke(encoder, bytes);
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private static Encoder getJaxbEncoder() throws ReflectiveOperationException {
    final Class jaxbClazz = Class.forName("javax.xml.bind.DatatypeConverter");
    final Method encodeMethod = jaxbClazz.getMethod("printBase64Binary", byte[].class);
    return new Encoder() {
      public String encodeToString(byte[] bytes) {
        try {
          return (String)encodeMethod.invoke(jaxbClazz, bytes);
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public static String encodeToString(byte[] bytes) {
    return encoder.encodeToString(bytes);
  }

  public static void main(String[] args) {
    check("", "");
    check("f", "Zg==");
    check("fo", "Zm8=");
    check("foo", "Zm9v");
    check("foob", "Zm9vYg==");
  }
  private static void check(String input, String expected) {
    String output = encodeToString(input.getBytes());
    if (output.equals(expected) == false) {
      throw new RuntimeException("FAILED: input=" + input + ", expected=" + expected + ", got=" + output);
    }
  }
}
