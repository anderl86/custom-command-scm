/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.customcommandscm;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;

import java.io.*;
import java.util.concurrent.TimeUnit;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author andi
 */
public class CustomCommandSCM extends SCM {
    private String commandAdditions = "";

    @DataBoundConstructor
    public CustomCommandSCM(String commandAdditions) {
        this.commandAdditions = Util.fixEmptyAndTrim(commandAdditions);
    }



    @Override
    public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> ab, FilePath workspace, Launcher lnchr, TaskListener tl) {
        return SCMRevisionState.NONE;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?, ?> ab, Launcher lnchr, FilePath fp, TaskListener tl, SCMRevisionState scmrs) throws IOException, InterruptedException {
        String cmd = DESCRIPTOR.getPollCommand();
        if(this.getCommandAdditions() != null) 
            cmd += " " + this.getCommandAdditions();
        
        String state = "";
        if(scmrs instanceof CustomCommandSCMRevisionState) {
            state = ((CustomCommandSCMRevisionState)scmrs).getState();
        }
        
        ByteArrayInputStream in = new ByteArrayInputStream(state.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        int retcode =   lnchr.launch()
                        .cmdAsSingleString(cmd)
                        .envs(ab.getEnvironment(null, tl))
                        .pwd(fp)
                        .stdin(in)
                        .stdout(out)
                        .stderr(tl.getLogger())
                        .start().joinWithTimeout(DESCRIPTOR.getPollCommandTimeout(), TimeUnit.SECONDS, tl);
         
        CustomCommandSCMRevisionState newstate = new CustomCommandSCMRevisionState(out.toString());
        if(retcode == 100)
            return new PollingResult(scmrs, newstate, PollingResult.Change.INSIGNIFICANT);
        else if(retcode == 101 || retcode == 1000)
            return new PollingResult(scmrs, newstate, PollingResult.Change.SIGNIFICANT);
        else if(retcode == 102 || retcode == 1001)
            return new PollingResult(scmrs, newstate, PollingResult.Change.INCOMPARABLE);
        else
            return new PollingResult(scmrs, newstate, PollingResult.Change.NONE);
    }

    @Override
    public void checkout(Run<?, ?> ab, Launcher lnchr, FilePath fp, TaskListener bl, File file, SCMRevisionState baseline) throws IOException, InterruptedException {
        String cmd = DESCRIPTOR.getCheckoutCommand();
        if(this.getCommandAdditions() != null) 
            cmd += " " + this.getCommandAdditions();

        EnvVars envVars = ab.getEnvironment(bl);
        OutputStream changelogfile = null;
        try {
            changelogfile = file != null ? new FileOutputStream(file) : new ByteArrayOutputStream();
            fp.mkdirs();
            int retcode =   lnchr.launch()
                    .cmdAsSingleString(cmd)
                    .envs(envVars)
                    .pwd(fp)
                    .stdout(changelogfile)
                    .stderr(lnchr.getListener().getLogger())
                    .start().join();

            if(retcode != 0) {
                throw new AbortException("Error checking out source code");
            }
        }
        finally {
            if(changelogfile != null) {
                changelogfile.close();
            }
        }
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new CustomCommandSCMChangeLogSet.Parser();
    }

    public String getCommandAdditions() {
        return commandAdditions;
    }

    public void setCommandAdditions(String commandAdditions) {
        this.commandAdditions = commandAdditions;
    }
    
    
    public static class CustomCommandSCMRevisionState extends SCMRevisionState {
        private String state;

        public CustomCommandSCMRevisionState(String state) {
            this.state = state;
        }        
        
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public static class DescriptorImpl extends SCMDescriptor<CustomCommandSCM> {
        private String scmName = Messages.CustomCommandSCM_DisplayName();
        private String pollCommand;
        private int pollCommandTimeout = 60;
        private String checkoutCommand;
        
        public DescriptorImpl() {
            super(CustomCommandSCM.class, null);
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
