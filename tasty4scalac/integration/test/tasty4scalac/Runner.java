package tasty4scalac;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

@RunWith(Parameterized.class)
public final class Runner {
    private static final Compiler compiler = Compiler$.MODULE$.scalac();
    private final CompileSource test;

    public Runner(CompileSource test) {
        this.test = test;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<CompileSource[]> parameters() {
        return Providers.compiledSourceProvider().testCases();
    }

    @Test
    public void compile() throws IOException {
        String code = new String(Files.readAllBytes(test.source()));
        compiler.compile(code);
    }
}

