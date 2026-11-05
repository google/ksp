import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ExampleJavaTransitiveArrayDefaultReference {
    Class<?>[] value() default {TransitiveChanges.class};
}
