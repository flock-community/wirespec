package community.flock.wirespec.scala;

import scala.Function1;
import scala.Option;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.reflect.ClassTag;

/**
 * Minimal hand-rolled stand-in for the Scala-emitted `Wirespec.scala` that
 * `--emit-shared` would otherwise place on the user's classpath. Exists only
 * in the kotest module's jvmTest source set so the Scala adapter's reflective
 * decoding can be exercised without applying the Gradle Scala plugin.
 *
 * Components use Scala-stdlib types so reflective accessor lookups in
 * `ScalaInterop` find the same return types they'd find at user runtime.
 */
public final class Wirespec {

    private Wirespec() {}

    public sealed interface GeneratorField<T>
        permits GeneratorFieldString, GeneratorFieldInteger, GeneratorFieldNumber,
                GeneratorFieldBoolean, GeneratorFieldBytes, GeneratorFieldEnum,
                GeneratorFieldUnion, GeneratorFieldArray, GeneratorFieldNullable,
                GeneratorFieldShape, GeneratorFieldDict {}

    public record GeneratorFieldString(
        Option<String> regex,
        List<Map<String, Object>> annotations
    ) implements GeneratorField<String> {}

    public record GeneratorFieldInteger(
        Option<Long> min,
        Option<Long> max,
        List<Map<String, Object>> annotations
    ) implements GeneratorField<Long> {}

    public record GeneratorFieldNumber(
        Option<Double> min,
        Option<Double> max,
        List<Map<String, Object>> annotations
    ) implements GeneratorField<Double> {}

    public record GeneratorFieldBoolean(
        List<Map<String, Object>> annotations
    ) implements GeneratorField<Boolean> {}

    public record GeneratorFieldBytes(
        List<Map<String, Object>> annotations
    ) implements GeneratorField<byte[]> {}

    public record GeneratorFieldEnum(
        List<String> values,
        List<Map<String, Object>> annotations,
        ClassTag<?> type
    ) implements GeneratorField<String> {}

    public record GeneratorFieldUnion(
        List<String> variants,
        List<Map<String, Object>> annotations,
        ClassTag<?> type
    ) implements GeneratorField<String> {}

    public record GeneratorFieldArray<T>(
        Function1<List<String>, T> generate
    ) implements GeneratorField<List<T>> {}

    public record GeneratorFieldNullable<T>(
        Function1<List<String>, T> generate
    ) implements GeneratorField<Option<T>> {}

    public record GeneratorFieldShape<T>(
        Map<String, List<Map<String, Object>>> annotations,
        Function1<List<String>, T> generate,
        ClassTag<?> type
    ) implements GeneratorField<T> {}

    public record GeneratorFieldDict<V>(
        Function1<List<String>, V> generate
    ) implements GeneratorField<Map<String, V>> {}

    public interface Generator {
        <T> T generate(List<String> path, GeneratorField<T> field);
    }
}
