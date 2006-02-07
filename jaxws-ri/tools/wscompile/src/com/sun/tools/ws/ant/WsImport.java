/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.tools.ws.ant;

import com.sun.tools.ws.wscompile.CompileTool;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.XMLCatalog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * wscompile task for use with the JAXWS project.
 *
 */
public class WsImport extends MatchingTask {

    private CommandlineJava cmd = new CommandlineJava();
    /*************************  -d option *************************/
    private File destDir = null;

    /** Gets the base directory to output generated class. **/
    public File getDestdir() {
        return this.destDir;
    }

    /** Sets the base directory to output generated class. **/
    public void setDestdir(File base) {
        this.destDir = base;
    }

    /** wsdllocation - set @WebService.wsdlLocation and @WebServiceClient.wsdlLocation values */

    private String wsdlLocation;

    public String getWsdllocation() {
        return wsdlLocation;
    }

    public void setWsdllocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    public void addConfiguredXMLCatalog(XMLCatalog entityResolver) {
        if(this.xmlCatalog==null){
            this.xmlCatalog = new XMLCatalog();
            xmlCatalog.setProject(getProject());
        }
        this.xmlCatalog.addConfiguredXMLCatalog(entityResolver);
    }

    private XMLCatalog xmlCatalog;

    private String pkg;
    public void setPackage(String pkg){
        this.pkg = pkg;
    }

    public String getPackage(){
        return pkg;
    }

    /**
     * Adds a JVM argument.
     *
     * @return JVM argument created
     */
    public Commandline.Argument createJvmarg() {
        return cmd.createVmArgument();
    }

    /********************  -extensions option **********************/
    protected boolean extension;

    /** Gets the "extension" flag. **/
    public boolean getExtension() {
        return extension;
    }

    /** Sets the "extension" flag. **/
    public void setExtension(boolean extension) {
        this.extension = extension;
    }

    /*************************  -keep option *************************/
    private boolean keep = false;

    /** Gets the "keep" flag. **/
    public boolean getKeep() {
        return keep;
    }

    /** Sets the "keep" flag. **/
    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    /*************************  -fork option *************************/
    private boolean fork = false;

    /** Gets the "fork" flag. **/
    public boolean getFork() {
        return fork;
    }

    /** Sets the "fork" flag. **/
    public void setFork(boolean fork) {
        this.fork = fork;
    }

    /*************************  -O option *************************/
    private boolean optimize = false;

    /** Gets the optimize flag. **/
    public boolean getOptimize() {
        return optimize;
    }

    /** Sets the optimize flag. **/
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    /*************************  -s option *************************/
    private File sourcedestdir;

    /** Sets the directory to place generated source java files. **/
    public void setSourcedestdir(File sourceBase) {
        keep = true;
        this.sourcedestdir = sourceBase;
    }

    /** Gets the directory to place generated source java files. **/
    public File getSourcedestdir() {
        return sourcedestdir;
    }

    /*************************  -verbose option *************************/
    protected boolean verbose = false;

    /** Gets the "verbose" flag. **/
    public boolean getVerbose() {
        return verbose;
    }

    /** Sets the "verbose" flag. **/
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

     /*************************  -g option *************************/
     private boolean debug = false;

     /** Gets the debug flag. **/
     public boolean getDebug() {
         return debug;
     }

     /** Sets the debug flag. **/
     public void setDebug(boolean debug) {
         this.debug = debug;
     }

    /***********************  include ant runtime **********************/
    /** not sure if these methods are needed */
    private boolean includeAntRuntime = false;

