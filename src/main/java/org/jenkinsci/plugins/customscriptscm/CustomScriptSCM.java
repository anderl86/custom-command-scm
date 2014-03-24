/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.customscriptscm;

import hudson.AbortException;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        if(this.getCommandAdditions() != null) 
            cmd += " " + this.getCommandAdditions();
        
        String state = "";
        if(scmrs instanceof CustomScriptSCMRevisionState) {
            state = ((CustomScriptSCMRevisionState)scmrs).getState();
        }
        
        ByteArrayInputStream in = new ByteArrayInputStream(state.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int retcode = lnchr.launch().cmdAsSingleString(cmd).pwd(fp).stdin(in).stdout(out).stderr(tl.getLogger()).start().joinWithTimeout(DESCRIPTOR.getPollCommandTimeout(), TimeUnit.SECONDS, tl);
         
        CustomScriptSCMRevisionState newstate = new CustomScriptSCMRevisionState(out.toString());
        if(retcode == 1000)
            return new PollingResult(scmrs, newstate, PollingResult.Change.SIGNIFICANT);
        else if(retcode == 1001)
            return new PollingResult(scmrs, newstate, PollingResult.Change.INCOMPARABLE);
        else
            return new PollingResult(scmrs, newstate, PollingResult.Change.NONE);
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> ab, Launcher lnchr, FilePath fp, BuildListener bl, File file) throws IOException, InterruptedException {
        String cmd = DESCRIPTOR.getCheckoutCommand();
        if(this.getCommandAdditions() != null) 
            cmd += " " + this.getCommandAdditions();
        
        FileOutputStream changelogfile = new FileOutputStream(file);
        
        int retcode = lnchr.launch().cmdAsSingleString(cmd).stdout(changelogfile).stderr(lnchr.getListener().getLogger()).pwd(fp).envs(ab.getBuildVariables()).start().join();
        if(retcode != 0) {
            throw new AbortException("Error checking out source code");
        }
        
        return true;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new CustomScriptSCMChangeLogSet.Parser();
    }

    public String getCommandAdditions() {
        return commandAdditions;
    }

    public void setCommandAdditions(String commandAdditions) {
        this.commandAdditions = commandAdditions;
    }
    
    
    public static class CustomScriptSCMRevisionState extends SCMRevisionState {
        private String state;

        public CustomScriptSCMRevisionState(String state) {
            this.state = state;
        }        
        
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }
    
    public static class DescriptorImpl extends SCMDescriptor<CustomScriptSCM> {
        private String scmName = Messages.CustomScriptSCM_DisplayName();
        private String pollCommand;
        private int pollCommandTimeout = 60;
        private String checkoutCommand;
        
        DescriptorImpl() {
            super(CustomScriptSCM.class, null);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return this.scmName;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            this.setScmName(json.getString("scmName"));
            this.setPollCommand(json.getString("pollCommand"));
            this.setPollCommandTimeout(json.getInt("pollCommandTimeout"));
            this.setCheckoutCommand(json.getString("checkoutCommand"));
            save();
            return true;
        }

        public String getPollCommand() {
            return pollCommand;
        }

        public void setPollCommand(String pollCommand) {
            this.pollCommand = pollCommand;
        }

        public int getPollCommandTimeout() {
            return pollCommandTimeout;
        }

        public void setPollCommandTimeout(int pollCommandTimeout) {
            this.pollCommandTimeout = pollCommandTimeout;
        }

        public String getCheckoutCommand() {
            return checkoutCommand;
        }
        
        public void setCheckoutCommand(String checkoutCommand) {
            this.checkoutCommand = checkoutCommand;
        }

        public String getScmName() {
            return scmName;
        }

        public void setScmName(String scmName) {
            this.scmName = scmName;
        }
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public SCMDescriptor<?> getDescriptor() {
        return DESCRIPTOR;
    }
}
