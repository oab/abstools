//$Id$

package abs.frontend.parser;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import abs.frontend.analyser.SemanticError;
import abs.frontend.analyser.SemanticErrorList;
import abs.frontend.ast.CompilationUnit;
import abs.frontend.ast.List;
import abs.frontend.ast.Model;
import abs.frontend.parser.ABSParser;
import abs.frontend.parser.ABSScanner;
import abs.frontend.parser.SyntaxError;

public class Main {

	private static final String ABS_STD_LIB = "abs/lang/abslang.abs";
    protected boolean verbose = false ;
	protected boolean typecheck = true;
	protected boolean stdlib = true;

	public static void main(final String[] args) throws Exception {
	    new Main().parse(args);
	}

	public java.util.List<String> parseArgs(String[] args) throws Exception {
	    ArrayList<String> remaindingArgs = new ArrayList<String>();
	    
        for (String arg : args) {
            if (arg.equals("-v"))
                verbose = true;
            else if (arg.equals("-notypecheck")) 
                typecheck = false;
            else if (arg.equals("-nostdlib")) 
                stdlib = false;
            else if (arg.equals("-h")) {
                printUsageAndExit();
            } else
                remaindingArgs.add(arg);
            
        }
        
        return remaindingArgs;
	    
	}
	
	public Model parse(final String[] args) throws Exception {

	    java.util.List<String> files = parseArgs(args);
	    
	    if (files.isEmpty()) {
	        printErrorAndExit("Please provide at least on intput file");
	    }
	    
		List<CompilationUnit> units = new List<CompilationUnit>();
		
		if (stdlib) {
		    units.add(getStdLib());
		}
		
		for (String file : files){
		    if (file.startsWith("-")) {
		        printErrorAndExit("Illegal option "+file);
		    }

			try{
				units.add(parseUnit(new File(file)));
				
			} catch (FileNotFoundException e1) {
				printErrorAndExit("File not found: " + file);
			} catch (ParseException pex) {
				System.err.println(file + ":" + pex.getError().getHelpMessage());
				System.exit(1);
			} catch (Exception e1) {
				// Catch-all
				System.err.println("Compilation of " + file +  " failed with Exception");
				System.err.println(e1);
				System.exit(1);
			}
		}
		
        Model m = new Model(units);
		
        // Dump tree for debug
        if (verbose){ 
            System.out.println("Result:");
            System.out.println(m);
            m.dumpTree("  ", System.out);
        }

        int numSemErrs = m.getErrors().size();
            
        if (numSemErrs > 0) {
            System.out.println("Semantic errors: " + numSemErrs);
            for (SemanticError error : m.getErrors()) {
                System.err.println(error.getMsgString());
                System.err.flush();
            }
        } else {
            if (typecheck) {
                SemanticErrorList typeerrors = m.typeCheck();
                for (SemanticError se : typeerrors) {
                    System.err.println(se.getMsgString());
                }
            }
        }
        
		return m;
	}

    private void printErrorAndExit(String error) {
        System.err.println("\nCompilation failed:\n");
        System.err.println("  "+error);
        System.err.println();
        printUsageAndExit();
    }

    private void printUsageAndExit() {
        printUsage();
        System.exit(1);
    }


	
	
    private static CompilationUnit getStdLib() throws Exception {
        URL url = Main.class.getClassLoader().getResource(ABS_STD_LIB);
        if (url == null) {
            System.err.println("Could not found ABS Standard Library");
            System.exit(1);
        }
        return parseUnit(new File(url.toURI()),null,new InputStreamReader(url.openStream()));
    }




    protected void printUsage() {
        System.out.println(
                "*******************************\n"+
                "*        ABS TOOL SUITE       *\n"+
                "*******************************\n"+
                "Usage: java "+this.getClass().getName()+" [options] <absfiles>\n\n" +
        		"  <absfiles>    ABS files to parse\n\n" +
        		"Options:\n"+
        		"  -v            verbose output\n" +
        		"  -notypecheck  disable typechecking\n" +
                "  -nostdlib     do not include the standard lib \n" +
        		"  -h            print this message\n");
        
    }




    public static CompilationUnit parseUnit(File file) throws Exception {
		Reader reader = new FileReader(file);
		BufferedReader rd = null;
		//Set to true to print source before parsing 
		boolean dumpinput = false;
		if (dumpinput){
			try {
				rd = new BufferedReader(new FileReader(file));
				String line = null;
				int i = 1 ; 
				while ((line = rd.readLine()) != null) {
					System.out.println(i++ + "\t" + line);
				}
			} catch (IOException x) {
				System.out.flush();
				System.err.println(x);
				System.err.flush();
			} finally {
				if (rd != null) rd.close();
			}
		}

		return parseUnit(file, null, reader); 
	}
    
    public static Model parse(File file, boolean withStdLib) throws Exception {
        List<CompilationUnit> units = new List<CompilationUnit>();
        if (withStdLib)
            units.add(getStdLib());
        units.add(parseUnit(file));
        return new Model(units);
    }
    
    public static Model parse(ArrayList<String> fileNames, boolean withStdLib) throws Exception {
    	List<CompilationUnit> units = new List<CompilationUnit>();
    	if (withStdLib) units.add(getStdLib());
    	for (String filename : fileNames) {
    		units.add(parseUnit(new File(filename)));
    	}
    	return new Model(units);
    }
	
	public static Model parse(File file, String sourceCode, InputStream stream, boolean withStdLib) throws Exception {
	    return parse(file, sourceCode, new BufferedReader(new InputStreamReader(stream)), withStdLib);
	}
	    
	public static Model parse(File file, String sourceCode, Reader reader, boolean withStdLib) throws Exception {
        List<CompilationUnit> units = new List<CompilationUnit>();
        if (withStdLib)
            units.add(getStdLib());
        units.add(parseUnit(file, sourceCode, reader));
        return new Model(units);
	}
	
    public static CompilationUnit parseUnit(File file, String sourceCode, Reader reader) throws Exception {
        try {
            ABSParser parser = new ABSParser();
            ABSScanner scanner = new ABSScanner(reader);
            parser.setSourceCode(sourceCode);
            parser.setFile(file);
            
            return (CompilationUnit) parser.parse(scanner);
        } finally {
            reader.close();
        }
	}


    public static Model parseString(String s, boolean withStdLib) throws Exception {
        return parse(null,s, new StringReader(s), withStdLib);
    }

}