    /**
     * Include ant's own classpath in this task's classpath?
     */
    public void setIncludeantruntime(boolean include) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the
     * task's classpath.
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime;
    }

    /***********************  include java runtime **********************/
    /** not sure if these methods are needed */
    private boolean includeJavaRuntime = false;

    /**
     * Sets whether or not to include the java runtime libraries to this
     * task's classpath.
     */
    public void setIncludejavaruntime(boolean include) {
        includeJavaRuntime = include;
    }

    /**
     * Gets whether or not the java runtime should be included in this
     * task's classpath.
     */
    public boolean getIncludejavaruntime() {
        return includeJavaRuntime;
    }

    /**
     * Files used to determine whether XJC should run or not.
     */
    private final ArrayList<File> dependsSet = new ArrayList<File>();
    private final ArrayList<File> producesSet = new ArrayList<File>();

    /**
     * Set to true once the &lt;produces> element is used.
     * This flag is used to issue a suggestion to users.
     */
    private boolean producesSpecified = false;

    /** Nested &lt;depends> element. */
    public void addConfiguredDepends( FileSet fs ) {
        addIndividualFilesTo( fs, dependsSet );
    }

    /** Nested &lt;produces> element. */
    public void addConfiguredProduces( FileSet fs ) {
        producesSpecified = true;
        if( !fs.getDir(getProject()).exists() ) {
            log(
                fs.getDir(getProject()).getAbsolutePath()+" is not found and thus excluded from the dependency check",
                Project.MSG_INFO );
        } else
            addIndividualFilesTo( fs, producesSet );
    }

    /**
     * Extracts {@link File} objects that the given {@link FileSet}
     * represents and adds them all to the given {@link java.util.List}.
     */
    private void addIndividualFilesTo( FileSet fs, List<File> lst ) {
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();
        File baseDir = ds.getBasedir();

        for (String value : includedFiles) {
            lst.add(new File(baseDir, value));
        }
    }

    /**
     * Determines the timestamp of the newest/oldest file in the given set.
     */
    private long computeTimestampFor( List<File> files, boolean findNewest ) {

        long lastModified = findNewest?Long.MIN_VALUE:Long.MAX_VALUE;

        for( File file : files ) {
            log("Checking timestamp of "+file.toString(), Project.MSG_VERBOSE );

            if( findNewest )
                lastModified = Math.max( lastModified, file.lastModified() );
            else
                lastModified = Math.min( lastModified, file.lastModified() );
        }

        if( lastModified == Long.MIN_VALUE ) // no file was found
            return Long.MAX_VALUE;  // force re-run

        if( lastModified == Long.MAX_VALUE ) // no file was found
            return Long.MIN_VALUE;  // force re-run

        return lastModified;
    }


    private String binding;
    /**
     * @return Returns the binding.
     */
    public String getBinding() {
        return binding;
    }
    /**
     * @param binding The binding to set.
     */
    public void setBinding(String binding) {
        this.binding = binding;
        dependsSet.add(new File(binding));
    }

    /**
     * Adds a new catalog file.
     */
    public void setCatalog( File catalog ) {
        this.catalog = catalog;
    }

    public File getCatalog(){
        return catalog;
    }

    private File catalog;


    private String wsdl;
    /**
     * @return Returns the wsdl.
     */
    public String getWsdl() {
        return wsdl;
    }
    /**
     * @param wsdl The wsdl to set.
     */
    public void setWsdl(String wsdl) {
        this.wsdl = wsdl;
        dependsSet.add(new File(wsdl));
    }
    
    private boolean mex = false;
    
    /**
     * @return Whether or not wsimport is using MEX
     * to retrieve wsdl.
     */
    public boolean getMex() {
        return mex;
    }

    /**
     * @param mex Set to true to tell wsimport to make
     * a MEX GetMetada request rather than an HTTP GET.
     */
    public void setMex(boolean mex) {
        this.mex = mex;
    }
    
    public void addConfiguredBinding( FileSet fs ) {
        DirectoryScanner ds = fs.getDirectoryScanner(project);
        String[] includedFiles = ds.getIncludedFiles();
        File baseDir = ds.getBasedir();
        for (String includedFile : includedFiles) {
            bindingFiles.add(new File(baseDir, includedFile));
        }
        addIndividualFilesTo( fs, dependsSet );
    }

    private void setupWsimportForkCommand() {
        ClassLoader loader = this.getClass().getClassLoader();

        Path classpath = new Path(project);
        if (loader instanceof AntClassLoader) {
           classpath = new Path(project, ((AntClassLoader)loader).getClasspath());
        }
        cmd.createClasspath(getProject()).append(classpath);
        cmd.setClassname("com.sun.tools.ws.WsImport");
        //setupWsimportArgs();
        //cmd.createArgument(true).setLine(forkCmd.toString());
    }

    private void setupWsimportArgs() {
        // d option
        if (null != getDestdir() && !getDestdir().getName().equals("")) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(getDestdir());
        }

        // extension flag
        if (getExtension()) {
            cmd.createArgument().setValue("-extension");
        }

        // g option
        if (getDebug()) {
            cmd.createArgument().setValue("-g");
        }

        // keep option
        if (getKeep()) {
            cmd.createArgument().setValue("-keep");
        }

        // optimize option
        if (getOptimize()) {
            cmd.createArgument().setValue("-O");
        }

        // s option
        if (null != getSourcedestdir() && !getSourcedestdir().getName().equals("")) {
            cmd.createArgument().setValue("-s");
            cmd.createArgument().setFile(getSourcedestdir());
        }

        //catalog
        if((getCatalog() != null) && (getCatalog().getName().length() > 0)){
            cmd.createArgument().setValue("-catalog");
            cmd.createArgument().setFile(getCatalog());
        }

        // verbose option
        if (getVerbose()) {
            cmd.createArgument().setValue("-verbose");
        }

        // mex option
        if (getMex()) {
            cmd.createArgument().setValue("-mex");
        }

        //wsdl
        if(getWsdl() != null){
            cmd.createArgument().setValue(getWsdl());
        }

        //package
        if((getPackage() != null) && (getPackage().length() > 0)){
            cmd.createArgument().setValue("-p");
            cmd.createArgument().setValue(getPackage());
        }

        if(getBinding() != null){
            cmd.createArgument().setValue("-b");
            cmd.createArgument().setFile(new File (getBinding()));
        }

        if((wsdlLocation != null) && (wsdlLocation.length() != 0)){
            cmd.createArgument().setValue("-wsdllocation");
            cmd.createArgument().setValue(wsdlLocation);
        }

        if(!bindingFiles.isEmpty()){
            for(File binding : bindingFiles){
                cmd.createArgument().setValue("-b");
                cmd.createArgument().setFile(binding);
            }
        }
    }


    /** Called by the project to let the task do it's work **/
    public void execute() throws BuildException {
        /* Create an instance of the rmic, redirecting output to
         * the project log
         */
        LogOutputStream logstr = null;
        boolean ok = false;
        try {
            if( !producesSpecified ) {
                log("Consider using <depends>/<produces> so that wsimport won't do unnecessary compilation",Project.MSG_INFO);
            }

            // up to date check
            long srcTime = computeTimestampFor(dependsSet,true);
            long dstTime = computeTimestampFor(producesSet,false);
            log("the last modified time of the inputs is  "+srcTime, Project.MSG_VERBOSE);
            log("the last modified time of the outputs is "+dstTime, Project.MSG_VERBOSE);

            if( srcTime < dstTime ) {
                log("files are up to date");
                return;
            }


            if(fork){
                setupWsimportForkCommand();
            } else {
                if (cmd.getVmCommand().size() > 1) {
                    log("JVM args ignored when same JVM is used.",Project.MSG_WARN);
                }
            }
            setupWsimportArgs();
            if (fork) {
                if (verbose) {
                    log("command line: "+"wsimport "+cmd.toString());
                }
                int status = run(cmd.getCommandline());
                ok = (status == 0);
            } else {
                if (verbose) {
                    log("command line: "+"wsimport "+cmd.getJavaCommand().toString());
                }
                logstr = new LogOutputStream(this, Project.MSG_WARN);

                ClassLoader old = Thread.currentThread().getContextClassLoader();
                ClassLoader loader = this.getClass().getClassLoader();
                Thread.currentThread().setContextClassLoader(loader);
                String sysPath = System.getProperty("java.class.path");
                try {
                    CompileTool compTool = new CompileTool(logstr, "wsimport");
                    if(xmlCatalog != null){
                        compTool.setEntityResolver(xmlCatalog);
                    }
                    if (loader instanceof AntClassLoader) {
                        System.setProperty("java.class.path", ((AntClassLoader)loader).getClasspath());
                    }
                    ok = compTool.run(cmd.getJavaCommand().getArguments());
                } finally {
                    if (sysPath != null) {
                        System.setProperty("java.class.path", sysPath);
                    }
                    Thread.currentThread().setContextClassLoader(old);
                }
            }
            if (!ok) {
                if (!verbose) {
                    log("Command invoked: "+"wsimport "+cmd.toString());
                }
                throw new BuildException("wsimport failed", location);
            }
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException)ex;
            } else {
                throw new BuildException("Error starting wsimport: ", ex,
                getLocation());
            }
        } finally {
            try {
                if (logstr != null) {
                    logstr.close();
                }
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     */
    private int run(String[] command) throws BuildException {
        LogStreamHandler logstr = new LogStreamHandler(this,
            Project.MSG_INFO, Project.MSG_WARN);
        Execute exe = new Execute(logstr);
        exe.setAntRun(project);
        exe.setCommandline(command);
        try {
            int rc = exe.execute();
            if (exe.killedProcess()) {
                log("Timeout: killed the sub-process", Project.MSG_WARN);
            }
            return rc;
        } catch (IOException e) {
            throw new BuildException(e, location);
        }
    }

    private Set<File> bindingFiles = new HashSet<File>();

}
