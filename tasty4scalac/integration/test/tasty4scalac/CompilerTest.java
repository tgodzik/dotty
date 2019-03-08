package tasty4scalac;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import scala.collection.immutable.Set;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public final class CompilerTest {
    private final Compiler compiler;

    public CompilerTest(Compiler factory) {
        this.compiler = factory;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Compiler[]> parameters() {
        return Arrays.asList(new Compiler[][]{
                {Scalac$.MODULE$.apply()},
                {Dotty$.MODULE$.apply()}
        });
    }

    @Test
    public void generatesTasty() {
        String code = "class A";
        Set<Tasty> tastySet = compiler.compile(code);
        Assert.assertFalse(tastySet.isEmpty());
    }
}
