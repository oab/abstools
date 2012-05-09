package eu.hatsproject.absplugin.actions.runconfig.java;

import static eu.hatsproject.absplugin.util.Constants.DEBUGGER_ARGS_OTHER_DEFAULT;
import static eu.hatsproject.absplugin.util.Constants.DEBUGGER_COMPILE_BEFORE_DEFAULT;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_COMPILE_BEFORE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_DEBUG_MODE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_DEBUG_MODE_DEFAULT;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_OBSERVER_LIST;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_OTHER_ARGS_ATTRIBUTE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_RANDOMSEED;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_SCHEDULER_ATTRIBUTE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_USE_EXTERNAL;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_DEBUGGER_USE_EXTERNAL_DEFAULT;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_ECLIPSE_SCHEDULER_ATTRIBUTE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_FLI_IGNORE_MISSING_CLASSES;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_HISTORY_FILE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_PRODUCT_NAME_ATTRIBUTE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_PROJECT_NAME_ATTRIBUTE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_RUNTARGET_ATTRIBUTE;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_RUN_AUTOMATICALLY;
import static eu.hatsproject.absplugin.util.Constants.RUNCONFIG_TEST_EXECUTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import abs.backend.java.visualization.UMLSequenceChart;
import eu.hatsproject.absplugin.actions.runconfig.RunConfigEnums.DebuggerObserver;
import eu.hatsproject.absplugin.actions.runconfig.RunConfigEnums.DebuggerScheduler;
import eu.hatsproject.absplugin.util.UtilityFunctions;

/**
 * A wrapper around ILaunchConfiguration to ease getting and setting launch configurations for java
 */
public class JavaLaunchConfig {



    private final ILaunchConfigurationWorkingCopy wConfig;
    private final ILaunchConfiguration rConfig;


    public JavaLaunchConfig(ILaunchConfigurationWorkingCopy config) {
        this.wConfig = config;
        this.rConfig = config;
    }

    public JavaLaunchConfig(ILaunchConfiguration config) {
        this.rConfig = config;
        this.wConfig = null;
    }



    public void setDefaults() {
        setRandomSeed(0);
        setProjectName("");
        setProductName("");

        setTestExecution(false);
        ArrayList<String> tempList = new ArrayList<String>();
        for (DebuggerObserver observer : DebuggerObserver.values()) {
            if(observer.getDefaultSelection()){
                tempList.add(observer.getClassName());
            }
        }
        setDebuggerObserverList(tempList);

        setCompileBefore(DEBUGGER_COMPILE_BEFORE_DEFAULT);
        setOtherArgs(DEBUGGER_ARGS_OTHER_DEFAULT);
        setUseExternal(RUNCONFIG_DEBUGGER_USE_EXTERNAL_DEFAULT);
        setDebugMode(RUNCONFIG_DEBUGGER_DEBUG_MODE_DEFAULT);
        if(RUNCONFIG_DEBUGGER_DEBUG_MODE_DEFAULT){
            setDebuggerScheduler(DebuggerScheduler.ECLIPSE.toString());
        } else {
            setDebuggerScheduler(DebuggerScheduler.getDefaultScheduler().toString());
        }
    }
    
    
    public boolean hasFixedRandomSeed() throws CoreException {
        String s = rConfig.getAttribute(RUNCONFIG_DEBUGGER_RANDOMSEED, "");
        return s.startsWith("-Dabs.randomseed=");
    }
    
