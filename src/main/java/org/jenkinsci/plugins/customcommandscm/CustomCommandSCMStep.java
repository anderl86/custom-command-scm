package org.jenkinsci.plugins.customcommandscm;

import com.google.inject.Inject;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.scm.SCM;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class CustomCommandSCMStep extends SCMStep {
    private String commandAdditions;

    @DataBoundConstructor
    public CustomCommandSCMStep(String commandAdditions) {
        this.commandAdditions = commandAdditions;
    }

    public String getCommandAdditions() {
        return commandAdditions;
    }

    @DataBoundSetter
    public void setCommandAdditions(String commandAdditions) {
        this.commandAdditions = commandAdditions;
    }

    @NonNull
    @Override
    public SCM createSCM() {
        return new CustomCommandSCM(this.commandAdditions);
    }

    @Extension
    public  static final  class DescriptorImpl extends SCMStepDescriptor {
        @Inject
        private CustomCommandSCM.DescriptorImpl scmDescriptor;

        @Override
        public String getFunctionName() {
            return "customCommandScm";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return this.scmDescriptor.getDisplayName();
        }
    }

}
