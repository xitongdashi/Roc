/*
 * Copyright (c) 2014 Roc Project.  This is free software.  See
 * LICENSE.txt for details.
 */

package mloss.roc;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mloss.roc.util.NaiveCsvReader;


// General idea is to read columns (scores or labels) from files and
// then send them for appropriate processing (joining, sorting, curve
// generation).

// TODO handle file options
// TODO project input CSVs
// TODO join input CSVs
// TODO connect input to Curve building
// TODO check for empty input
// TODO more (basic) examples in help


/**
 * <p>Main reads the results of a binary classification and generates a
 * report of the ROC and PR results.  The inputs are the scores given to
 * the examples and the associated groud truth labels.  The output is a
 * customizable report including ROC and/or PR curves and areas.</p>
 *
 * <p>The inputs can be specified with or without the examples in one or
 * more CSV (or other delimited text) inputs.  The general aim is to
 * take in a variety of inputs so that little or no processing needs to
 * be done to the output of a classification algorithm and so that
 * generating ROC/PR analyses are easy.  Indeed, this main will even
 * join scores and labels together database-style.</p>
 */
public class Main {

    public static final String aboutMessage =
        "Roc 0.1.0.  ROC and PR curves.\n" +
        "Copyright (c) 2014 Roc Project.  This is free software; see the license.\n" +
        "https://github.com/kboyd/Roc";

    // TODO replace license message with dumping license file from JAR
    // (or similar "resource" access)
    public static final String licenseMessage =
        "Roc is free, open source software licensed under the BSD 2-clause (FreeBSD) license.";

    public static final String helpKey = "--help";
    public static final String versionKey = "--version";
    public static final String aboutKey = "--about";
    public static final String licenseKey = "--license";
    public static final String debugKey = "--debug";
    public static final String scoresKey = "--scores";
    public static final String labelsKey = "--labels";
    public static final String scoresLabelsKey = "--scores-labels";

    public static final String stdioFileName = "-";

    private static final String indent = "        ";

    // TODO load help from a file or have it compiled in or options package?
    // file pros: easy to write, could work with multiple languages
    // file cons: how load reliably? (resources loader?), synchronization of code and help
    // here pros: code and help automatically synchronized
    // here cons: complex to read/write, hard to format
    // opts pros: might provide best-of-both-worlds of above
    // opts cons: third-party, syntax/verbosity

    // Manually wrap the help text to 80 characters (column 89 for
    // non-indented, column 81 for indented)
    public static final String help =
        "                      Roc.  Everything ROC and PR curves.\n\n" +

        "SYNOPSIS\n\n" +

        "java mloss.roc.Main [OPTION [ARGUMENT]]...\n\n" +

        "DESCRIPTION\n\n" +

        "Reads the scores and labels of a binary classification result and prints ROC and\n" +
        "PR analysis reports.  When no options are given, this program operates as if\n" +
        "'--scores-labels -' had been given.  A '-' as a file name indicates standard\n" +
        "input and can be used for any file argument that makes sense.\n" +
        "\n" +

        "File options can be appended to a file name to specify the columns of interest.\n" +
        "The syntax is a colon followed by a dictionary having no spaces.  The keys of\n" +
        "the dictionary are 'k' for key, 's' for score, and 'l' for label.  The values\n" +
        "are column numbers or a list of column numbers for 'k' to use a compound key.\n" +
        "See the examples below.\n" +
        "\n" +

        "OPTIONS\n\n" +

