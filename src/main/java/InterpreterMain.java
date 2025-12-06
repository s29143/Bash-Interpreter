import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;

public class InterpreterMain {

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("> ");
            System.out.flush();

            String line = br.readLine();
            if (line == null) break;

            StringReader sr = new StringReader(line + "\n");
            BashLexer lexer = new BashLexer(sr);
            BashParser parser = new BashParser(lexer);

            parser.parse();
        }
    }
}
