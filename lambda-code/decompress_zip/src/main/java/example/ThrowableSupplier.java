package example;

@FunctionalInterface
public interface ThrowableSupplier<T, E extends Throwable> {
    T get() throws E;
}
