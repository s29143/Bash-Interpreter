import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CommandExecutor {

    private static int lastExitCode = 0;

    public static int getLastExitCode() {
        return lastExitCode;
    }

    public static void execute(String cmd, List<String> args) {
        execute(cmd, args, null);
    }

    public static void execute(String cmd, List<String> args, RedirectInfo redirect) {
        List<String> expandedArgs = expandSpecialArgs(args);

        int code;
        if (redirect == null) {
            code = runCommand(cmd, expandedArgs, System.out);
            lastExitCode = code;
            return;
        }

        boolean append = redirect.append();
        Path outDir = Paths.get("out");
        Path file = outDir.resolve(redirect.filename());

        try {
            Files.createDirectories(outDir);
            try (PrintStream out = new PrintStream(
                    new FileOutputStream(file.toFile(), append),
                    true,
                    StandardCharsets.UTF_8)) {
                code = runCommand(cmd, expandedArgs, out);
            }
        } catch (IOException e) {
            System.err.println("redirect error: " + e.getMessage());
            code = 1;
        }

        lastExitCode = code;
    }

    private static List<String> expandSpecialArgs(List<String> args) {
        List<String> out = new ArrayList<>(args.size());
        for (String a : args) {
            if ("$?".equals(a)) out.add(String.valueOf(lastExitCode));
            else out.add(a);
        }
        return out;
    }

    private static int runCommand(String cmd, List<String> args, PrintStream out) {
        return switch (cmd) {
            case "echo" -> echo(args, out);
            case "ls" -> ls(args, out);
            case "cat" -> cat(args, out);
            case "wc" -> wc(args, out);
            default -> {
                System.err.println("Unknown command: " + cmd);
                yield 1;
            }
        };
    }

    private static int echo(List<String> args, PrintStream out) {
        out.println(String.join(" ", args));
        return 0;
    }

    private static int ls(List<String> args, PrintStream out) {
        Path dir = args.isEmpty() ? Paths.get(".") : Paths.get(args.get(0));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) out.println(p.getFileName().toString());
            return 0;
        } catch (IOException e) {
            System.err.println("ls: " + e.getMessage());
            return 1;
        }
    }

    private static int cat(List<String> args, PrintStream out) {
        if (args.isEmpty()) {
            System.err.println("Usage: cat [filename]");
            return 1;
        }

        int code = 0;
        for (String arg : args) {
            Path p = Paths.get(arg);
            if (!Files.exists(p)) {
                System.err.println("No file: " + arg);
                code = 1;
                continue;
            }
            try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) out.println(line);
            } catch (IOException e) {
                System.err.println("No file: " + arg);
                code = 1;
            }
        }
        return code;
    }

    private static int wc(List<String> args, PrintStream out) {
        if (args.isEmpty()) {
            System.err.println("Usage: wc [filename]");
            return 1;
        }

        int code = 0;
        for (String arg : args) {
            Path p = Paths.get(arg);
            if (!Files.exists(p)) {
                System.err.println("No file: " + arg);
                code = 1;
                continue;
            }
            try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                out.println(br.lines().count());
            } catch (IOException e) {
                System.err.println("No file: " + arg);
                code = 1;
            }
        }
        return code;
    }
}
