package org.jenkinsci.plugins.customscriptscm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.util.Digester2;
import hudson.util.IOException2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

class CustomScriptSCMChangeLogSet extends ChangeLogSet<CustomScriptSCMChangeLogEntry> {
   private final List<CustomScriptSCMChangeLogEntry> changeLogEntries;
    
   public CustomScriptSCMChangeLogSet(AbstractBuild<?, ?> build, List<CustomScriptSCMChangeLogEntry> changeLogEntries) {
       super(build);
       this.changeLogEntries = changeLogEntries;
        for(CustomScriptSCMChangeLogEntry changeLogEntry: changeLogEntries) {
                changeLogEntry.setParent(this);
        }
   }
    
    @Override
    public boolean isEmptySet() {
        return this.changeLogEntries.isEmpty();
    }

    public Iterator<CustomScriptSCMChangeLogEntry> iterator() {
        return this.changeLogEntries.iterator();
    }
    
    public static class Parser extends ChangeLogParser {
        @Override
        public ChangeLogSet<? extends Entry> parse(AbstractBuild ab, File file) throws IOException, SAXException {
            List<CustomScriptSCMChangeLogEntry> changeLogEntries = new ArrayList<CustomScriptSCMChangeLogEntry>();

            Digester digester = new Digester2();
            digester.push(changeLogEntries);

            digester.addObjectCreate("*/entry", CustomScriptSCMChangeLogEntry.class);
            digester.addBeanPropertySetter("*/entry/date");
            digester.addBeanPropertySetter("*/entry/user-id", "userId");
            digester.addBeanPropertySetter("*/entry/changeset-number", "changesetNumber");
            digester.addBeanPropertySetter("*/entry/changeset-url", "changesetUrl");
            digester.addBeanPropertySetter("*/entry/comment", "comment");
            digester.addSetNext("*/entry", "add");

            digester.addObjectCreate("*/entry/items/item", CustomScriptSCMChangeLogEntry.Item.class);
            digester.addBeanPropertySetter("*/entry/items/item/change-type", "changeType");
            digester.addBeanPropertySetter("*/entry/items/item/file-name", "filename");
            digester.addSetNext("*/entry/items/item", "addFile");

            try {
                    digester.parse(file);
            }
            catch(IOException e) {
                    throw new IOException2("Failed to parse " + file, e);
            }
            catch(SAXException e) {
                    throw new IOException2("Failed to parse " + file, e);
            }

            return new CustomScriptSCMChangeLogSet(ab, changeLogEntries);
        }
        
    }
}