        helpKey + "\n" + indent + "Display this help.\n" +
        versionKey + " | " + aboutKey + "\n" + indent +
        "Display the version and other information about this software.\n" +
        licenseKey + "\n" + indent +
        "Display a summary of the license for this software.\n" +
        debugKey + "\n" + indent + "Print stack traces, etc.\n" +
        scoresKey + " FILE[:OPTS]\n" + indent +
        "File containing scores, one per line (CSV format).  Must be specified in\n" + indent +
        "combination with --labels.  The scores are matched to the labels by the\n" + indent +
        "specified key or otherwise by line number.  Accepts file options for key\n" + indent +
        "and score columns.  No default.\n" +
        labelsKey + " FILE[:OPTS]\n" + indent +
        "File containing labels, one per line (CSV format).  Labels specified by\n" + indent +
        "themselves are treated as already ranked from most positive to most\n" + indent +
        "negative.  Accepts file options for key and label columns.  No default.\n" +
        scoresLabelsKey + " FILE[:OPTS]\n" + indent +
        "File containing scores and labels, in two-column CSV format.\n" + indent +
        "Accepts file options for score and label columns.  No default.\n" +
        "\n" +

        "EXAMPLES\n\n" +

        "Join results with ground truth:\n" +
        "    ...Main --scores results.csv:{k:1,s:5} --labels truth.csv:{l:4,k:3}\n" +

        "Join using compound key (whose fields are reversed in the truth):\n" +
        "    ...Main --scores results.csv:{s:3,k:[1,2]} --labels truth.csv:{k:[2,1],l:3}\n" +

        "Like above with shell quoting (bash):\n" +
        "    ...Main --scores results.csv:'{k:1,s:5}' --labels 'truth.csv:{l:4,k:3}'\n" +

        "Specify score and label columns:\n" +
        "    ...Main --scores-labels scrsLbls.csv:{s:4,l:3}\n" +

        "Options for standard input:\n" +
        "    <classifier> | java...Main --scores-labels -:{s:102,l:103}\n" +

        "";  // This is here to make inserting/reordering lines easier

    /**
     * Pattern to recognize various requests for help: -h, -?, --help,
     * help, etc.
     */
    private static final Pattern helpPattern =
        Pattern.compile("(--?(h(elp)?|\\?))|help");

    /**
     * Pattern to recognize basic JSON-like dictionaries attached at the
     * end of a string with a colon.
     */
    private static final Pattern okFileNameWithOptionsPattern =
        Pattern.compile("(.+):\\{((?:[kls]:(?:\\d+|\\[\\d+(?:,\\d+)*\\]))(?:,[kls]:(?:\\d+|\\[\\d+(?:,\\d+)*\\]))*)\\}");

    /**
     * Pattern to extract individual key-value pairs from the basic
     * JSON-like dictionaries.
     */
    private static final Pattern fileNameOptionPattern =
        Pattern.compile(",?([kls]):(?:(\\d+)|\\[(\\d+(?:,\\d+)*)\\])");

    /** Pattern to recognize a file with an attempted dictionary. */
    private static final Pattern fileNameWithDictPattern =
        Pattern.compile("(.+):(\\{.*?\\})");

    /** Pattern to split by commas. */
    private static final Pattern commaSplitPattern =
        Pattern.compile("\\s*,\\s*");

    /**
     *
     */
    public static void apiMain(String[] args, BufferedReader input,
                               PrintWriter output, PrintWriter error
                               ) throws MainException, FileNotFoundException, IOException {
        // Environment.  For now this is a somewhat naive environment
        // that maps keys (command line options) to FileArguments or
        // unparsed values (or null).  Booleans are represented by key
        // existence.  It would be better to map the keys to appropriate
        // objects that can handle the various types of environment
        // values, like in a command line parsing library.
        String defaultOperation = scoresLabelsKey;
        String defaultFileName = stdioFileName;
        String defaultDelimiter = ",";
        Map<String, Object> env = new TreeMap<String, Object>();

        // Search the command line arguments for requested help.  Help
        // overrides all other operations and must be processed before
        // any possible exceptions.  Therefore it must be treated
        // specially.
        for (String arg : args) {
            if (helpPattern.matcher(arg.toLowerCase()).matches()) {
                error.println(help);
                return;
            }
        }

        // Parse the command line arguments
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            String arg = args[argIndex].toLowerCase();
            if (arg.equals(helpKey) ||
                arg.equals(versionKey) ||
                arg.equals(aboutKey) ||
                arg.equals(licenseKey)
                ) {
                env.put(arg, null);
            } else if (arg.equals(scoresKey) ||
                       arg.equals(labelsKey) ||
                       arg.equals(scoresLabelsKey)
                       ) {
                // Check next argument exists
                if (argIndex + 1 < args.length) {
                    FileArgument fileArg = parseFileNameWithOptions(args[argIndex + 1]);
                    env.put(arg, fileArg);
                    argIndex++;
                } else {
                    throw new MainException(String.format("Argument missing for option: %s", arg), ExitStatus.ERROR_USAGE);
                }
            } else if (arg.equals(debugKey)) {
                // Ignore.  Nothing to be done.
            } else {
                throw new MainException(String.format("Unrecognized option: %s", arg), ExitStatus.ERROR_USAGE);
            }
        }

