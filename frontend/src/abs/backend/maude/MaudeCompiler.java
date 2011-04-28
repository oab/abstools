/** 
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.backend.maude;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import abs.frontend.ast.Model;
import abs.frontend.parser.Main;

public class MaudeCompiler extends Main {
    String module = "ABS-SIMULATOR-RL";
    private boolean flatten;
    private File outputfile;

    
    public static void main(final String... args) {
        try {
            new MaudeCompiler().compile(args);
        } catch (Exception e) {
            System.err.println("An error occurred during compilation: " + e.getMessage());

            if (Arrays.asList(args).contains("-debug")) {
                e.printStackTrace();
            }

            System.exit(1);
        }
    }
    
    @Override
    public List<String> parseArgs(String[] args) throws Exception {
        List<String> restArgs = super.parseArgs(args);
        List<String> remainingArgs = new ArrayList<String>();

        for (int i = 0; i < restArgs.size(); i++) {
            String arg = restArgs.get(i);
            if (arg.equals("-timed")) {
                module = "ABS-SIMULATOR-EQ-TIMED";
            } else if (arg.startsWith("-product=")) {
                flatten = true;
                product = arg.split("=")[1];
            } else if (arg.equals("-o")) {
                i++;
                if (i == restArgs.size()) {
                    System.err.println("Please provide an output file");
                    System.exit(1);
                } else {
                    outputfile = new File(args[i]);
                }
                
            } else {
                remainingArgs.add(arg);
            }
        }

        return remainingArgs;
    }
    
    
    /**
     * @param args
     * @throws Exception
     */
    public void compile(String[] args) throws Exception {
        final Model model = parse(args);
        if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors())
            return;

        if (flatten) {
            model.flattenForProduct(product);
        }
        
        PrintStream stream = System.out;
        if (outputfile != null) {
            stream = new PrintStream(outputfile);
        }
        
        model.generateMaude(stream, module, !flatten);
    }

    protected void printUsage() {
        super.printUsage();
        System.out.println("Maude Backend:\n"
                + "  -o <file>      write output to <file> instead of standard output\n"
                + "  -timed         generate code for timed interpreter\n"
        );
    }

}

// Local Variables:
// tab-width: 4
// End:
