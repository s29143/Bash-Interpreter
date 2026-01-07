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

    public record SimpleCommand(String cmd, List<String> args) {}

    private record ExecResult(int code, String out) {}

    public static void execute(String cmd, List<String> args) {
        execute(cmd, args, null);
    }

    public static void execute(String cmd, List<String> args, RedirectInfo redirect) {
        List<SimpleCommand> pipeline = List.of(new SimpleCommand(cmd, args));
        executePipeline(pipeline, redirect);
    }

    public static void executePipeline(List<SimpleCommand> pipeline, RedirectInfo redirect) {
        String input = "";
        int code = 0;

        for (SimpleCommand sc : pipeline) {
            List<String> expandedArgs = expandSpecialArgs(sc.args());
            ExecResult r = runCommand(sc.cmd(), expandedArgs, input);
            code = r.code();
            input = r.out();
            lastExitCode = code;
        }

        writeFinalOutput(input, redirect);
    }

    private static List<String> expandSpecialArgs(List<String> args) {
        List<String> out = new ArrayList<>(args.size());
        for (String a : args) {
            if ("$?".equals(a)) out.add(String.valueOf(lastExitCode));
            else out.add(a);
        }
        return out;
    }

    private static ExecResult runCommand(String cmd, List<String> args, String input) {
        return switch (cmd) {
            case "echo" -> echo(args);
            case "ls"   -> ls(args);
            case "cat"  -> cat(args);
            case "wc"   -> wc(args, input);
            case "grep" -> grep(args, input);
            default -> {
                System.err.println("Unknown command: " + cmd);
                yield new ExecResult(1, "");
            }
        };
    }

    private static ExecResult echo(List<String> args) {
        return new ExecResult(0, String.join(" ", args) + System.lineSeparator());
    }

    private static ExecResult ls(List<String> args) {
        Path dir = args.isEmpty() ? Paths.get(".") : safePath(args.get(0), "ls");
        if (dir == null) return new ExecResult(1, "");

        StringBuilder sb = new StringBuilder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) sb.append(p.getFileName()).append(System.lineSeparator());
            return new ExecResult(0, sb.toString());
        } catch (IOException e) {
            System.err.println("ls: " + e.getMessage());
            return new ExecResult(1, "");
        }
    }

    private static ExecResult cat(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: cat [filename]");
            return new ExecResult(1, "");
        }

        StringBuilder sb = new StringBuilder();
        int code = 0;

        for (String arg : args) {
            Path p = safePath(arg, "cat");
            if (p == null) { code = 1; continue; }

            if (!Files.exists(p)) {
                System.err.println("No file: " + arg);
                code = 1;
                continue;
            }

            try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append(System.lineSeparator());
            } catch (IOException e) {
                System.err.println("cat: " + arg + ": " + e.getMessage());
                code = 1;
            }
        }

        return new ExecResult(code, sb.toString());
    }

    private static ExecResult wc(List<String> args, String input) {
        if (args.isEmpty()) {
            long lines = countLines(input);
            return new ExecResult(0, lines + System.lineSeparator());
        }

        StringBuilder sb = new StringBuilder();
        int code = 0;

        for (String arg : args) {
            Path p = safePath(arg, "wc");
            if (p == null) { code = 1; continue; }

            if (!Files.exists(p)) {
                System.err.println("No file: " + arg);
                code = 1;
                continue;
            }

            try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                long cnt = br.lines().count();
                sb.append(cnt).append(System.lineSeparator());
            } catch (IOException e) {
                System.err.println("wc: " + arg + ": " + e.getMessage());
                code = 1;
            }
        }

        return new ExecResult(code, sb.toString());
    }

    private static ExecResult grep(List<String> args, String input) {
        if (args.size() != 1) {
            System.err.println("Usage: grep [pattern]");
            return new ExecResult(1, "");
        }
        String pat = args.get(0);

        if (input.isEmpty()) return new ExecResult(0, "");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new StringReader(input))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(pat)) sb.append(line).append(System.lineSeparator());
            }
        } catch (IOException impossible) {
        }

        return new ExecResult(0, sb.toString());
    }

    private static void writeFinalOutput(String text, RedirectInfo redirect) {
        if (redirect == null) {
            System.out.print(text);
            return;
        }

        boolean append = redirect.append();
        Path outDir = Paths.get("out");

        final Path file;
        try {
            file = outDir.resolve(redirect.filename()).normalize();
        } catch (InvalidPathException e) {
            System.err.println("redirect error: invalid path: " + e.getInput());
            lastExitCode = 1;
            return;
        }

        try {
            Files.createDirectories(outDir);

            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (PrintStream out = new PrintStream(
                    new FileOutputStream(file.toFile(), append),
                    true,
                    StandardCharsets.UTF_8)) {
                out.print(text);
            }
        } catch (IOException e) {
            System.err.println("redirect error: " + e.getMessage());
            lastExitCode = 1;
        }
    }

    private static Path safePath(String s, String who) {
        try {
            return Paths.get(s);
        } catch (InvalidPathException e) {
            System.err.println(who + ": invalid path: " + e.getInput());
            return null;
        }
    }

    private static long countLines(String s) {
        if (s == null || s.isEmpty()) return 0;

        long cnt = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') cnt++;
        }
        if (s.charAt(s.length() - 1) != '\n') cnt++;
        return cnt;
    }
}
