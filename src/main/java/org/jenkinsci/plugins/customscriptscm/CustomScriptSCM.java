/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.customscriptscm;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author andi
 */
public class CustomScriptSCM extends SCM {
    private String commandAdditions = "";

    @DataBoundConstructor
    public CustomScriptSCM(String commandAdditions) {
        this.commandAdditions = Util.fixEmptyAndTrim(commandAdditions);
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> ab, Launcher lnchr, TaskListener tl) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> ap, Launcher lnchr, FilePath fp, TaskListener tl, SCMRevisionState scmrs) throws IOException, InterruptedException {
        String cmd = DESCRIPTOR.getPollCommand();
        if(this.commandAdditions != null) 
            cmd += " " + this.commandAdditions;
        int retcode = lnchr.launch().cmdAsSingleString(cmd).stdout(tl).pwd(fp).start().joinWithTimeout(DESCRIPTOR.getPollCommandTimeout(), TimeUnit.SECONDS, tl);
        return retcode == 1000 ? PollingResult.BUILD_NOW : PollingResult.NO_CHANGES;
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> ab, Launcher lnchr, FilePath fp, BuildListener bl, File file) throws IOException, InterruptedException {
        String cmd = DESCRIPTOR.getCheckoutCommand();
        if(this.commandAdditions != null) 
            cmd += " " + this.commandAdditions;
        
        FileOutputStream changelogfile = new FileOutputStream(file);
        
        int retcode = lnchr.launch().cmdAsSingleString(cmd).stdout(changelogfile).stderr(lnchr.getListener().getLogger()).pwd(fp).envs(ab.getBuildVariables()).start().join();
        return retcode == 0;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new CustomScriptSCMChangeLogSet.Parser();
    }
    
    public static class DescriptorImpl extends SCMDescriptor<CustomScriptSCM> {
        private String pollCommand;
        private int pollCommandTimeout = 60;
        private String checkoutCommand;
        
        DescriptorImpl() {
            super(CustomScriptSCM.class, null);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return Messages.CustomScriptSCM_DisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            this.setPollCommand(json.getString("pollCommand"));
            this.setPollCommandTimeout(json.getInt("pollCommandTimeout"));
            this.setCheckoutCommand(json.getString("checkoutCommand"));
            save();
            return true;
        }

        /**
         * @return the pollCommand
         */
        public String getPollCommand() {
            return pollCommand;
        }

        /**
         * @param pollCommand the pollCommand to set
         */
        public void setPollCommand(String pollCommand) {
            this.pollCommand = pollCommand;
        }

        /**
         * @return the pollCommandTimeout
         */
        public int getPollCommandTimeout() {
            return pollCommandTimeout;
        }

        /**
         * @param pollCommandTimeout the pollCommandTimeout to set
         */
        public void setPollCommandTimeout(int pollCommandTimeout) {
            this.pollCommandTimeout = pollCommandTimeout;
        }

        /**
         * @return the checkoutCommand
         */
        public String getCheckoutCommand() {
            return checkoutCommand;
        }

        /**
         * @param checkoutCommand the checkoutCommand to set
         */
        public void setCheckoutCommand(String checkoutCommand) {
            this.checkoutCommand = checkoutCommand;
        }
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public SCMDescriptor<?> getDescriptor() {
        return DESCRIPTOR;
    }
}