    public void unsetRandomSeed() {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_RANDOMSEED, "");
    }

    
    public void setRandomSeed(int seed) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_RANDOMSEED, "-Dabs.randomseed=" + seed);
    }
    
    public void setRandomSeed(String seed) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_RANDOMSEED, "-Dabs.randomseed=" + seed);
    }

    public int getRandomSeed() throws CoreException {
        String s = getRandomSeedString();
        if (UtilityFunctions.isNumber(s)) {
            return Integer.parseInt(s);
        } else {
            return 0;
        }
    }
    
    public String getRandomSeedString() throws CoreException {
        String s = rConfig.getAttribute(RUNCONFIG_DEBUGGER_RANDOMSEED, "-Dabs.randomseed=0");
        if (s.startsWith("-Dabs.randomseed=")) {
            return s.substring("-Dabs.randomseed=".length());
        } else {
            return "";
        }
    }

    public void setProjectName(String name) {
        wConfig.setAttribute(RUNCONFIG_PROJECT_NAME_ATTRIBUTE, name);
    }

    public String getProjectName() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_PROJECT_NAME_ATTRIBUTE, "");
    }


    public void setProductName(String name) {
        wConfig.setAttribute(RUNCONFIG_PRODUCT_NAME_ATTRIBUTE, name);
    }

    public String getProductName() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_PRODUCT_NAME_ATTRIBUTE, (String)null);
    }

    public void setTestExecution(boolean executeTests) {
        wConfig.setAttribute(RUNCONFIG_TEST_EXECUTION, executeTests);
    }

    public boolean getTestExecution() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_TEST_EXECUTION, false);
    }

    public void setDebuggerObserverList(List<String> observerList) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_OBSERVER_LIST, observerList);
    }

    @SuppressWarnings("unchecked")
    public List<String> getDebuggerObserverList() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_DEBUGGER_OBSERVER_LIST, Collections.emptyList());
    }

    public void setCompileBefore(boolean compileBefore) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_COMPILE_BEFORE, compileBefore);
    }

    public boolean getCompileBefore() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_DEBUGGER_COMPILE_BEFORE, false);
    }


    public void setOtherArgs(String otherArgs) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_OTHER_ARGS_ATTRIBUTE, otherArgs);
    }

    public String getOtherArgs() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_DEBUGGER_OTHER_ARGS_ATTRIBUTE, "");
    }

    public void setUseExternal(boolean useExternal) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_USE_EXTERNAL, useExternal);
    }

    public boolean getUseExternal() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_DEBUGGER_USE_EXTERNAL, false);
    }


    public void setDebugMode(boolean debugMode) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_DEBUG_MODE, debugMode);
    }

    public boolean getDebugMode() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_DEBUGGER_DEBUG_MODE, false);
    }


    public void setDebuggerScheduler(String debuggerScheduler) {
        wConfig.setAttribute(RUNCONFIG_DEBUGGER_SCHEDULER_ATTRIBUTE, debuggerScheduler);
    }

    public String getDebuggerScheduler() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_DEBUGGER_SCHEDULER_ATTRIBUTE, "");
    }

    public void setRunTarget(String runTarget) {
        wConfig.setAttribute(RUNCONFIG_RUNTARGET_ATTRIBUTE, runTarget);
    }

    public String getRunTarget() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_RUNTARGET_ATTRIBUTE, "");
    }

    public void setDrawSequenceDiagram(boolean draw) throws CoreException {
        if (draw != getDrawSequenceDiagram()) {
            List<String> observers = new ArrayList<String>(getDebuggerObserverList());
            if (draw) {
                observers.add(UMLSequenceChart.class.getName());
            } else {
                observers.remove(UMLSequenceChart.class.getName());
            }
            setDebuggerObserverList(observers);
        }
    }

    public boolean getDrawSequenceDiagram() throws CoreException {
        return getDebuggerObserverList().contains(UMLSequenceChart.class.getName());
    }



    public EclipseScheduler getScheduler() throws CoreException {
        String s = rConfig.getAttribute(RUNCONFIG_ECLIPSE_SCHEDULER_ATTRIBUTE, EclipseScheduler.MANUAL.toString());
        return EclipseScheduler.valueOf(s);
    }

    public void setScheduler(EclipseScheduler s) {
        wConfig.setAttribute(RUNCONFIG_ECLIPSE_SCHEDULER_ATTRIBUTE, s.toString());
    }

    @SuppressWarnings("unchecked")
    public List<String> getDebuggerClassPathList() throws CoreException {
        List<String> result = new ArrayList<String>();
        List<String> entries = rConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, Collections.EMPTY_LIST);
        for (String e : entries) {
            IRuntimeClasspathEntry cpe = JavaRuntime.newRuntimeClasspathEntry(e);
            String location = cpe.getLocation();
            if (location != null) {
                result.add(location);
            }
        }
        return result;
    }

    public void setHistoryFile(String fileName) {
        wConfig.setAttribute(RUNCONFIG_HISTORY_FILE, fileName);
    }
    
    public String getHistoryFile() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_HISTORY_FILE, "");
    }

    public boolean getRunAutomatically() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_RUN_AUTOMATICALLY, false);
    }

    public void setRunAutomatically(boolean runAutomatically)  {
        wConfig.setAttribute(RUNCONFIG_RUN_AUTOMATICALLY, runAutomatically);
    }

    public boolean getIgnoreMissingFLIClasses() throws CoreException {
        return rConfig.getAttribute(RUNCONFIG_FLI_IGNORE_MISSING_CLASSES, false);
    }

    public void setIgnoreMissingFLIClasses(boolean ignore) {
        wConfig.setAttribute(RUNCONFIG_FLI_IGNORE_MISSING_CLASSES, ignore);
    }

}