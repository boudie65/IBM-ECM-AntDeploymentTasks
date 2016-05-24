package com.ibm.deploy.ant.task.ce;

import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import com.ibm.deploy.ant.tools.CodeModuleManager;

import lombok.Getter;
import lombok.Setter;

public class ImportCodeModuleTask extends ObjectStoreNestedTask {
    private @Getter @Setter String codeModuleName;
    private @Getter @Setter String codeModulePath;
    private @Getter @Setter boolean updateReferencingActions;
    
    private ArrayList<FileSet> filesets = new ArrayList<FileSet>();

    public void execute() throws BuildException {
        validateInput();
        CodeModuleManager codeModuleManager = new CodeModuleManager( getObjectStore(), this );
        codeModuleManager.createOrUpdate(codeModuleName, codeModulePath, getCodeModuleFiles(), updateReferencingActions );
    }
 
    private ArrayList<File> getCodeModuleFiles() {
        ArrayList<File> files = new ArrayList<File>();
         
        for (FileSet fs : filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject()); 
            String[] includedFiles = ds.getIncludedFiles();

            for(int i=0; i<includedFiles.length; i++) {
                File file = new File(ds.getBasedir() + "/" + includedFiles[i]);
                log("Adding file '" + file.getAbsolutePath() + "' to code module", verbosity);
                files.add(file);
            }
        }
        return files;
    }
     
    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    protected void validateInput() {
        checkForNull("codeModuleName", codeModuleName);
        checkForNull("codeModulePath", codeModulePath);
    }
}