        // Enforce logical constraints between options
        if (env.containsKey(scoresKey) && !env.containsKey(labelsKey)) {
            throw new MainException(String.format("Option '%s' must be accompanied by option '%s'.", scoresKey, labelsKey), ExitStatus.ERROR_USAGE);
        }

        // Check files exist up front before processing anything.  (All
        // files happen to be inputs.)
        for (Object value : env.values()) {
            if (value instanceof FileArgument) {
                String fileName = ((FileArgument) value).name;
                if (fileName.equals(defaultFileName))
                    continue;
                File file = new File(fileName);
                if (!file.exists() || !file.isFile() || !file.canRead()) {
                    throw new MainException(String.format("Not a readable file: %s", fileName), ExitStatus.ERROR_FILE);
                }
            }
        }

        // If no operations, do the default
        if (env.size() == 0) {
            FileArgument defaultFile = new FileArgument();
            defaultFile.name = defaultFileName;
            env.put(defaultOperation, defaultFile);
        }

        // Process the operations

        // Print about
        if (env.containsKey(versionKey) || env.containsKey(aboutKey)) {
            error.println(aboutMessage);
        }
        // Print license
        if (env.containsKey(licenseKey)) {
            error.println(licenseMessage);
        }
        // Process inputs.  Providing scores and labels together in one
        // input overrides providing them individually.
        if (env.containsKey(scoresLabelsKey)) {
            // Input is scores and labels together
            //output.println(scoresLabelsKey);
            //output.println(env.get(scoresLabelsKey));
            // Get scores and labels CSV input from a file or stdin as specified
            FileArgument slFile = (FileArgument) env.get(scoresLabelsKey);
            BufferedReader scoresLabelsInput = openFileOrDefault(slFile.name, stdioFileName, input);
            // Generate curve and output
            processScoresLabelsOneInput(scoresLabelsInput, defaultDelimiter, 0, 1, output);
        } else if (env.containsKey(labelsKey)) {
            // Input is scores and labels separately or labels only
            BufferedReader scoresInput = null;
            BufferedReader labelsInput = null;
            // Get scores input only if given
            if (env.containsKey(scoresKey)) {
                //output.println(scoresKey);
                //output.println(env.get(scoresKey));
                // Get scores CSV input from a file or stdin as specified
                FileArgument sFile = (FileArgument) env.get(scoresKey);
                scoresInput = openFileOrDefault(sFile.name, stdioFileName, input);
            }
            // Get labels CSV input from a file or stdin as specified
            FileArgument lFile = (FileArgument) env.get(labelsKey);
            labelsInput = openFileOrDefault(lFile.name, stdioFileName, input);
            // Generate curve and output
            processScoresLabelsTwoInputs(scoresInput, labelsInput, defaultDelimiter, output);
        }
    }

    /**
     *
     */
    public static void apiMain(String[] args)
        throws MainException, FileNotFoundException, IOException {
        apiMain(args,
                new BufferedReader(new InputStreamReader(System.in)),
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true));
    }

    /**
     *
     */
    public enum ExitStatus {
        OK,
        ERROR_USAGE,
        ERROR_INPUT,
        ERROR_FILE,
        ERROR_INTERNAL,
    }

    /**
     *
     */
    public static class MainException extends Exception {
        private static final long serialVersionUID = 1L;

        public ExitStatus exitStatus;

        public MainException(String message, ExitStatus exitStatus) {
            super(message);
            this.exitStatus = exitStatus;
        }
    }

    /**
     * Calls {@link #apiMain(String[])}, handles its exceptions, and
     * exits.
     */
    public static void main(String[] args) {
        try {
            apiMain(args);
        } catch (Exception e) {
            //System.err.println(String.format("roc: Error: %s: %s", e.getClass().getName(), e.getMessage()));
            System.err.println(String.format("roc: Error: %s", e.getMessage()));
            // Print a stack trace if in debug mode
            if (Arrays.asList(args).contains(debugKey)) {
                e.printStackTrace();
            }
            // Determine and return exit status
            int exitStatus = ExitStatus.ERROR_INTERNAL.ordinal();
            if (e instanceof MainException) {
                exitStatus = ((MainException) e).exitStatus.ordinal();
            }
            System.exit(exitStatus);
        }
    }

    public static FileArgument parseFileNameWithOptions(String token) throws MainException {
        FileArgument fileArg = new FileArgument();
        // Parse the file name and (possible) options
        Matcher fileWOptsMatcher = okFileNameWithOptionsPattern.matcher(token);
        Matcher fileWDictMatcher = fileNameWithDictPattern.matcher(token);
        if (fileWOptsMatcher.matches()) {
            fileArg.name = fileWOptsMatcher.group(1);
            String options = fileWOptsMatcher.group(2);
            Matcher optMatcher = fileNameOptionPattern.matcher(options);
            while (optMatcher.find()) {
                // TODO worry about keys specified multiple times?
                String key = optMatcher.group(1);
                switch (key.charAt(0)) {
                case 'k':
                    String[] colStrings = commaSplitPattern.split(optMatcher.group(3));
                    int[] cols = new int[colStrings.length];
                    for (int colIndex = 0; colIndex < cols.length; colIndex++) {
                        cols[colIndex] = Integer.parseInt(colStrings[colIndex]);
                    }
                    fileArg.keyCols = cols;
                    break;
                case 'l':
                    fileArg.labelCol = Integer.parseInt(optMatcher.group(2));
                    break;
                case 's':
                    fileArg.scoreCol = Integer.parseInt(optMatcher.group(2));
                    break;
                default:
                    String msg = String.format("File option key not in {k,l,s}: '%s' in '%s'", key, options);
                    throw new MainException(msg, ExitStatus.ERROR_USAGE);
                }
            }
        } else if (fileWDictMatcher.matches()) {
            String msg = String.format("Bad options for file '%s': %s",
                                       fileWDictMatcher.group(1),
                                       fileWDictMatcher.group(2));
            throw new MainException(msg, ExitStatus.ERROR_USAGE);
        } else {
            // Apparently no options so the whole token is the file name
            fileArg.name = token;
        }
        return fileArg;
    }

    private static class FileArgument {
        public String name;
        public int[] keyCols;
        public int scoreCol;
        public int labelCol;
    }

    public static BufferedReader openFileOrDefault(String fileName, String defaultName, BufferedReader defaultInput) throws IOException {
        if (fileName.equals(defaultName)) {
            return defaultInput;
        } else {
            return new BufferedReader(new FileReader(fileName));
        }
    }

    private static void processScoresLabelsOneInput(BufferedReader scoresLabelsInput, String delimiter, int scoresColumn, int labelsColumn, PrintWriter output) throws IOException {
        // Read the CSV input
        List<String[]> csvRows = new NaiveCsvReader(scoresLabelsInput, delimiter).readAll();
        //for (String[] row : csvRows) {
        //    output.println(Arrays.toString(row));
        //}
        // Create iterators for appropriate columns
        Iterator<Double> scoresIterator = new StringToDoubleConversionIterator(new ProjectionIterator<String>(csvRows, scoresColumn));
        Iterator<String> labelsIterator = new ProjectionIterator<String>(csvRows, labelsColumn);
        // Call curve generation and output
        printReport(buildCurve(scoresIterator, labelsIterator), output);
    }

    private static void processScoresLabelsTwoInputs(BufferedReader scoresInput, BufferedReader labelsInput, String delimiter, PrintWriter output) throws IOException {
        List<String[]> csvRows;
        Iterator<Double> scoresIterator = null;
        // Process scores input only if given
        if (scoresInput != null) {
            // Read CSV input
            csvRows = new NaiveCsvReader(scoresInput, delimiter).readAll();
            //for (String[] row : csvRows) {
            //    output.println(Arrays.toString(row));
            //}
            // Create scores iterator
            scoresIterator = new StringToDoubleConversionIterator(new ProjectionIterator<String>(csvRows, 0));
        }
        // Process labels input
        csvRows = new NaiveCsvReader(labelsInput, delimiter).readAll();
        //for (String[] row : csvRows) {
        //    output.println(Arrays.toString(row));
        //}
        // Create labels iterator
        Iterator<String> labelsIterator = new ProjectionIterator<String>(csvRows, 0);
        // Build the curve
        Curve curve;
        if (scoresIterator == null) {
            curve = buildCurve(labelsIterator);
        } else {
            curve = buildCurve(scoresIterator, labelsIterator);
        }
        // Call curve generation and output
        printReport(curve, output);
    }

    private static Curve buildCurve(Iterator<String> labelsIterator) {
        return new Curve.Builder<Double,String>()
            .rankedLabels(new IteratorIterable<String>(labelsIterator))
            .positiveLabel("1")
            .build();
    }

    private static Curve buildCurve(Iterator<Double> scoresIterator, Iterator<String> labelsIterator) {
        return new Curve.Builder<Double,String>()
            .predicteds(new IteratorIterable<Double>(scoresIterator))
            .actuals(new IteratorIterable<String>(labelsIterator))
            .positiveLabel("1")
            .build();
    }

    public static class ProjectionIterator<E> implements Iterator<E> {
        private Iterator<E[]> iterator;
        private int column;

        public ProjectionIterator(List<E[]> table, int column) {
            iterator = table.iterator();
            this.column = column;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public E next() {
            E[] next = iterator.next();
            if (next.length > column) {
                return next[column];
            } else {
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class StringToDoubleConversionIterator implements Iterator<Double> {

        private Iterator<String> iterator;

        public StringToDoubleConversionIterator(Iterator<String> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Double next() {
            String next = iterator.next();
            try {
                return Double.parseDouble(next);
            } catch (NumberFormatException e) {
                // Fix exception message so that it is independent of the exception name
                throw new NumberFormatException(String.format("Not a floating-point number: \"%s\"", next));
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class IteratorIterable<T> implements Iterable<T> {
        private Iterator<T> iterator;

        public IteratorIterable(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        public Iterator<T> iterator() {
            return iterator;
        }
    }

    public static void printReport(Curve curve, PrintWriter output /* TODO make report object to handle what to include and what format */) {
        output.println("%YAML 1.1");
        output.println("---");
        output.println("ROC area: " + curve.rocArea());
        double[][] rocPoints = curve.rocPoints();
        output.println(String.format("ROC points count: %d", rocPoints.length));
        output.println("ROC points:");
        for (double[] point : rocPoints) {
            // Just format the floating point numbers to string for now
            // because I can't find a floating point format that works
            // like the string formatting and chops off the trailing
            // zeros.
            output.println(String.format("  - [%s, %s]", point[0], point[1]));
        }
        output.println("ROC points Gnuplot text: |");
        for (double[] point : rocPoints) {
            // Just format the floating point numbers to string for now
            // because I can't find a floating point format that works
            // like the string formatting and chops off the trailing
            // zeros.
            output.println(String.format("%s %s", point[0], point[1]));
        }
        output.println("# End ROC points Gnuplot text");
        output.println("...");
    }
}
