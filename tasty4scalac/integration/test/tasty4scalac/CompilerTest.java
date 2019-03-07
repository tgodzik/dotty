package tasty4scalac;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
    public void compilesSimpleClass() {
        String code = "class A";
        compiler.compile(code);
    }
}
