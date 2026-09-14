package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.introspector;

import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.YAMLException;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.util.ArrayUtils;

public class MethodProperty extends GenericProperty {
   private final PropertyDescriptor property;
   private final boolean readable;
   private final boolean writable;
   private static final String TRANSIENT = "transient";

   private static Type discoverGenericType(PropertyDescriptor property) {
      Method readMethod = property.getReadMethod();
      if (readMethod != null) {
         return readMethod.getGenericReturnType();
      } else {
         Method writeMethod = property.getWriteMethod();
         if (writeMethod != null) {
            Type[] paramTypes = writeMethod.getGenericParameterTypes();
            if (paramTypes.length > 0) {
               return paramTypes[0];
            }
         }

         return null;
      }
   }

   public MethodProperty(PropertyDescriptor property) {
      super(property.getName(), property.getPropertyType(), discoverGenericType(property));
      this.property = property;
      this.readable = property.getReadMethod() != null;
      this.writable = property.getWriteMethod() != null;
   }

   @Override
   public void set(Object object, Object value) throws Exception {
      if (!this.writable) {
         throw new YAMLException("No writable property '" + this.getName() + "' on class: " + object.getClass().getName());
      } else {
         this.property.getWriteMethod().invoke(object, value);
      }
   }

   @Override
   public Object get(Object object) {
      try {
         this.property.getReadMethod().setAccessible(true);
         return this.property.getReadMethod().invoke(object);
      } catch (Exception var3) {
         throw new YAMLException("Unable to find getter for property '" + this.property.getName() + "' on object " + object + ":" + var3);
      }
   }

   @Override
   public List<Annotation> getAnnotations() {
      List<Annotation> annotations;
      if (this.isReadable() && this.isWritable()) {
         annotations = ArrayUtils.toUnmodifiableCompositeList(this.property.getReadMethod().getAnnotations(), this.property.getWriteMethod().getAnnotations());
      } else if (this.isReadable()) {
         annotations = ArrayUtils.toUnmodifiableList(this.property.getReadMethod().getAnnotations());
      } else {
         annotations = ArrayUtils.toUnmodifiableList(this.property.getWriteMethod().getAnnotations());
      }

      return annotations;
   }

   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      A annotation = null;
      if (this.isReadable()) {
         annotation = this.property.getReadMethod().getAnnotation(annotationType);
      }

      if (annotation == null && this.isWritable()) {
         annotation = this.property.getWriteMethod().getAnnotation(annotationType);
      }

      return annotation;
   }

   @Override
   public boolean isWritable() {
      return this.writable;
   }

   @Override
   public boolean isReadable() {
      return this.readable;
   }

   private static boolean isTransient(FeatureDescriptor fd) {
      return Boolean.TRUE.equals(fd.getValue("transient"));
   }

   static boolean addPublicFields(Class<?> type, Map<String, Property> properties) {
      boolean inaccessableFieldsExist = false;

      try {
         for (PropertyDescriptor property : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            Method readMethod = property.getReadMethod();
            if ((readMethod == null || !readMethod.getName().equals("getClass")) && !isTransient(property)) {
               properties.put(property.getName(), new MethodProperty(property));
            }
         }
      } catch (IntrospectionException var9) {
         throw new YAMLException(var9);
      }

      for (Class<?> c = type; c != null; c = c.getSuperclass()) {
         for (Field field : c.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
               if (Modifier.isPublic(modifiers)) {
                  properties.put(field.getName(), new FieldProperty(field));
               } else {
                  inaccessableFieldsExist = true;
               }
            }
         }
      }

      return inaccessableFieldsExist;
   }
}
